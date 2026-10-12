package org.github.gabrielmartins.command.tree;

import lombok.Getter;
import org.github.gabrielmartins.command.annotation.platform.Platform;

import java.lang.invoke.MethodHandle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class CommandNode {

    private final String name;
    private final Map<String, CommandNode> children = new LinkedHashMap<>();
    private final List<String> aliases;

    private MethodHandle handler;
    private Platform platform;
    private String permission;
    private String description;
    private String usage;

    public CommandNode(String name, List<String> aliases) {
        this.name = name;
        this.aliases = aliases == null ? List.of() : aliases;
    }

    public CommandNode child(String name, List<String> aliases) {
        return children.computeIfAbsent(name, key -> new CommandNode(key, aliases));
    }

    public CommandNode find(String token) {
        CommandNode direct = children.get(token);
        if (direct != null) return direct;
        return children.values().stream()
                .filter(node -> node.aliases.contains(token))
                .findFirst()
                .orElse(null);
    }

    public boolean isExecutable() {
        return handler != null;
    }

    public void bind(MethodHandle handler, Platform platform, String permission, String description, String usage) {
        this.handler = handler;
        this.platform = platform;
        this.permission = permission;
        this.description = description;
        this.usage = usage;
    }
}
