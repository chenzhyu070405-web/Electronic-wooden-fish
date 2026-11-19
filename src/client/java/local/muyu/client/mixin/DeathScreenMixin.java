package local.muyu.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
abstract class DeathScreenMixin {
    @ModifyExpressionValue(method = "handlePlayerCombatKill", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;isHardcore()Z"))
    private boolean muyu$showRespawnButton(boolean hardcore, ClientboundPlayerCombatKillPacket packet) {
        return hardcore && !(packet.message().getContents() instanceof TranslatableContents text
                && text.getKey().startsWith("death.attack.muyu.ascension"));
    }
}
