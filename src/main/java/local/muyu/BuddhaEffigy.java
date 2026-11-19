package local.muyu;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** Created only for the sanctuary camera. No world entity is spawned or saved. */
public final class BuddhaEffigy extends PathfinderMob {
    public float ceremonyAge;
    public BuddhaEffigy(EntityType<? extends BuddhaEffigy> type, Level level) {
        super(type, level);
        setNoAi(true);
    }
}
