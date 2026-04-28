package me.kall.duplicationless.config;

import com.google.gson.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonConfig {
    private final Path configPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final LinkedHashMap<String, JsonElement> configMap = new LinkedHashMap<>();
    private static final JsonParser PARSER = new JsonParser();

    public static final Path CONFIG_DIR;

    static {
        try {
            CONFIG_DIR = Path.of(JsonConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()).normalize().toAbsolutePath().getParent().getParent().resolve("config");
        } catch (Exception exception) {
            System.err.println("Exception finding config directory");
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    private JsonConfig(@NotNull Path configPath, String version) {
        this.configPath = configPath;
        this.put("Version", version);
    }

    @Contract("_, _ -> new")
    public static @NotNull JsonConfig create(Path configPath, String version) {
        return new JsonConfig(configPath, version);
    }

    @Contract("_, _ -> new")
    public static @NotNull JsonConfig create(String modID, String version) {
        return create(CONFIG_DIR.resolve(modID + ".json"), version);
    }

    public JsonConfig initialize() {
        if (Files.exists(this.configPath)) {
            this.read();
        } else {
            this.create();
        }
        return this;
    }

    private void read() {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.configPath.toFile()))) {
            JsonObject fileConfig = PARSER.parse(reader).getAsJsonObject();

            Map<String, JsonElement> defaultConfig = new LinkedHashMap<>(this.configMap);

            this.configMap.clear();
            for (Map.Entry<String, JsonElement> entry : fileConfig.entrySet()) {
                this.configMap.put(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, JsonElement> entry : defaultConfig.entrySet()) {
                if (!this.configMap.containsKey(entry.getKey())) {
                    this.configMap.put(entry.getKey(), entry.getValue());
                }
            }

            JsonElement fileVersion = fileConfig.get("Version");
            if (fileVersion == null || !fileVersion.getAsString().equals(defaultConfig.get("Version").getAsString())) {
                this.configMap.put("Version", defaultConfig.get("Version"));
                this.saveToFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configPath, e);
        }
    }

    private void create() {
        try {
            Files.createDirectories(this.configPath.getParent());
            this.saveToFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file: " + configPath, e);
        }
    }

    public void saveToFile() {
        try (Writer writer = new FileWriter(this.configPath.toFile())) {
            this.gson.toJson(this.configMap, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file: " + this.configPath, e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public JsonConfig put(String key, Object value) {
        this.configMap.put(key, this.gson.toJsonTree(value));
        return this;
    }

    private JsonElement get(String key) {
        return this.configMap.get(key);
    }

    public int getInt(String key) {
        return this.get(key).getAsInt();
    }

    public double getDouble(String key) {
        return this.get(key).getAsDouble();
    }

    public float getFloat(String key) {
        return this.get(key).getAsFloat();
    }

    public long getLong(String key) {
        return this.get(key).getAsLong();
    }

    public boolean getBoolean(String key) {
        return this.get(key).getAsBoolean();
    }

    public String getString(String key) {
        return this.get(key).getAsString();
    }

    public <T> Stream<T> getStream(String key, @NotNull Class<T> valueType) {
        return StreamSupport.stream(this.configMap.get(key).getAsJsonArray().spliterator(), false).map(element -> this.gson.fromJson(element, valueType));
    }

    public <T> List<T> getList(String key, @NotNull Class<T> valueType) {
        return this.getStream(key, valueType).collect(Collectors.toList());
    }

    public <T> Set<T> getSet(String key, @NotNull Class<T> valueType) {
        return this.getStream(key, valueType).collect(Collectors.toSet());
    }
}