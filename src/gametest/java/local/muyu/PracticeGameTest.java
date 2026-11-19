package local.muyu;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class PracticeGameTest implements FabricClientGameTest {
    private static final BlockPos FIRST = new BlockPos(0, 100, 2), SECOND = new BlockPos(1, 100, 2);
    private final List<String> chat = new CopyOnWriteArrayList<>();

    @Override public void runTest(ClientGameTestContext c) {
        c.runOnClient(client -> {
            client.options.pauseOnLostFocus = false;
            client.options.languageCode = "zh_cn";
            client.getLanguageManager().setSelected("zh_cn");
            client.reloadResourcePacks();
        });
        c.waitFor(client -> net.minecraft.network.chat.Component.translatable("muyu.temple").getString().equals("云上莲台"), 1200);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> { if (!overlay) chat.add(message.getString()); });
        TestWorldSave saved;
        Balance.Gift firstGift;
        BigInteger merit;
        try (var world = c.worldBuilder().create()) {
            var s = world.getServer();
            s.runCommand("difficulty peaceful");
            s.runCommand("fill -3 99 -3 4 99 5 minecraft:stone");
            s.runCommand("tp @p 0.5 100 0.5");
            s.runCommand("gamemode survival @p");
            s.runCommand("give @p minecraft:diamond 1");
            s.runOnServer(server -> {
                var player = player(server);
                player.level().getGameRules().set(GameRules.KEEP_INVENTORY, false, server);
                validateContent(server);
                player.level().setBlockAndUpdate(FIRST, Content.FISH.defaultBlockState());
                var fishStack = new ItemStack(Content.FISH_ITEM);
                var previousHand = player.getMainHandItem().copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, fishStack);
                var support = SECOND.below();
                var top = new BlockHitResult(Vec3.atBottomCenterOf(SECOND), Direction.UP, support, false);
                check(fishStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top)).consumesAction(),
                        "place a crafted wooden fish on the platform");
                check(player.level().getBlockState(SECOND).is(Content.FISH) && fishStack.isEmpty(), "block item consumed on placement");
                player.setItemInHand(InteractionHand.MAIN_HAND, previousHand);
            });
            world.getConnection().waitForChunksDownload();
            c.waitFor(client -> client.level != null && client.level.getBlockState(FIRST).is(Content.FISH));
            c.getInput().lookAt(FIRST);
            c.waitTicks(3);
            c.getInput().holdKeyFor(options -> options.keyUse, 24);
            c.waitTicks(8);
            var once = s.computeOnServer(server -> Practice.account(player(server)).merit());
            check(once.equals(BigInteger.ONE) || once.intValueExact() == 11, "Holding use should produce exactly one base knock, got " + once);
            c.takeScreenshot("wooden-fish");
            hit(c, SECOND);
            c.waitTicks(8);
            var twice = s.computeOnServer(server -> Practice.account(player(server)).merit());
            check(twice.subtract(once).equals(BigInteger.ONE) || twice.subtract(once).intValueExact() == 11, "two blocks, one account");
            s.runOnServer(server -> {
                var p = player(server);
                check(p.level().destroyBlock(FIRST, true), "wooden fish breaks normally");
                check(Practice.account(p).merit().equals(twice), "break keeps account");
                var drops = p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(FIRST).inflate(1), entity -> entity.getItem().is(Content.FISH_ITEM));
                check(drops.size() == 1, "one wooden fish drops from the live loot table");
            });
            s.runCommand("execute as @p run muyu rehearse");
            hit(c, SECOND);
            awaitFrame(c, Balance.SPEAK + 10);
            check(Balance.DIALOGUE.contains(LocalScene.current().line()), "the audience displays a Buddha scripture");
            c.takeScreenshot("lotus-court");
            var locked = s.computeOnServer(server -> Practice.account(player(server)));
            hit(c, SECOND);
            world.getConnection().waitForServerboundPackets();
            check(s.computeOnServer(server -> Practice.account(player(server))).equals(locked), "knocks during the audience are ignored");
            c.runOnClient(client -> client.gui.screen().onClose());
            c.waitForScreen(net.minecraft.client.gui.screens.PauseScreen.class);
            c.waitTicks(3);
            int paused = LocalScene.current().tick();
            c.waitTicks(10);
            check(LocalScene.current().tick() == paused, "timeline waits while the pause menu covers the scene");
            c.runOnClient(client -> client.gui.setScreen(null));
            awaitFrame(c, Balance.BLESS + 6);
            c.takeScreenshot("blessing");
            firstGift = s.computeOnServer(server -> Practice.account(player(server)).rite().orElseThrow().gift());
            merit = s.computeOnServer(server -> Practice.account(player(server)).merit());
            check(s.computeOnServer(server -> Practice.account(player(server)).rite().orElseThrow().blessed()), "escrow exists before death");
            saved = world.getWorldSave();
        }
        try (var world = saved.open()) {
            var s = world.getServer();
            awaitFrame(c, 4);
            check(s.computeOnServer(server -> Practice.account(player(server)).rite().orElseThrow().gift()) == firstGift, "reload preserves exact gift");
            waitForDeath(c);
            c.takeScreenshot("westward");
            var expected = s.computeOnServer(server -> "‘" + player(server).getName().getString() + "’上西天了");
            c.waitFor(client -> chat.contains(expected));
            check(chat.stream().filter(expected::equals).count() == 1, "one vanilla death announcement");
            respawn(c);
            s.waitFor(server -> player(server).getInventory().countItem(Content.GIFTS.get(firstGift)) == 1);
            s.runOnServer(server -> {
                var p = player(server);
                var a = Practice.account(p);
                check(a.merit().equals(merit) && a.peak().equals(merit) && a.ascensions().intValueExact() == 1, "respawn restores ledger");
                check(a.history().get(firstGift).intValueExact() == 1 && a.pending().isEmpty() && a.rite().isEmpty(), "one payment");
                check(p.getInventory().countItem(Items.DIAMOND) == 0 && !p.level().getGameRules().get(GameRules.KEEP_INVENTORY), "ordinary items follow death rule");
                p.level().getGameRules().set(GameRules.KEEP_INVENTORY, true, server);
                p.level().getGameRules().set(GameRules.SHOW_DEATH_MESSAGES, false, server);
                for (int slot=0; slot<36; slot++) p.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            });
            s.runCommand("tp @p 0.5 100 0.5");
            readyToHit(c, world, SECOND);
            s.runOnServer(server -> winNormalRoll(player(server), SECOND));
            waitForDeath(c);
            respawn(c);
            s.waitFor(server -> player(server).isAlive() && !Practice.account(player(server)).pending().isEmpty());
            s.runOnServer(server -> {
                var p = player(server);
                check(p.getInventory().countItem(Items.COBBLESTONE) == 36 * 64, "full bag retained");
                check(p.level().getGameRules().get(GameRules.KEEP_INVENTORY), "keepInventory stays enabled");
                check(!p.level().getGameRules().get(GameRules.SHOW_DEATH_MESSAGES), "quiet world setting preserved");
                p.getInventory().setItem(0, ItemStack.EMPTY);
            });
            s.waitFor(server -> Practice.account(player(server)).pending().isEmpty());
            s.runOnServer(server -> {
                var p = player(server);
                check(Content.GIFTS.containsValue(p.getInventory().getItem(0).getItem()), "reserved gift arrives in the vacated slot");
                check(Practice.account(p).ascensions().intValueExact() == 2, "exactly two completed ceremonies");
            });
            check(chat.stream().filter(expected::equals).count() == 2, "one ascension announcement in a quiet world");
        }
        try (var world = saved.open()) {
            world.getServer().runOnServer(server -> {
                var a = Practice.account(player(server));
                check(a.ascensions().intValueExact() == 2 && a.pending().isEmpty() && a.rite().isEmpty(), "settled state survives reopening");
                check(a.history().values().stream().reduce(BigInteger.ZERO, BigInteger::add).intValueExact() == 2, "history and item counts agree");
            });
            world.getServer().runCommand("execute as @p run muyu");
            world.getServer().runCommand("execute as @p run muyu history");
            c.waitTicks(70);
            int beforeBell = chat.size();
            world.getServer().runOnServer(server -> checkBell(player(server)));
            c.waitFor(client -> Balance.VERSES.stream().anyMatch(id -> chat.subList(beforeBell, chat.size()).contains(
                    net.minecraft.network.chat.Component.translatable("muyu.verse." + id).getString())), 160);
        }
        checkHardcore(c);
    }
    private static void checkHardcore(ClientGameTestContext c) {
        var builder = c.worldBuilder().adjustSettings(ui -> ui.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.HARDCORE));
        try (var world = builder.create()) {
            var s = world.getServer();
            s.runCommand("fill -3 99 -3 4 99 5 minecraft:stone");
            s.runCommand("tp @p 0.5 100 0.5");
            s.runOnServer(server -> {
                check(server.isHardcore(), "test world is hardcore");
                server.overworld().setBlockAndUpdate(FIRST, Content.FISH.defaultBlockState());
                player(server).setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            });
            readyToHit(c, world, FIRST);
            s.runCommand("execute as @p run muyu rehearse");
            hit(c, FIRST);
            waitForDeath(c);
            respawn(c);
            s.waitFor(server -> Practice.account(player(server)).ascensions().equals(BigInteger.ONE));
            check(s.computeOnServer(server -> !player(server).isSpectator()), "blessed hardcore player returns to survival");
            world.getConnection().waitForClientboundPackets();
            s.waitFor(server -> player(server).isAlive() && player(server).connection.hasClientLoaded());
            s.runCommand("kill @p");
            waitForDeath(c);
            respawn(c);
            s.waitFor(server -> player(server).isSpectator());
            check(s.computeOnServer(server -> Practice.account(player(server)).ascensions().equals(BigInteger.ONE)), "ordinary hardcore death remains permanent");
        }
    }
    private static void readyToHit(ClientGameTestContext c, TestSingleplayerContext world, BlockPos pos) {
        c.waitFor(client -> client.gui.screen() == null && client.player != null
                && client.player.distanceToSqr(Vec3.atCenterOf(pos)) < 12 && client.level.getBlockState(pos).is(Content.FISH), 800);
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForServerboundPackets();
        world.getServer().waitFor(server -> player(server).connection.hasClientLoaded());
        c.waitTicks(8);
    }
    private static void winNormalRoll(ServerPlayer p, BlockPos pos) {
        // This is a fresh player after respawn. No rehearsal or omen cooldown is armed.
        var before = Practice.account(p);
        var tier = Balance.Tier.at(before.merit().add(BigInteger.ONE));
        check(tier.chance > 0 && before.rite().isEmpty(), "eligible account remains idle until a knock");
        var rng = p.level().getRandom();
        int seed = 0;
        for (; seed < 1_000_000; seed++) {
            rng.setSeed(seed);
            if (rng.nextInt(Balance.ROLL_SIZE) >= Balance.EVENT_CHANCE && tier.ascends(rng.nextInt(Balance.ROLL_SIZE))) break;
        }
        check(seed < 1_000_000, "find a real winning random draw for this rank");
        rng.setSeed(seed);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        p.level().getBlockState(pos).useItemOn(p.getMainHandItem(), p.level(), p, InteractionHand.MAIN_HAND, hit);
        var after = Practice.account(p);
        check(after.rite().isPresent() && after.merit().equals(before.merit().add(BigInteger.ONE)),
                "ordinary, unforced block interaction wins its independent random ascension roll");
    }
    private static void checkBell(ServerPlayer player) {
        var bell = Content.GIFTS.get(Balance.Gift.BELL);
        var stack = new ItemStack(bell);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var cooldowns = player.getCooldowns();
        var random = player.level().getRandom();
        for (var omen : Balance.Omen.values()) {
            cooldowns.removeCooldown(cooldowns.getCooldownGroup(stack));
            // Control only the disposable test world's next event draw.
            int seed = 0;
            for (; seed < 10_000; seed++) {
                random.setSeed(seed);
                if (Balance.draw(Balance.Omen.values(), random) == omen) break;
            }
            check(seed < 10_000, "seed for " + omen);
            random.setSeed(seed);
            var merit = Practice.account(player).merit();
            int xp = player.totalExperience;
            float health = player.getHealth();
            bell.use(player.level(), player, InteractionHand.MAIN_HAND);
            var expected = omen == Balance.Omen.BONUS ? merit.add(BigInteger.TEN) : merit;
            check(Practice.account(player).merit().equals(expected), "event merit " + omen);
            if (omen == Balance.Omen.EXPERIENCE) check(player.totalExperience - xp == Balance.AUSPICIOUS_XP, "dragon experience");
            if (omen == Balance.Omen.LIGHTNING) check(player.getHealth() < health, "real lightning damage");
            check(cooldowns.isOnCooldown(stack), "bell cooldown " + omen);
            bell.use(player.level(), player, InteractionHand.MAIN_HAND);
            check(Practice.account(player).merit().equals(expected) && Practice.account(player).rite().isEmpty(),
                    "bell cooldown and no passive ascension");
        }
    }
    private static void validateContent(MinecraftServer s) {
        DivineGearChecks.run(s);
        check(Content.FISH.asItem() == Content.FISH_ITEM, "pick block maps to wooden fish item");
        var key = ResourceKey.create(Registries.RECIPE, Muyu.id("wooden_fish"));
        var recipe = (ShapedRecipe)s.getRecipeManager().byKey(key).orElseThrow().value();
        var empty = ItemStack.EMPTY;
        var input = CraftingInput.of(3,3,List.of(empty,empty,new ItemStack(Items.STICK),
                new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.NOTE_BLOCK),new ItemStack(Items.BIRCH_PLANKS),
                empty,new ItemStack(Items.REDSTONE),empty));
        check(recipe.matches(input,s.overworld()) && recipe.assemble(input).is(Content.FISH_ITEM), "live shaped recipe");
        var sword = Content.GIFTS.get(Balance.Gift.SWORD).getDefaultInstance();
        var base = Items.NETHERITE_SWORD.getDefaultInstance();
        check(attack(sword) == 3 * attack(base), "Godslayer has triple the base damage of a netherite sword");
        s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(entry -> {
            var e = entry.value();
            check(e.isSupportedItem(sword) == e.isSupportedItem(base) && e.isPrimaryItem(sword) == e.isPrimaryItem(base), "enchantment parity " + entry.key());
        });
        var apple = Content.GIFTS.get(Balance.Gift.APPLE).getDefaultInstance();
        var enchanted = Items.ENCHANTED_GOLDEN_APPLE.getDefaultInstance();
        check(apple.get(DataComponents.FOOD).equals(enchanted.get(DataComponents.FOOD)) &&
                apple.get(DataComponents.CONSUMABLE).equals(enchanted.get(DataComponents.CONSUMABLE)), "vanilla enchanted apple effects");
    }
    private static double attack(ItemStack stack) {
        double[] result = {1};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, mod) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) result[0] += mod.amount();
        });
        return result[0];
    }
    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
    private static void hit(ClientGameTestContext c, BlockPos pos) {
        c.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
    }
    private static void awaitFrame(ClientGameTestContext c, int t) {
        c.waitFor(client -> LocalScene.current() != null && LocalScene.current().tick() >= t, 1600);
    }
    private static void respawn(ClientGameTestContext c) {
        c.runOnClient(client -> client.player.respawn());
        c.waitFor(client -> client.player != null && client.player.isAlive() && !(client.gui.screen() instanceof DeathScreen));
    }
    private static void waitForDeath(ClientGameTestContext c) {
        c.waitFor(client -> client.gui.screen() instanceof DeathScreen, 1600);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
