package org.me.pyke.pyRoulette.logging;

import org.bukkit.Bukkit;
import org.me.pyke.pyRoulette.PyRoulette;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class AuditLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final PyRoulette plugin;
    private final Object lock = new Object();
    private Path logFile;
    private BufferedWriter writer;
    private boolean enabled;

    public AuditLogger(PyRoulette plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        synchronized (lock) {
            closeWriter();
            enabled = plugin.getConfig().getBoolean("audit-log.enabled", true);
            String fileName = plugin.getConfig().getString("audit-log.file", "roulette-audit.log");
            logFile = plugin.getDataFolder().toPath().resolve(fileName).normalize();
        }
    }

    public void log(String action, Map<String, String> values) {
        if (!enabled) {
            return;
        }
        StringBuilder line = new StringBuilder();
        line.append('[').append(LocalDateTime.now().format(FORMATTER)).append("] ");
        line.append(action);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            line.append(" | ").append(entry.getKey()).append('=').append(entry.getValue());
        }
        line.append(System.lineSeparator());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(line.toString()));
    }

    private void write(String line) {
        synchronized (lock) {
            if (!enabled) {
                return;
            }
            try {
                ensureWriter();
                writer.write(line);
                writer.flush();
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not write audit log: " + exception.getMessage());
            }
        }
    }

    public void close() {
        synchronized (lock) {
            enabled = false;
            closeWriter();
        }
    }

    private void ensureWriter() throws IOException {
        if (writer != null) {
            return;
        }
        Files.createDirectories(logFile.getParent());
        writer = Files.newBufferedWriter(
                logFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private void closeWriter() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not close audit log: " + exception.getMessage());
        } finally {
            writer = null;
        }
    }
}
