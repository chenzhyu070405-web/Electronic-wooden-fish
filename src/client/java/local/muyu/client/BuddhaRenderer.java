package local.muyu.client;

import local.muyu.BuddhaEffigy;
import local.muyu.Muyu;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

final class BuddhaRenderer extends MobRenderer<BuddhaEffigy, BuddhaRenderer.Pose, BuddhaModel> {
    static final class Pose extends LivingEntityRenderState { float ceremonyAge; }
    private static final Identifier SKIN = Muyu.id("textures/entity/buddha.png");
    BuddhaRenderer(EntityRendererProvider.Context ctx) { super(ctx, new BuddhaModel(ctx.bakeLayer(BuddhaModel.LAYER)), 0); }
    @Override public Pose createRenderState() { return new Pose(); }
    @Override public Identifier getTextureLocation(Pose state) { return SKIN; }
    @Override public void extractRenderState(BuddhaEffigy figure, Pose state, float partial) {
        super.extractRenderState(figure, state, partial);
        state.ceremonyAge = figure.ceremonyAge;
    }
}
