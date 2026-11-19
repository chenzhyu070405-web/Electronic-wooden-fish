package local.muyu.client;

import local.muyu.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class GoldenLotusGameTest implements FabricClientGameTest {
    private static final BlockPos FISH = new BlockPos(0,101,3);
    private record Sample(float age, Vec3 feet, boolean first, float yaw, float pitch) {}
    private volatile Sample latest;

    @Override public void runTest(ClientGameTestContext c) {
        c.runOnClient(client -> {
            client.options.pauseOnLostFocus = false;
            client.options.languageCode = "zh_cn";
            client.getLanguageManager().setSelected("zh_cn");
            client.reloadResourcePacks();
            LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
                var frame = ctx.levelState().getData(GoldenLotusRenderer.FRAME);
                var camera = ctx.levelState().cameraRenderState;
                latest = frame == null ? null : new Sample(frame.age(), frame.feetFromEye().add(camera.pos),
                        frame.firstPerson(), camera.yRot, camera.xRot);
            });
        });
        c.waitFor(client -> net.minecraft.network.chat.Component.translatable("muyu.temple").getString().equals("云上莲台"),1200);
        try (var world = c.worldBuilder().create()) {
            var s = world.getServer();
            s.runCommand("difficulty peaceful");
            s.runCommand("fill -10 99 -10 10 99 10 minecraft:smooth_stone");
            s.runCommand("fill -1 99 -9 -1 99 9 minecraft:polished_andesite");
            s.runCommand("fill 3 99 -9 3 99 9 minecraft:polished_andesite");
            s.runCommand("setblock 0 100 3 minecraft:oak_log");
            s.runCommand("setblock 0 101 3 muyu:wooden_fish");
            s.runCommand("tp @p 0.5 100 0.5 0 0");
            s.runCommand("time set noon");
            world.getConnection().waitForChunksDownload();
            c.waitFor(client -> client.level.getBlockState(FISH).is(Content.FISH) && client.player.getY() == 100);
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            c.getInput().lookAt(0, 0);
            c.waitTicks(8);
            trigger(world,true);
            sample(c,24,true);
            c.takeScreenshot("halo-eye-rise");
            sample(c,28,true);
            require(Math.abs(latest.pitch)<1, "level first-person camera");
            c.takeScreenshot("halo-eye-level");
            c.getInput().lookAt(34, 0);
            sample(c,38,true);
            require(Math.abs(latest.yaw)>20, "player can turn inside the bloom");
            c.takeScreenshot("halo-eye-turn");
            c.waitFor(client -> latest==null, 120);

            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            c.getInput().lookAt(32,55);
            c.getInput().pressKey(o -> o.keyToggleGui);
            c.waitTicks(3);
            trigger(world,false);
            sample(c,9,false);
            c.takeScreenshot("halo-open");
            c.getInput().lookAt(32,23);
            sample(c,27,false);
            c.takeScreenshot("halo-fold");
            sample(c,39,false);
            c.takeScreenshot("halo-bud");
            sample(c,48,false);
            c.takeScreenshot("halo-dissolve");
            sample(c,56,false);
            c.takeScreenshot("halo-seat");
            c.waitFor(client -> latest==null,120);

            c.getInput().lookAt(32,45);
            trigger(world,false);
            sample(c,10,false);
            var before = latest.feet;
            c.takeScreenshot("halo-walk-start");
            c.getInput().holdKey(o -> o.keyRight);
            try {
                sample(c,33,false);
                c.takeScreenshot("halo-walk-end");
                var moved = latest.feet.distanceTo(before);
                require(moved > 1.0, "feet moved with the player: " + moved);
                c.runOnClient(client -> require(client.player.position().distanceTo(latest.feet)<.45,
                        "rendered flower follows interpolated player feet"));
                System.out.println("Golden lotus: level first person, head turn, third-person open/bud/dissolve, walking follow " + moved + " blocks.");
            } finally { c.getInput().releaseKey(o -> o.keyRight); }
            c.waitFor(client -> latest==null,120);
            s.runCommand("item replace entity @p armor.head with muyu:divine_helmet");
            s.runCommand("item replace entity @p armor.chest with muyu:divine_chestplate");
            s.runCommand("item replace entity @p armor.legs with muyu:divine_leggings");
            s.runCommand("item replace entity @p armor.feet with muyu:divine_boots");
            s.runCommand("item replace entity @p weapon.mainhand with muyu:divine_pickaxe");
            c.waitFor(client -> client.player.getMainHandItem().is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    Muyu.id("divine_pickaxe"))));
            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            c.getInput().lookAt(130, -9);
            c.waitTicks(5);
            c.takeScreenshot("divine-equipment");
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            c.getInput().pressKey(o -> o.keyToggleGui);
            s.runCommand("gamemode survival @p");
            s.runOnServer(server -> {
                var player=server.getPlayerList().getPlayers().getFirst();
                int slot=9;
                for (String kind : new String[]{"helmet","chestplate","leggings","boots","horse_armor","nautilus_armor",
                        "sword","pickaxe","axe","shovel","hoe","spear"}) {
                    var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Muyu.id("divine_"+kind));
                    player.getInventory().setItem(slot++,new ItemStack(item));
                }
            });
            c.waitFor(client -> client.player.getInventory().getItem(20).is(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Muyu.id("divine_spear"))));
            c.getInput().pressKey(o -> o.keyInventory);
            c.waitForScreen(net.minecraft.client.gui.screens.inventory.InventoryScreen.class);
            c.getInput().setCursorPos(4,4);
            c.takeScreenshot("divine-inventory");
            c.runOnClient(client -> client.gui.screen().onClose());
        }
    }

    private void sample(ClientGameTestContext c, int tick, boolean first) {
        c.waitFor(client -> latest!=null && latest.age>=tick && latest.first==first,120);
    }

    /** Select a reproducible native roll; use a real wooden fish / bell in this disposable world. */
    private static void trigger(TestSingleplayerContext w, boolean knock) {
        w.getServer().runOnServer(s -> {
            ServerPlayer p=s.getPlayerList().getPlayers().getFirst();
            var random=p.level().getRandom();
            int seed=0;
            for (;seed<1_000_000;seed++) {
                random.setSeed(seed);
                if (knock && random.nextInt(Balance.ROLL_SIZE)>=Balance.EVENT_CHANCE) continue;
                if (Balance.draw(Balance.Omen.values(),random)==Balance.Omen.HALO) break;
            }
            require(seed<1_000_000,"halo seed");
            if (knock) {
                random.setSeed(seed);
                p.level().getBlockState(FISH).useWithoutItem(p.level(),p,
                        new BlockHitResult(Vec3.atCenterOf(FISH),Direction.NORTH,FISH,false));
                require(Practice.account(p).merit().intValueExact()==1,"ordinary knock still credited");
            } else {
                var bell=Content.GIFTS.get(Balance.Gift.BELL);
                var stack=new ItemStack(bell);
                p.setItemInHand(InteractionHand.MAIN_HAND,stack);
                p.getCooldowns().removeCooldown(p.getCooldowns().getCooldownGroup(stack));
                random.setSeed(seed);
                bell.use(p.level(),p,InteractionHand.MAIN_HAND);
            }
        });
    }
    private static void require(boolean value,String text) { if(!value) throw new AssertionError(text); }
}
