package org.github.gabrielmartins.command.update.checker;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.github.gabrielmartins.command.BukkitCommand;
import org.github.gabrielmartins.command.Constant;
import org.github.gabrielmartins.command.update.BuildInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public final class UpdateChecker {

    private final BuildInfo buildInfo;
    private final AtomicReference<CachedResult> cache = new AtomicReference<>();
    public void checkAutomatically() {
        if (!buildInfo.isRelease()) {
            log().info("Build de desenvolvimento detectado (origem: %s) — checagem automática desativada."
                    .formatted(buildInfo.getOrigin()));
            return;
        }
        fetch(null);
    }

    public void checkManually(CommandSender sender) {
        fetch(sender);
    }

    private void fetch(CommandSender sender) {
        CachedResult cached = cache.get();
        if (cached != null && !cached.isExpired()) {
            deliver(cached.latest(), sender);
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constant.API_URL.formatted(Constant.REPO)))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CommandFramework/" + buildInfo.getVersion())
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> handleResponse(response, sender))
                .exceptionally(error -> {
                    log().warning("Falha ao consultar o GitHub: " + error.getMessage());
                    reply(sender, "§cNão foi possível consultar o GitHub agora.");
                    return null;
                });
    }

    private void handleResponse(HttpResponse<String> response, CommandSender sender) {
        int status = response.statusCode();

        if (status == 404) {
            log().warning("Nenhuma release encontrada em '%s'.".formatted(Constant.REPO));
            reply(sender, "§cNenhuma release publicada no repositório.");
            return;
        }

        if (status == 403) {
            log().warning("GitHub recusou a consulta (rate limit).");
            reply(sender, "§cLimite de consultas do GitHub atingido. Tente mais tarde.");
            return;
        }

        if (status != 200) {
            log().warning("Resposta inesperada do GitHub (HTTP %d).".formatted(status));
            reply(sender, "§cFalha ao verificar atualizações (HTTP %d).".formatted(status));
            return;
        }

        parseTag(response.body()).ifPresentOrElse(
                latest -> {
                    cache.set(new CachedResult(latest, System.currentTimeMillis()));
                    deliver(latest, sender);
                },
                () -> {
                    log().warning("Não foi possível interpretar a resposta do GitHub.");
                    reply(sender, "§cResposta inesperada do GitHub.");
                });
    }

    private Optional<String> parseTag(String json) {
        Matcher matcher = Constant.TAG_PATTERN.matcher(json);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(matcher.group(1).replaceFirst("^[vV]", ""));
    }

    private void deliver(String latest, CommandSender sender) {
        String current = buildInfo.getVersion().replace("-SNAPSHOT", "");

        if (latest.equalsIgnoreCase(current)) {
            log().info("Você está na versão mais recente (%s).".formatted(current));
            reply(sender, "§aVocê está na versão mais recente: §f" + current);
            return;
        }

        if (isNewer(latest, current)) {
            log().warning("""
                    ============================================
                     Atualização disponível: %s -> %s
                     Baixe em: %s
                    ============================================
                    """.formatted(current, latest, Constant.RELEASES_URL.formatted(Constant.REPO)).strip());
            reply(sender, "§eNova versão disponível: §f%s §7-> §a%s".formatted(current, latest));
            reply(sender, "§7Baixe em: §f" + Constant.RELEASES_URL.formatted(Constant.REPO));
            return;
        }

        log().info("Versão local (%s) está à frente da última release (%s)."
                .formatted(current, latest));
        reply(sender, "§7Sua versão (%s) está à frente da última release (%s)."
                .formatted(current, latest));
    }

    static boolean isNewer(String candidate, String current) {
        String[] a = candidate.split("[.\\-+]");
        String[] b = current.split("[.\\-+]");
        int length = Math.max(a.length, b.length);

        for (int i = 0; i < length; i++) {
            String left = i < a.length ? a[i] : "0";
            String right = i < b.length ? b[i] : "0";

            boolean leftNumeric = left.matches("\\d+");
            boolean rightNumeric = right.matches("\\d+");

            int comparison = (leftNumeric && rightNumeric)
                    ? Integer.compare(Integer.parseInt(left), Integer.parseInt(right))
                    : left.compareToIgnoreCase(right);

            if (comparison != 0) return comparison > 0;
        }
        return false;
    }

    private void reply(CommandSender sender, String message) {
        if (sender == null) return;
        BukkitCommand.getInstance().getServer().getScheduler()
                .runTask(BukkitCommand.getInstance(), () -> sender.sendMessage(message));
    }

    private Logger log() {
        return BukkitCommand.getInstance().getLogger();
    }

    private record CachedResult(String latest, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > Constant.CACHE_TTL.toMillis();
        }
    }
}
