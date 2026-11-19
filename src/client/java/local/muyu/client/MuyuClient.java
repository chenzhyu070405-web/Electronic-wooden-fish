package local.muyu.client;

import local.muyu.Content;
import local.muyu.LocalScene;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class MuyuClient implements ClientModInitializer {
    private static boolean heldKnock;

    /** A held use binding generates multiple vanilla startUseItem calls. Consume only its first edge. */
    public static boolean firstPress() {
        if (heldKnock) return false;
        heldKnock = true;
        return true;
    }

    @Override public void onInitializeClient() {
        GoldenLotusRenderer.initialize();
        ParticleProviderRegistry.getInstance().register(Content.LOTUS, LotusParticle.Provider::new);
        ModelLayerRegistry.registerModelLayer(BuddhaModel.LAYER, BuddhaModel::layer);
        EntityRenderers.register(Content.BUDDHA, BuddhaRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(MuyuClient::tick);
    }
    private static void tick(Minecraft client) {
        if (!client.options.keyUse.isDown() || client.level == null) heldKnock = false;
        var frame = LocalScene.current();
        if (client.player == null || client.level == null || !client.hasSingleplayerServer()) return;
        boolean active = frame != null && frame.viewer().equals(client.player.getUUID()) && client.player.isAlive();
        var screen = client.gui.screen();
        if (active && screen == null) client.gui.setScreen(new SanctuaryScreen(client.level));
        else if (!active && screen instanceof SanctuaryScreen) client.gui.setScreen(null);
    }
}
