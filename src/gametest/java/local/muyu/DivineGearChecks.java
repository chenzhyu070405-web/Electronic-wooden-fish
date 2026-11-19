package local.muyu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;

final class DivineGearChecks {
    static void run(MinecraftServer server) {
        checkPiece(server,"helmet","SSS","S S");
        checkPiece(server,"chestplate","S S","SSS","SSS");
        checkPiece(server,"leggings","SSS","S S","S S");
        checkPiece(server,"boots","S S","S S");
        checkPiece(server,"horse_armor","S S","SLS","SSS");
        checkPiece(server,"nautilus_armor"," SS","SNS"," SS");
        checkPiece(server,"spear","  S"," | ","|  ");
        checkPiece(server,"sword","S","S","|");
        checkPiece(server,"pickaxe","SSS"," | "," | ");
        checkPiece(server,"axe","SS","S|"," |");
        checkPiece(server,"hoe","SS"," |"," |");
        checkPiece(server,"shovel","S","|","|");
        System.out.println("Divine gear: crafted 12 live recipes; verified netherite attack speeds, doubled equipment stats, tool roles and repair material.");
    }
    private static void checkPiece(MinecraftServer server, String kind, String... pattern) {
        var id=Muyu.id("divine_"+kind);
        var recipe=(ShapedRecipe)server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,id)).orElseThrow().value();
        var shard=Content.GIFTS.get(Balance.Gift.SHARD);
        var cells=new ArrayList<ItemStack>();
        for (String row : pattern) for (char c : row.toCharArray()) cells.add(switch(c) {
            case 'S' -> new ItemStack(shard);
            case '|' -> new ItemStack(Items.STICK);
            case 'N' -> new ItemStack(Items.NAUTILUS_SHELL);
            case 'L' -> new ItemStack(Items.LEATHER);
            default -> ItemStack.EMPTY;
        });
        var input=CraftingInput.of(pattern[0].length(),pattern.length,cells);
        check(recipe.matches(input,server.overworld()), kind+" recipe");
        var gear=recipe.assemble(input);
        var base=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("netherite_"+kind)).getDefaultInstance();
        check(gear.is(BuiltInRegistries.ITEM.getValue(id)) && gear.getCount()==1,kind+" output");
        check(gear.getMaxDamage()==2*base.getMaxDamage(),kind+" durability");
        if(base.isDamageableItem()) check(gear.isValidRepairItem(new ItemStack(shard)),kind+" repairs");
        var enchant=base.get(DataComponents.ENCHANTABLE);
        if(enchant!=null) check(gear.get(DataComponents.ENCHANTABLE).value()==2*enchant.value(),kind+" enchantability");
        if(base.has(DataComponents.EQUIPPABLE)) {
            var equipment=gear.get(DataComponents.EQUIPPABLE);
            check(equipment.assetId().orElseThrow().identifier().equals(Muyu.id("divine")),kind+" equipped model");
            for(var attr : List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.KNOCKBACK_RESISTANCE))
                equal(stat(gear,attr,0,equipment.slot()),2*stat(base,attr,0,equipment.slot()),kind+" defense");
        } else {
            equal(stat(gear,Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND),2*stat(base,Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND),kind+" damage");
            equal(stat(gear,Attributes.ATTACK_SPEED,4,EquipmentSlot.MAINHAND),stat(base,Attributes.ATTACK_SPEED,4,EquipmentSlot.MAINHAND),kind+" attack speed");
            check(java.util.Objects.equals(gear.get(DataComponents.BLOCK_TRANSFORMER),base.get(DataComponents.BLOCK_TRANSFORMER)), kind+" block actions");
            if(gear.has(DataComponents.TOOL)) for(var block:List.of(Blocks.STONE,Blocks.DIRT,Blocks.OAK_LOG,Blocks.COBWEB)) {
                var state=block.defaultBlockState();
                equal(gear.getDestroySpeed(state),Math.min(Float.MAX_VALUE,base.getDestroySpeed(state)*2),kind+" mining");
            }
        }
        if(kind.equals("spear")) {
            var spear=gear.get(DataComponents.KINETIC_WEAPON);
            var netherite=base.get(DataComponents.KINETIC_WEAPON);
            check(spear.contactCooldownTicks()==netherite.contactCooldownTicks() && spear.delayTicks()==netherite.delayTicks(),"spear cadence");
            check(gear.get(DataComponents.ATTACK_ANIMATION).equals(base.get(DataComponents.ATTACK_ANIMATION)),"spear animation");
            equal(spear.damageMultiplier(),2*netherite.damageMultiplier(),"spear kinetic damage");
            equal(spear.damageConditions().orElseThrow().minRelativeSpeed(),
                    2*netherite.damageConditions().orElseThrow().minRelativeSpeed(),"spear relative speed threshold");
            equal(gear.get(DataComponents.ATTACK_RANGE).maxReach(),2*base.get(DataComponents.ATTACK_RANGE).maxReach(),"spear reach");
        }
        server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder ->
                check(holder.value().isSupportedItem(gear)==holder.value().isSupportedItem(base),kind+" enchantment support"));
    }
    private static double stat(ItemStack stack, Holder<Attribute> attribute, double base, EquipmentSlot slot) {
        return stack.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,base,slot);
    }
    private static void equal(double actual, double expected, String label) {
        check(Math.abs(expected-actual)<0.00001,label+" expected "+expected+", got "+actual);
    }
    private static void check(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    private DivineGearChecks() {}
}
