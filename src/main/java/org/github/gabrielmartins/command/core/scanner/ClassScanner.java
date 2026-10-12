package org.github.gabrielmartins.command.core.scanner;

import lombok.SneakyThrows;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ClassScanner {

    @SneakyThrows
    public List<Class<?>> scan(JavaPlugin plugin, String packageName) {
        List<Class<?>> found = new ArrayList<>();
        String path = packageName.replace('.', '/');
        URL resource = plugin.getClass().getClassLoader().getResource(path);
        if (resource == null) return found;

        File root = new File(resource.toURI());
        if (root.isFile()) {
            try (JarFile jar = new JarFile(root)) {
                jar.stream()
                        .map(JarEntry::getName)
                        .filter(n -> n.startsWith(path) && n.endsWith(".class"))
                        .map(n -> n.replace('/', '.').replace(".class", ""))
                        .forEach(n -> collect(n, found));
            }
        } else {
            walk(root, packageName, found);
        }
        return found;
    }

    @SneakyThrows
    private void walk(File dir, String packageName, List<Class<?>> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                walk(file, packageName + "." + file.getName(), out);
            } else if (file.getName().endsWith(".class")) {
                collect(packageName + "." + file.getName().replace(".class", ""), out);
            }
        }
    }

    @SneakyThrows
    private void collect(String className, List<Class<?>> out) {
        out.add(Class.forName(className));
    }
}
