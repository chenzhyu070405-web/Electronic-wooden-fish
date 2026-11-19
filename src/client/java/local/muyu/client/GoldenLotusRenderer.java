package local.muyu.client;

import local.muyu.LocalBloom;
import local.muyu.LocalScene;
import local.muyu.Muyu;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

final class GoldenLotusRenderer {
    record Frame(Vec3 feetFromEye, Vec3 forward, boolean firstPerson, float age, float yaw) {}
    static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create(() -> "muyu golden lotus");
    private static final Identifier GOLD = Muyu.id("textures/effect/lotus_gold.png");
    private static LocalPlayer owner;
    private static int started;
    private static float yaw;

    static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cue = LocalBloom.take();
            if (client.player != owner || owner != null && (!owner.isAlive() || owner.tickCount - started >= LotusAnimation.END))
                owner = null;
            if (cue != null && client.player != null && client.hasSingleplayerServer()
                    && cue.player().equals(client.player.getUUID()) && cue.dimension().equals(client.level.dimension())) {
                owner = client.player;
                started = owner.tickCount;
                yaw = cue.yaw();
            }
            if (LocalScene.current() != null) owner = null;
        });
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            if (owner == null || owner != Minecraft.getInstance().player || owner.level() != context.level()) return;
            var camera = context.camera();
            float partial = context.deltaTracker().getGameTimeDeltaPartialTick(false);
            var feet = owner.getPosition(partial);
            var f = camera.forwardVector();
            context.levelState().setData(FRAME, new Frame(feet.subtract(camera.position()), new Vec3(f.x(), f.y(), f.z()),
                    !camera.isDetached(), owner.tickCount - started + partial, yaw));
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            var frame = context.levelState().getData(FRAME);
            if (frame == null || frame.age() >= LotusAnimation.END) return;
            var poses = context.poseStack();
            var offset = frame.feetFromEye();
            poses.pushPose();
            poses.translate(offset.x, offset.y, offset.z);
            context.submitNodeCollector().submitCustomGeometry(poses, RenderTypes.entityTranslucentCull(GOLD),
                    (pose, vertices) -> new LotusAnimation(pose, vertices, frame).draw());
            poses.popPose();
        });
    }
    private GoldenLotusRenderer() {}
}
