package local.muyu.client;

import local.muyu.Balance;
import local.muyu.Muyu;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Robed, seated figure with curled hair, urna, long earlobes and a thirteen-petal lotus throne. */
final class BuddhaModel extends EntityModel<BuddhaRenderer.Pose> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(Muyu.id("buddha"), "sanctuary");
    private final ModelPart chest, head, benediction, palm;

    BuddhaModel(ModelPart root) {
        super(root);
        chest = root.getChild("chest");
        head = chest.getChild("head");
        benediction = chest.getChild("benediction");
        palm = benediction.getChild("palm");
    }
    private static CubeListBuilder box(int u, int v, float x, float y, float z, int w, int h, int d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }
    static LayerDefinition layer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("plinth", box(0, 98, -10, 21, -8, 20, 3, 16)
                .texOffs(0, 98).addBox(-11, 20, -9, 22, 2, 18), PartPose.ZERO);
        for (int n = 0; n < 13; n++) {
            float angle = n * Mth.TWO_PI / 13;
            root.addOrReplaceChild("petal_" + n, box(52, 64, -2.5F, -1, -2, 5, 4, 4)
                    .texOffs(52, 64).addBox(-1.5F, -2.5F, -1.5F, 3, 2, 3),
                    PartPose.offsetAndRotation(Mth.sin(angle) * 9.5F, 18.5F, Mth.cos(angle) * 7.6F, 0.46F, angle, 0));
        }
        root.addOrReplaceChild("folded_legs", box(48, 0, -10, 13, -5, 20, 5, 10)
                .texOffs(0, 34).addBox(-4, 13, -6, 8, 3, 3), PartPose.ZERO);
        var chest = root.addOrReplaceChild("chest", box(48, 0, -6, -10, -3.5F, 12, 11, 7)
                .texOffs(48, 0).addBox(-7, -9, -3, 14, 3, 6), PartPose.offset(0, 14, 0));
        chest.addOrReplaceChild("sash", box(88, 34, -2, 0, -0.45F, 4, 12, 1),
                PartPose.offsetAndRotation(3.8F, -10.4F, -3.55F, 0, 0, 0.55F));
        var head = chest.addOrReplaceChild("head", box(0, 0, -5, -9, -4, 10, 10, 8)
                .texOffs(0, 34).addBox(-6.1F, -4.5F, -1.4F, 2, 6, 2)
                .texOffs(0, 34).addBox(4.1F, -4.5F, -1.4F, 2, 6, 2), PartPose.offset(0, -10.7F, -0.1F));
        head.addOrReplaceChild("curls", box(0, 64, -5.3F, -11.3F, -4.35F, 11, 4, 9)
                .texOffs(0, 64).addBox(-3, -13.7F, -2.6F, 6, 3, 6)
                .texOffs(0, 98).addBox(-1.3F, -15, -1.2F, 3, 2, 3), PartPose.ZERO);
        var right = chest.addOrReplaceChild("benediction", box(48, 0, -2.5F, -1, -2.5F, 5, 6, 5),
                PartPose.offset(-6.3F, -8.5F, -0.3F));
        right.addOrReplaceChild("palm", box(0, 34, -1.8F, -1, -1.5F, 4, 6, 3)
                .texOffs(0, 34).addBox(1.4F, 2, -1.4F, 2, 3, 2), PartPose.offset(0, 4.8F, -0.2F));
        chest.addOrReplaceChild("resting_arm", box(48, 0, -2, 0, -2, 4, 8, 5),
                PartPose.offsetAndRotation(6, -8.6F, -0.2F, -0.42F, 0, -0.12F));
        chest.addOrReplaceChild("resting_hand", box(0, 34, -3, -1, -2, 6, 3, 4),
                PartPose.offsetAndRotation(4.2F, -0.7F, -3.4F, 0, 0.2F, -0.13F));
        return LayerDefinition.create(mesh, 128, 128);
    }
    @Override public void setupAnim(BuddhaRenderer.Pose pose) {
        super.setupAnim(pose);
        float t = pose.ceremonyAge;
        float bless = Mth.clamp((t - Balance.BLESS + 16) / 34, 0, 1);
        float point = Mth.clamp((t - Balance.POINT) / 20, 0, 1);
        head.yRot = pose.yRot * Mth.DEG_TO_RAD * 0.7F;
        head.xRot = 0.06F + pose.xRot * Mth.DEG_TO_RAD * 0.6F + 0.07F * Mth.sin((t - Balance.SPEAK) * 0.08F);
        chest.y += Mth.sin(t * 0.035F) * 0.16F;
        benediction.xRot = -0.55F - bless * 0.85F - point * 0.38F;
        benediction.zRot = 0.20F + 0.20F * bless;
        palm.xRot = -1.75F + point * 1.30F;
    }
}
