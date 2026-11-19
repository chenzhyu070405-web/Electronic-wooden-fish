package local.muyu.client;

import local.muyu.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import static local.muyu.Balance.*;

public final class SanctuaryScreen extends Screen {
    private static final Identifier HALL = Muyu.id("textures/gui/cloud_hall.png");
    private static final Identifier CLOUD = Muyu.id("textures/gui/cloud.png");
    private final BuddhaEffigy buddha;

    SanctuaryScreen(ClientLevel level) {
        super(Component.translatable("muyu.temple"));
        buddha = new BuddhaEffigy(Content.BUDDHA, level);
        // GUI-only entity: the renderer needs a nonzero ID, but the level never tracks it.
        buddha.setId(-1);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {}
    @Override public void onClose() {
        minecraft.gui.setScreen(null);
        minecraft.gui.setPauseScreen(false, false);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        var frame = LocalScene.current();
        if (frame == null || minecraft.player == null || !frame.viewer().equals(minecraft.player.getUUID())) return;
        float t = frame.tick() + Mth.clamp(partial, 0, 1);
        if (frame.tick() < TEMPLE) {
            g.fillGradient(0, 0, width, height, tint(0xffe7a4, 0.54F * t / TEMPLE), tint(0xfff3c2, 0.24F * t / TEMPLE));
            motes(g, t, 28);
            veil(g, (t - 48) / (TEMPLE - 48));
            if (t < 68) caption(g, Component.translatable("muyu.ascent"), height * 3 / 4);
        } else {
            drawTemple(g, t, mouseX, mouseY);
            g.nextStratum();
            if (t < SPEAK) caption(g, Component.translatable("muyu.temple"), height * 85 / 100);
            else if (t < BLESS) caption(g, Component.translatable("muyu.line." + frame.line()), height * 85 / 100);
            else if (t < POINT) {
                var stack = Content.GIFTS.get(frame.gift()).getDefaultInstance();
                presentGift(g, stack, t);
                caption(g, Component.translatable("muyu.bestow", minecraft.player.getName(), stack.getHoverName()), height * 88 / 100);
            } else caption(g, Component.translatable("muyu.passage"), height * 85 / 100);
            veil(g, Math.max((TEMPLE + 16 - t) / 16, (t - FINALE + 18) / 18));
        }
        if (minecraft.gui.overlay() == null && !minecraft.getWindow().isIconified()
                && (minecraft.isWindowActive() || !minecraft.options.pauseOnLostFocus)) LocalScene.presented(frame);
    }

    private void presentGift(GuiGraphicsExtractor g, net.minecraft.world.item.ItemStack gift, float t) {
        float progress = Mth.clamp((t - BLESS) / 19, 0, 1);
        float ease = progress * (2 - progress);
        float x = width / 2F - (1 - ease) * height * 0.10F;
        float y = height * (0.50F + 0.285F * ease) + Mth.sin((t - BLESS) * 0.12F) * 1.2F;
        float scale = Math.max(0.7F, Math.min(height / 160F, 1.65F)) * (0.65F + 0.35F * ease);
        for (int i = 0; i < 8; i++) {
            float angle = i * Mth.TWO_PI / 8 + t * 0.022F;
            int dx = Math.round(x + Mth.cos(angle) * 11 * scale);
            int dy = Math.round(y + Mth.sin(angle) * 8 * scale);
            g.fill(dx, dy, dx + 2, dy + 1, 0xdffff0a9);
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y).scale(scale);
        g.item(gift, -8, -8);
        pose.popMatrix();
    }

    private void drawTemple(GuiGraphicsExtractor g, float t, int mouseX, int mouseY) {
        // Preserve the painted perspective and circle even on ultrawide and narrow windows.
        float scale = Math.max(width / 512F, height / 288F);
        int w = Math.round(512 * scale), h = Math.round(288 * scale);
        g.blit(RenderPipelines.GUI_TEXTURED, HALL, (width-w)/2, (height-h)/2, 0, 0, w, h, 512, 288, 512, 288);
        int cloudWidth = Math.max(64, width / 3);
        for (int i = 0; i < 5; i++) {
            int x = (int)((i * width * 0.29 + t * (0.18 + i * 0.035)) % (width + cloudWidth)) - cloudWidth;
            int y = height * (48 + (i % 3) * 9) / 100;
            g.blit(RenderPipelines.GUI_TEXTURED, CLOUD, x, y, 0, 0, cloudWidth, cloudWidth / 5, 96, 20, 96, 20);
        }
        motes(g, t, 42);
        g.nextStratum();
        buddha.ceremonyAge = t;
        int cx = width / 2, cy = height * 46 / 100;
        int size = Math.min(height * 23 / 100, width / 5);
        float gazeX = cx + Mth.clamp((mouseX - cx) * 0.06F, -8, 8);
        float gazeY = cy + Mth.clamp((mouseY - cy) * 0.04F, -4, 4);
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, 0, 0, width, cy * 2, Math.max(1, size),
                0, gazeX, gazeY, buddha);
    }

    private void motes(GuiGraphicsExtractor g, float t, int count) {
        for (int i = 0; i < count; i++) {
            int x = Math.floorMod((int)(i * 73 + Math.sin(i + t * 0.015) * 9), Math.max(1, width));
            int y = Math.floorMod((int)(i * 43 - t * (0.24 + (i % 5) * 0.07)), Math.max(1, height));
            int color = tint(0xffe7a0, 0.5F + 0.28F * Mth.sin(i + t * 0.055F));
            g.fill(x, y, x + 2, y + 1, color);
            if (i % 5 == 0) g.fill(x, y - 1, x + 1, y + 2, color);
        }
    }
    private void caption(GuiGraphicsExtractor g, Component message, int y) {
        var lines = font.split(message, Math.max(36, width - 32));
        int max = 0;
        for (var line : lines) max = Math.max(max, font.width(line));
        int top = Math.min(y, height - lines.size() * 10 - 6);
        g.fill(width/2 - max/2 - 9, top - 4, width/2 + max/2 + 9, top + lines.size()*10 + 3, 0xcf35251e);
        for (var line : lines) {
            g.centeredText(font, line, width/2, top, 0xffffedb4);
            top += 10;
        }
    }
    private void veil(GuiGraphicsExtractor g, float opacity) {
        if (opacity > 0) {
            g.nextStratum();
            g.fill(0, 0, width, height, tint(0xfff9e6, opacity));
        }
    }
    private static int tint(int rgb, float alpha) { return (Math.round(255 * Mth.clamp(alpha, 0, 1)) << 24) | rgb; }
}
