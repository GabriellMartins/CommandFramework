package org.github.gabrielmartins.command.core.register;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.github.gabrielmartins.command.BukkitCommand;
import org.github.gabrielmartins.command.core.scanner.ClassScanner;
import org.github.gabrielmartins.command.core.context.CommandContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class CommandRegister {
    private final ClassScanner scanner = new ClassScanner();
    public void create(String packageName) {
        for (Class<?> type : scanner.scan(BukkitCommand.getInstance(), packageName)) {
            List<Method> handlers = Arrays.stream(type.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(org.github.gabrielmartins.command.annotation.Command.class))
                    .toList();
            if (handlers.isEmpty()) continue;

            Object instance = instantiate(type);
            if (instance == null) continue;

            for (Method method : handlers) {
                var meta = method.getAnnotation(org.github.gabrielmartins.command.annotation.Command.class);
                MethodHandle handle = bind(instance, method, type.getSimpleName());
                if (handle == null) continue;
                register(meta, handle);
            }
        }
    }

    @SneakyThrows
    private void register(org.github.gabrielmartins.command.annotation.Command meta, MethodHandle handle) {
        CommandMap commandMap = resolveCommandMap();

        Command command = new Command(meta.name(), meta.description(), meta.usage(),
                List.of(meta.aliases())) {

            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (meta.playerOnly() && !(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§cApenas jogadores podem usar este comando.");
                    return true;
                }
                if (!meta.permission().isBlank() && !sender.hasPermission(meta.permission())) {
                    sender.sendMessage("§cVocê não tem permissão para isso.");
                    return true;
                }

                CommandContext context = buildContext(sender, label, args);
                Runnable task = () -> invoke(handle, context);
                if (meta.async()) {
                    Bukkit.getScheduler().runTaskAsynchronously(BukkitCommand.getInstance(), task);
                } else {
                    task.run();
                }
                return true;
            }
        };

        if (!meta.permission().isBlank()) command.setPermission(meta.permission());

        commandMap.register(BukkitCommand.getInstance().getName().toLowerCase(), command);
    }

    @SneakyThrows
    private CommandMap resolveCommandMap() {
        Object server = Bukkit.getServer();
        try {
            Method direct = server.getClass().getMethod("getCommandMap");
            return (CommandMap) direct.invoke(server);
        } catch (NoSuchMethodException ignored) {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (SimpleCommandMap) field.get(server);
        }
    }

    private CommandContext buildContext(CommandSender sender, String label, String[] raw) {
        List<String> args = List.of(raw);
        return switch (sender) {
            case org.bukkit.entity.Player player ->
                    new CommandContext.PlayerContext(player, label, raw, args);
            case CommandSender s -> new CommandContext.Console(s, label, raw, args);
        };
    }

    private void invoke(MethodHandle handle, CommandContext context) {
        try {
            handle.invoke(context);
        } catch (Throwable throwable) {
            BukkitCommand.getInstance().getLogger().log(Level.SEVERE, "Erro ao executar comando", throwable);
            context.getSender().sendMessage("§cOcorreu um erro ao executar esse comando.");
        }
    }

    private MethodHandle bind(Object instance, Method method, String owner) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    method.getDeclaringClass(), MethodHandles.lookup());
            return lookup.unreflect(method)
                    .bindTo(instance)
                    .asType(MethodType.methodType(void.class, CommandContext.class));
        } catch (IllegalAccessException e) {
            BukkitCommand.getInstance().getLogger().warning("Método '%s' de %s precisa ser acessível: %s"
                    .formatted(method.getName(), owner, e.getMessage()));
            return null;
        }
    }

    private Object instantiate(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            BukkitCommand.getInstance().getLogger().warning("Não foi possível instanciar %s (precisa de construtor vazio)"
                    .formatted(type.getSimpleName()));
            return null;
        }
    }
}
