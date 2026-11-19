package local.muyu;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class RelicArtGameTest implements FabricClientGameTest {
    private static final BlockPos FISH = new BlockPos(0,100,0);
    private static final BlockPos FRAME = new BlockPos(0,101,4);

    @Override public void runTest(ClientGameTestContext c) {
        c.runOnClient(client -> {
            client.options.pauseOnLostFocus = false;
            client.options.languageCode = "zh_cn";
            client.getLanguageManager().setSelected("zh_cn");
            client.reloadResourcePacks();
        });
        c.waitFor(client -> net.minecraft.network.chat.Component.translatable("muyu.temple").getString().equals("云上莲台"),1200);
        try (var world = c.worldBuilder().create()) {
            var s = world.getServer();
            s.runCommand("difficulty peaceful");
            s.runCommand("time set noon");
            s.runCommand("fill -9 99 -9 9 99 9 minecraft:smooth_sandstone");
            s.runCommand("fill -8 100 5 8 105 5 minecraft:polished_tuff");
            s.runCommand("gamemode survival @p");
            s.runCommand("tp @p 1.85 100 -1.30");
            s.runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.level().setBlockAndUpdate(FISH, Content.FISH.defaultBlockState().setValue(WoodenFishBlock.FACING,Direction.NORTH));
                var frame = new ItemFrame(p.level(),FRAME,Direction.NORTH);
                frame.setItem(new ItemStack(Content.GIFTS.get(Balance.Gift.SWORD)),false);
                p.level().addFreshEntity(frame);
            });
            world.getConnection().waitForChunksRender();
            c.waitFor(client -> client.player != null && client.player.getY()==100 && client.level.getBlockState(FISH).is(Content.FISH));
            c.getInput().lookAt(FISH);
            c.getInput().pressKey(options -> options.keyToggleGui);
            c.waitTicks(5);
            c.takeScreenshot("redwood-rest");
            s.runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.level().getRandom().setSeed(42);
                p.level().getBlockState(FISH).useWithoutItem(p.level(),p,
                        new BlockHitResult(Vec3.atCenterOf(FISH),Direction.NORTH,FISH,false));
                if (!p.level().getBlockState(FISH).getValue(WoodenFishBlock.STRUCK))
                    throw new AssertionError("valid knock must select the contact pose");
            });
            c.waitTicks(Balance.STRIKE_TICKS+2);
            s.runOnServer(server -> {
                var level = server.overworld();
                var rest = level.getBlockState(FISH);
                if (rest.getValue(WoodenFishBlock.STRUCK)) throw new AssertionError("mallet must return to rest");
                // Hold the same contact model still for the screenshot after checking the real rebound.
                level.setBlockAndUpdate(FISH,rest.setValue(WoodenFishBlock.STRUCK,true));
            });
            c.waitFor(client -> client.level.getBlockState(FISH).getValue(WoodenFishBlock.STRUCK));
            c.waitTicks(3);
            c.takeScreenshot("redwood-strike");
            s.runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.level().setBlockAndUpdate(FISH,p.level().getBlockState(FISH).setValue(WoodenFishBlock.STRUCK,false));
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.GIFTS.get(Balance.Gift.SWORD)));
                p.getInventory().setItem(1,new ItemStack(Content.FISH_ITEM));
            });
            s.runCommand("tp @p 0.5 100 3.30 0 0");
            c.waitFor(client -> client.player.getZ()>3 && client.player.getMainHandItem().is(Content.GIFTS.get(Balance.Gift.SWORD)));
            c.getInput().lookAt(0,4);
            c.waitTicks(5);
            c.takeScreenshot("godslayer-framed");
            c.getInput().pressKey(options -> options.keyToggleGui);
            s.runCommand("tp @p 0.5 100 1.70 0 0");
            c.waitFor(client -> client.player.getZ()<2);
            c.waitTicks(5);
            c.takeScreenshot("godslayer-hotbar");
            c.getInput().pressKey(options -> options.keyInventory);
            c.waitForScreen(InventoryScreen.class);
            c.getInput().setCursorPos(5,5);
            c.takeScreenshot("godslayer-inventory");
            c.runOnClient(client -> client.gui.screen().onClose());
        }
    }
}
