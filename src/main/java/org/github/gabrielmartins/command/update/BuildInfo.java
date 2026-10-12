package org.github.gabrielmartins.command.update;

import lombok.Getter;

@Getter
public final class BuildInfo {

    private final String version;
    private final String origin;

    private BuildInfo(String version, String origin) {
        this.version = version;
        this.origin = origin;
    }

    public static BuildInfo load() {
        try (var stream = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (stream == null) return new BuildInfo("dev", "dev");
            var props = new java.util.Properties();
            props.load(stream);
            return new BuildInfo(
                    props.getProperty("version", "desconhecida"),
                    props.getProperty("origin", "dev"));
        } catch (Exception e) {
            return new BuildInfo("desconhecida", "dev");
        }
    }

    public boolean isRelease() {
        return "release".equalsIgnoreCase(origin);
    }

    public boolean looksLikeGitCheckout() {
        try {
            var location = new java.io.File(getClass().getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            var dir = location.isDirectory() ? location : location.getParentFile();
            return dir != null && new java.io.File(dir, ".git").exists();
        } catch (Exception e) {
            return false;
        }
    }
}
