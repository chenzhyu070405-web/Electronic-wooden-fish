package local.muyu;

import java.util.UUID;

/** Only immutable frame data crosses the two threads of the local game. */
public final class LocalScene {
    public record Frame(UUID viewer, String riteId, int tick, String line, Balance.Gift gift) {}
    private static volatile Frame current;
    private static volatile Frame drawn;

    public static Frame current() { return current; }
    public static void presented(Frame frame) { drawn = frame; }
    static boolean wasPresented(Frame frame) { return frame != null && frame == drawn; }
    static void show(Frame frame) { current = frame; }
    static void clear() { current = null; drawn = null; }
    private LocalScene() {}
}
