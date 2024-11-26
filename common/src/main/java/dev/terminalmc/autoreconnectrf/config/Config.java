/*
 * AutoReconnect
 * Copyright (C) 2024 TerminalMC
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Config {
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = AutoReconnect.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static class Options {
        public static final List<Integer> defaultDelays = List.of(3, 10, 30, 60);
        public List<Integer> delays = defaultDelays;

        public static final boolean defaultInfinite = false;
        public boolean infinite = defaultInfinite;

        public static final boolean defaultConditionType = false;
        public boolean conditionType = defaultConditionType;

        public static final List<String> defaultConditionKeys = new ArrayList<>(
                List.of("multiplayer.disconnect.banned"));
        public List<String> conditionKeys = defaultConditionKeys;

        public static final List<String> defaultConditionPatterns = new ArrayList<>();
        public List<String> conditionPatterns = defaultConditionPatterns;

        public static final List<AutoMessage> defaultAutoMessages = new ArrayList<>();
        public List<AutoMessage> autoMessages = defaultAutoMessages;
    }

    public static final class AutoMessage {
        public static final String defaultName = "";
        public String name = defaultName;

        public static final List<String> defaultMessages = new ArrayList<>();
        public List<String> messages = defaultMessages;

        public static final int defaultDelay = 1000;
        public int delay = defaultDelay;

        public Iterator<String> getMessages() {
            return messages.iterator();
        }

        public int getDelay() {
            return delay;
        }
    }

    // Utils

    public int getDelayForAttempt(int attempt) {
        if (attempt < options.delays.size()) return options.delays.get(attempt);
        if (options.infinite) return options.delays.getLast(); // repeat last
        return -1; // no more attempts configured
    }

    public boolean hasAttempts() {
        return !options.delays.isEmpty();
    }

    public Optional<AutoMessage> getAutoMessagesForName(String name) {
        return options.autoMessages.stream().filter(autoMessage -> name.equals(autoMessage.name)).findFirst();
    }

    // Validation

    private void validate() {
        if (options.delays == null) options.delays = Options.defaultDelays;
        else if (!options.delays.isEmpty()) options.delays = options.delays.stream().filter(i -> i > 0).toList();
        if (options.autoMessages == null) options.autoMessages = Options.defaultAutoMessages;
        else if (!options.autoMessages.isEmpty()) for (AutoMessage autoMessage : options.autoMessages) {
            if (autoMessage.name == null) autoMessage.name = AutoMessage.defaultName;
            if (autoMessage.messages == null) autoMessage.messages = AutoMessage.defaultMessages;
            else if (!autoMessage.messages.isEmpty())
                autoMessage.messages = autoMessage.messages.stream().filter(Objects::nonNull).toList();
            if (autoMessage.delay <= 0) autoMessage.delay = AutoMessage.defaultDelay;
        }
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
        }
        if (config == null) {
            config = new Config();
        }
        config.validate();
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            AutoReconnect.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        if (instance == null) return;
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            AutoReconnect.onConfigSaved(instance);
        } catch (IOException e) {
            AutoReconnect.LOG.error("Unable to save config.", e);
        }
    }
}
