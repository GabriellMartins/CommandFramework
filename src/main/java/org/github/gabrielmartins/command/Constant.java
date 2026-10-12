package org.github.gabrielmartins.command;

import java.time.Duration;
import java.util.regex.Pattern;

public final class Constant {

    public static final String REPO = "GabriellMartins/CommandFramework";
    public static final String API_URL = "[api.github.com](https://api.github.com/repos/%s/releases/latest)";
    public static final String RELEASES_URL = "[github.com](https://github.com/%s/releases)";
    public static final Duration CACHE_TTL = Duration.ofMinutes(10);
    public static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
}
