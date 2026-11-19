package local.muyu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.muyu.Content;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
abstract class CeremonyMessageMixin {
    /** Use vanilla's single broadcast and death-screen packet, including in quiet worlds. */
    @WrapOperation(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object muyu$announcePassage(GameRules rules, GameRule<?> rule, Operation<Object> original, DamageSource source) {
        if (rule == GameRules.SHOW_DEATH_MESSAGES && source.is(Content.WEST)) return true;
        return original.call(rules, rule);
    }
}
