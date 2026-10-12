package org.github.gabrielmartins.command.core.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public sealed class CommandContext permits CommandContext.Console, CommandContext.PlayerContext {

    private final CommandSender sender;
    private final String label;
    private final String[] rawArgs;
    private final List<String> args;

    public Optional<Player> player() {
        return sender instanceof Player p ? Optional.of(p) : Optional.empty();
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public String arg(int index) {
        return index >= 0 && index < args.size() ? args.get(index) : null;
    }

    public int argInt(int index, int fallback) {
        String value = arg(index);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void info(String message) {
        sender.sendMessage(message);
    }

    public static final class Console extends CommandContext {
        public Console(CommandSender sender, String label, String[] raw, List<String> args) {
            super(sender, label, raw, args);
        }
    }

    public static final class PlayerContext extends CommandContext {
        @Getter
        private final Player playerInstance;

        public PlayerContext(Player player, String label, String[] raw, List<String> args) {
            super(player, label, raw, args);
            this.playerInstance = player;
        }
    }
}
