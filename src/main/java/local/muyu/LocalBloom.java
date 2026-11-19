package local.muyu;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** A one-shot cue shared by the integrated game and its own client. */
public record LocalBloom(UUID player, ResourceKey<Level> dimension, float yaw) {
    private static final AtomicReference<LocalBloom> NEXT = new AtomicReference<>();
    static void start(ServerPlayer player) {
        NEXT.set(new LocalBloom(player.getUUID(), player.level().dimension(), player.getYRot()));
    }
    public static LocalBloom take() { return NEXT.getAndSet(null); }
    static void clear() { NEXT.set(null); }
}
