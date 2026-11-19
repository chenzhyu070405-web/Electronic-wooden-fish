package local.muyu.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.muyu.Practice;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Hardcore normally switches the new player to spectator just after copying their data. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class CeremonyRespawnMixin {
    @Shadow public ServerPlayer player;

    @ModifyExpressionValue(method = "handleClientCommand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isHardcore()Z"))
    private boolean muyu$allowBlessedRespawn(boolean hardcore) {
        return hardcore && !Practice.consumeHardcoreReturn(player);
    }
}
