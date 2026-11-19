package local.muyu.client.mixin;

import local.muyu.Content;
import local.muyu.client.MuyuClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class UseKeyMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void muyu$oneKnockPerPress(CallbackInfo callback) {
        var client = (Minecraft)(Object)this;
        if (client.level != null && client.hasSingleplayerServer() && client.hitResult instanceof BlockHitResult hit
                && client.level.getBlockState(hit.getBlockPos()).is(Content.FISH) && !MuyuClient.firstPress())
            callback.cancel();
    }
}
