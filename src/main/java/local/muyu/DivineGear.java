package local.muyu;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.equipment.*;

final class DivineGear {
    private static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Muyu.id("divine"));

    static void initialize() {
        Map<Item, Item> originals = new LinkedHashMap<>();
        for (Item original : new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS, Items.NETHERITE_HORSE_ARMOR, Items.NETHERITE_NAUTILUS_ARMOR, Items.NETHERITE_SWORD,
                Items.NETHERITE_SPEAR, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE}) {
            String path = BuiltInRegistries.ITEM.getKey(original).getPath().replace("netherite_", "divine_");
            var props = new Item.Properties().stacksTo(1);
            if (original != Items.NETHERITE_HORSE_ARMOR && original != Items.NETHERITE_NAUTILUS_ARMOR)
                props.repairable(Content.GIFTS.get(Balance.Gift.SHARD));
            originals.put(Content.item(path, props, Item::new), original);
        }
        // Vanilla binds components such as axe stripping and spear damage types after registration.
        DefaultItemComponentEvents.MODIFY.register(ctx -> ctx.modify(originals.keySet(),
                (builder, item) -> scale(builder, item.components(), originals.get(item).components())));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out -> originals.forEach((item, base) -> {
            if (!isMiningTool(base)) out.accept(item);
        }));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> originals.forEach((item, base) -> {
            if (isMiningTool(base)) out.accept(item);
        }));
    }

    private static boolean isMiningTool(Item item) {
        return item == Items.NETHERITE_PICKAXE || item == Items.NETHERITE_AXE || item == Items.NETHERITE_SHOVEL || item == Items.NETHERITE_HOE;
    }

    private static void scale(DataComponentMap.Builder b, DataComponentMap own, DataComponentMap base) {
        b.addAll(base);
        b.set(DataComponents.ITEM_NAME, own.get(DataComponents.ITEM_NAME));
        b.set(DataComponents.ITEM_MODEL, own.get(DataComponents.ITEM_MODEL));
        if (base.has(DataComponents.REPAIRABLE)) b.set(DataComponents.REPAIRABLE, own.get(DataComponents.REPAIRABLE));
        if (base.has(DataComponents.MAX_DAMAGE)) b.set(DataComponents.MAX_DAMAGE, 2 * base.get(DataComponents.MAX_DAMAGE));
        var enchant = base.get(DataComponents.ENCHANTABLE);
        if (enchant != null) b.set(DataComponents.ENCHANTABLE, new Enchantable(2 * enchant.value()));
        var mods = base.get(DataComponents.ATTRIBUTE_MODIFIERS);
        b.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(mods.modifiers().stream().map(entry -> {
            var m = entry.modifier();
            if (entry.attribute().equals(Attributes.ATTACK_SPEED)) return entry;
            double innate = entry.attribute().equals(Attributes.ATTACK_DAMAGE) ? 1 : 0;
            // Double the displayed total, including the player's unarmed base.
            return new ItemAttributeModifiers.Entry(entry.attribute(),
                    new AttributeModifier(m.id(), 2 * m.amount() + innate, m.operation()), entry.slot(), entry.display());
        }).toList()));
        var e = base.get(DataComponents.EQUIPPABLE);
        if (e != null) b.set(DataComponents.EQUIPPABLE, new Equippable(e.slot(), e.equipSound(), Optional.of(ASSET),
                e.cameraOverlay(), e.allowedEntities(), e.dispensable(), e.swappable(), e.damageOnHurt(),
                e.equipOnInteract(), e.canBeSheared(), e.shearingSound()));
        var tool = base.get(DataComponents.TOOL);
        if (tool != null) b.set(DataComponents.TOOL, new Tool(tool.rules().stream().map(rule -> new Tool.Rule(rule.blocks(),
                rule.speed().map(speed -> Math.min(Float.MAX_VALUE, 2 * speed)), rule.correctForDrops())).toList(),
                2 * tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
        var weapon = base.get(DataComponents.WEAPON);
        if (weapon != null) b.set(DataComponents.WEAPON, new Weapon(weapon.itemDamagePerAttack(), 2 * weapon.disableBlockingForSeconds()));
        var spear = base.get(DataComponents.KINETIC_WEAPON);
        if (spear != null) scaleSpear(b, base, spear);
    }

    private static void scaleSpear(DataComponentMap.Builder b, DataComponentMap base, KineticWeapon k) {
        b.set(DataComponents.KINETIC_WEAPON, new KineticWeapon(k.contactCooldownTicks(), k.delayTicks(),
                k.dismountConditions().map(DivineGear::charge), k.knockbackConditions().map(DivineGear::charge),
                k.damageConditions().map(DivineGear::charge), k.forwardMovement()*2, k.damageMultiplier()*2, k.sound(), k.hitSound()));
        var r = base.get(DataComponents.ATTACK_RANGE);
        b.set(DataComponents.ATTACK_RANGE, new AttackRange(2*r.minReach(), 2*r.maxReach(), 2*r.minCreativeReach(),
                2*r.maxCreativeReach(), 2*r.hitboxMargin(), r.mobFactor()));
    }

    private static KineticWeapon.Condition charge(KineticWeapon.Condition c) {
        return new KineticWeapon.Condition(c.maxDurationTicks()*2, c.minSpeed()*2, c.minRelativeSpeed()*2);
    }
    private DivineGear() {}
}
