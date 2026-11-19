package local.muyu;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.server.level.ServerPlayer;

public final class Content {
    private static final ResourceKey<Block> FISH_KEY = ResourceKey.create(Registries.BLOCK, Muyu.id("wooden_fish"));
    public static final WoodenFishBlock FISH = Registry.register(BuiltInRegistries.BLOCK, FISH_KEY,
            new WoodenFishBlock(BlockBehaviour.Properties.of().setId(FISH_KEY).strength(1.2F)
                    .sound(SoundType.WOOD).noOcclusion().lightLevel(s -> s.getValue(WoodenFishBlock.STRUCK) ? 7 : 0)));
    public static final Item FISH_ITEM = item("wooden_fish", new Item.Properties().useBlockDescriptionPrefix(),
            props -> new BlockItem(FISH, props));
    public static final Map<Balance.Gift, Item> GIFTS = Map.of(
            Balance.Gift.APPLE, gift(Balance.Gift.APPLE, Rarity.EPIC, new Item.Properties()
                    .food(Foods.ENCHANTED_GOLDEN_APPLE, Consumables.ENCHANTED_GOLDEN_APPLE)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true), Item::new),
            Balance.Gift.SHARD, gift(Balance.Gift.SHARD, Rarity.UNCOMMON, new Item.Properties(), Item::new),
            Balance.Gift.BELL, gift(Balance.Gift.BELL, Rarity.RARE, new Item.Properties().stacksTo(1), DestinyBell::new),
            Balance.Gift.SWORD, gift(Balance.Gift.SWORD, Rarity.EPIC, new Item.Properties().fireResistant()
                    .sword(ToolMaterial.NETHERITE, 19.0F, -2.4F), Item::new));
    public static final SimpleParticleType LOTUS = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Muyu.id("lotus"),
            FabricParticleTypes.simple(false));
    private static final ResourceKey<EntityType<?>> EFFIGY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Muyu.id("buddha"));
    public static final EntityType<BuddhaEffigy> BUDDHA = Registry.register(BuiltInRegistries.ENTITY_TYPE, EFFIGY_KEY,
            EntityType.Builder.of(BuddhaEffigy::new, MobCategory.MISC).sized(1.9F, 2.65F)
                    .noSummon().noSave().build(EFFIGY_KEY));
    public static final ResourceKey<DamageType> WEST = ResourceKey.create(Registries.DAMAGE_TYPE, Muyu.id("ascension"));

    public enum Cue {
        KNOCK("knock", 0.82F), OMEN("omen", 0.65F), ASCENT("ascent", 0.8F), AUDIENCE("audience", 0.8F),
        BLESSING("blessing", 0.8F), PASSAGE("passage", 0.78F);

        public final SoundEvent sound;
        public final float volume;
        Cue(String path, float volume) {
            var id = Muyu.id(path);
            sound = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
            this.volume = volume;
        }
    }

    static void initialize() {
        Cue.values();
        DivineGear.initialize();
        FabricDefaultAttributeRegistry.register(BUDDHA, BuddhaEffigy.createMobAttributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> out.accept(FISH_ITEM));
    }

    private static Item gift(Balance.Gift gift, Rarity rarity, Item.Properties props, Function<Item.Properties, Item> factory) {
        return item(gift.id, props.rarity(rarity).component(DataComponents.LORE,
                new ItemLore(List.of(Component.translatable("muyu.lore." + gift.id)))), factory);
    }
    static Item item(String path, Item.Properties props, Function<Item.Properties, Item> factory) {
        var key = ResourceKey.create(Registries.ITEM, Muyu.id(path));
        var item = factory.apply(props.setId(key));
        if (item instanceof BlockItem blockItem) blockItem.registerBlocks(Item.BY_BLOCK, item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
    private static final class DestinyBell extends Item {
        DestinyBell(Properties properties) { super(properties); }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer local) Practice.ringBell(local, hand);
            return InteractionResult.SUCCESS;
        }
    }
    private Content() {}
}
