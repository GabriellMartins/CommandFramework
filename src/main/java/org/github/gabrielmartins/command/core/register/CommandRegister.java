package org.github.gabrielmartins.command.core.register;

import lombok.SneakyThrows;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.github.gabrielmartins.command.BukkitCommand;
import org.github.gabrielmartins.command.core.scanner.ClassScanner;
import org.github.gabrielmartins.command.annotation.platform.Platform;
import org.github.gabrielmartins.command.core.context.CommandContext;
import org.github.gabrielmartins.command.tree.CommandNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandRegister {

    private final ClassScanner scanner = new ClassScanner();
    private final Map<String, CommandNode> roots = new LinkedHashMap<>();
    public void create(String packageName) {
        for (Class<?> type : scanner.scan(BukkitCommand.getInstance(), packageName)) {
            List<Method> handlers = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(org.github.gabrielmartins.command.annotation.Command.class))
                    .toList();
            if (handlers.isEmpty()) continue;

            Object instance = instantiate(type);
            if (instance == null) continue;

            for (Method method : handlers) {
                registerMethod(instance, method);
            }
        }

        roots.values().forEach(this::registerRoot);
    }

    private void registerMethod(Object instance, Method method) {
        org.github.gabrielmartins.command.annotation.Command meta = method.getAnnotation(org.github.gabrielmartins.command.annotation.Command.class);

        MethodHandle handle = bind(instance, method);
        if (handle == null) return;

        String[] path = meta.name().trim().split("\\s+");
        String rootName = path[0];

        CommandNode node = roots.computeIfAbsent(rootName,
                key -> new CommandNode(key, List.of(meta.aliases())));

        for (int i = 1; i < path.length; i++) {
            node = node.child(path[i], List.of());
        }

        node.bind(handle, meta.platform(), meta.permission(), meta.description(), meta.usage());
    }

    @SneakyThrows
    private void registerRoot(CommandNode root) {
        CommandMap commandMap = resolveCommandMap();

        Command command = new Command(root.getName()) {

            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                dispatch(root, sender, label, args);
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return suggest(root, args);
            }
        };

        command.setAliases(root.getAliases());
        command.setUsage(root.getUsage());
        command.setDescription(root.getDescription());

        commandMap.register(BukkitCommand.getInstance().getName().toLowerCase(), command);
    }

    private void dispatch(CommandNode root, CommandSender sender, String label, String[] args) {
        CommandNode node = root;
        int consumed = 0;

        while (consumed < args.length) {
            CommandNode next = node.find(args[consumed]);
            if (next == null) break;
            node = next;
            consumed++;
        }

        if (!node.isExecutable()) {
            sender.sendMessage("§cSubcomando inválido. Use §f/" + root.getName() + " help§c.");
            return;
        }

        Platform platform = node.getPlatform();
        if (platform == Platform.PLAYER && !(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return;
        }
        if (platform == Platform.CONSOLE && sender instanceof Player) {
            sender.sendMessage("§cApenas o console pode usar este comando.");
            return;
        }

        String permission = node.getPermission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            sender.sendMessage("§cVocê não tem permissão para isso.");
            return;
        }

        String[] remaining = Arrays.copyOfRange(args, consumed, args.length);
        CommandContext context = buildContext(sender, label, node, remaining);

        CommandNode finalNode = node;
        BukkitCommand.getInstance().getServer().getScheduler().runTask(BukkitCommand.getInstance(), () -> invoke(finalNode, context));
    }

    private List<String> suggest(CommandNode root, String[] args) {
        CommandNode node = root;
        for (int i = 0; i < args.length - 1; i++) {
            CommandNode next = node.find(args[i]);
            if (next == null) return List.of();
            node = next;
        }

        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        return node.getChildren().keySet().stream()
                .filter(name -> name.startsWith(prefix))
                .sorted()
                .toList();
    }

    private CommandContext buildContext(CommandSender sender, String label, CommandNode node, String[] remaining) {
        List<String> arguments = List.of(remaining);
        return switch (sender) {
            case Player player ->
                    new CommandContext.PlayerContext(player, label, remaining, arguments);
            case CommandSender console ->
                    new CommandContext.Console(console, label, remaining, arguments);
        };
    }

    private void invoke(CommandNode node, CommandContext context) {
        try {
            node.getHandler().invoke(context);
        } catch (Throwable throwable) {
            context.info("§cOcorreu um erro ao executar esse comando.");
            throwable.printStackTrace();
        }
    }

    @SneakyThrows
    private CommandMap resolveCommandMap() {
        Object server = BukkitCommand.getInstance().getServer();
        try {
            Method direct = server.getClass().getMethod("getCommandMap");
            return (CommandMap) direct.invoke(server);
        } catch (NoSuchMethodException ignored) {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (SimpleCommandMap) field.get(server);
        }
    }

    private MethodHandle bind(Object instance, Method method) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    method.getDeclaringClass(), MethodHandles.lookup());
            return lookup.unreflect(method)
                    .bindTo(instance)
                    .asType(MethodType.methodType(void.class, CommandContext.class));
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private Object instantiate(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
