/*
 * AutoReconnect
 * Copyright (C) 2026 TerminalMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.terminalmc.autoreconnectrf.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.platform.services.PlatformServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Config {

    private static final Path DIR_PATH = PlatformServices.getInstance().getConfigDir();
    private static final String FILE_NAME = AutoReconnect.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = AutoReconnect.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {
        // Deserializer and self-instantiation only.
    }

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {

        public static final Supplier<List<Float>> delaysDefault = () -> new ArrayList<>(List.of(
                3F,
                10F,
                30F,
                60F
        ));
        public List<Float> delays = delaysDefault.get();

        public static final boolean initialDefault = false;
        public boolean initial = initialDefault;

        public static final boolean infiniteDefault = false;
        public boolean infinite = infiniteDefault;

        public static final boolean conditionTypeDefault = false;
        public boolean conditionType = conditionTypeDefault;

        public static final Supplier<List<String>> conditionKeysDefault =
                () -> new ArrayList<>(List.of(
                        "disconnect.loginFailedInfo",
                        "disconnect.spam",
                        "disconnect.timeout",
                        "disconnect.unknownHost",
                        "multiplayer.disconnect.banned",
                        "multiplayer.disconnect.code_of_conduct",
                        "multiplayer.disconnect.incompatible",
                        "multiplayer.disconnect.ip_banned",
                        "multiplayer.disconnect.kicked",
                        "multiplayer.disconnect.name_taken",
                        "multiplayer.disconnect.not_whitelisted",
                        "multiplayer.disconnect.outdated_client",
                        "multiplayer.disconnect.outdated_server"
                ));
        public List<String> conditionKeys = conditionKeysDefault.get();

        public static final Supplier<List<String>> conditionPatternsDefault =
                () -> new ArrayList<>(List.of());
        public List<String> conditionPatterns = conditionPatternsDefault.get();

        public static final Supplier<List<AutoMessage>> autoMessagesDefault =
                () -> new ArrayList<>(List.of());
        public List<AutoMessage> autoMessages = autoMessagesDefault.get();

        public static final boolean commandSigningDefault = false;
        public boolean commandSigning = commandSigningDefault;

        public static final boolean regexIdsDefault = false;
        public boolean regexIds = regexIdsDefault;
    }

    public static final class AutoMessage {

        public static final String idDefault = "";
        public String id = idDefault;

        public static final float delayDefault = 1F;
        public float delay = delayDefault;

        public static final Supplier<List<String>> messagesDefault = List::of;
        public List<String> messages = messagesDefault.get();

        public Iterator<String> getMessages() {
            return messages.iterator();
        }
    }

    // Utils

    public float getDelayForAttempt(int attempt) {
        if (attempt < options.delays.size())
            return options.delays.get(attempt);
        if (options.infinite)
            return options.delays.getLast(); // repeat last
        return -1F; // no more attempts configured
    }

    public boolean hasAttempts() {
        return !options.delays.isEmpty();
    }

    public List<AutoMessage> getAutoMessagesForId(String id) {
        return options.autoMessages.stream()
                .filter(autoMessage -> {
                    if (options.regexIds) {
                        // Can't depend on the config patterns being valid
                        try {
                            return Pattern.compile(autoMessage.id).matcher(id).matches();
                        } catch (PatternSyntaxException ignored) {
                            return false;
                        }
                    } else {
                        return autoMessage.id.equals(id);
                    }
                })
                .toList();
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    @SuppressWarnings("unused")
    public static Config reloadAndSave() {
        instance = Config.load();
        save();
        return instance;
    }

    @SuppressWarnings("unused")
    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Validation

    /**
     * Cleanup and validation method, called after config is loaded and before it is saved.
     */
    private void validate() {
        if (options.delays == null) {
            options.delays = Options.delaysDefault.get();
        } else if (!options.delays.isEmpty()) {
            options.delays = options.delays.stream().filter(i -> i > 0F).toList();
        }
        if (options.autoMessages == null) {
            options.autoMessages = Options.autoMessagesDefault.get();
        } else if (!options.autoMessages.isEmpty()) {
            for (AutoMessage autoMessage : options.autoMessages) {
                if (autoMessage.id == null) {
                    autoMessage.id = AutoMessage.idDefault;
                }
                if (autoMessage.messages == null) {
                    autoMessage.messages = AutoMessage.messagesDefault.get();
                } else if (!autoMessage.messages.isEmpty()) {
                    autoMessage.messages =
                            autoMessage.messages.stream().filter(Objects::nonNull).toList();
                }
                if (autoMessage.delay <= 0) {
                    autoMessage.delay = AutoMessage.delayDefault;
                }
            }
        }
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        @Nullable Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) {
                backup();
                AutoReconnect.LOG.warn("Resetting config");
            }
        }
        if (config == null)
            config = new Config();
        config.validate();
        return config;
    }

    @SuppressWarnings("SameParameterValue")
    private static @Nullable Config load(Path file, Gson gson) {
        try (
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file.toFile()),
                        StandardCharsets.UTF_8
                )
        ) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            AutoReconnect.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup() {
        try {
            AutoReconnect.LOG.warn("Copying {} to {}", FILE_NAME, BACKUP_FILE_NAME);
            if (!Files.isDirectory(DIR_PATH))
                Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(
                    file,
                    backupFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            AutoReconnect.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null)
            return;
        instance.validate();
        try {
            if (!Files.isDirectory(DIR_PATH))
                Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(
                            new FileOutputStream(tempFile.toFile()),
                            StandardCharsets.UTF_8
                    )
            ) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(
                    tempFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
            AutoReconnect.onConfigSaved(instance);
        } catch (IOException e) {
            AutoReconnect.LOG.error("Unable to save config", e);
        }
    }
}
