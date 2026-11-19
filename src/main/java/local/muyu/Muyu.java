package local.muyu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class Muyu implements ModInitializer {
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("muyu", path); }
    public static Component gold(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GOLD);
    }
    @Override public void onInitialize() {
        Content.initialize();
        Practice.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
                Commands.literal("muyu").executes(ctx -> status(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("history").executes(ctx -> history(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("claim").executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException();
                    Practice.deliver(p);
                    p.sendSystemMessage(gold(Practice.account(p).pending().isEmpty() ? "muyu.claim.empty" : "muyu.claim.wait"));
                    return 1;
                }))
                .then(Commands.literal("rehearse").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> { Practice.rehearse(ctx.getSource().getPlayerOrException()); return 1; }))));
    }
    private static int status(ServerPlayer p) {
        var a = Practice.account(p);
        var tier = Balance.Tier.at(a.merit());
        var next = tier.next();
        p.sendSystemMessage(gold("muyu.status", a.merit().toString(), Component.translatable(tier.key())));
        p.sendSystemMessage(next == null ? gold("muyu.boundless") :
                gold("muyu.next", Component.translatable(next.key()), next.minimum.subtract(a.merit()).toString()));
        return 1;
    }
    private static int history(ServerPlayer p) {
        var a = Practice.account(p);
        p.sendSystemMessage(gold("muyu.record", a.ascensions().toString(), a.peak().toString()));
        Component last = a.lastAscension() == 0 ? Component.translatable("muyu.never") : Component.literal(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(a.lastAscension())));
        p.sendSystemMessage(gold("muyu.last", last));
        for (var gift : Balance.Gift.values()) if (a.history().containsKey(gift))
            p.sendSystemMessage(gold("muyu.collected", Content.GIFTS.get(gift).getDefaultInstance().getHoverName(), a.history().get(gift).toString()));
        if (!a.pending().isEmpty()) p.sendSystemMessage(gold("muyu.claim.wait"));
        return 1;
    }
}
