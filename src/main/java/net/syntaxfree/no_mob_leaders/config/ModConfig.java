package net.syntaxfree.no_mob_leaders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("NoMobLeaders");
    private static final Object LOCK = new Object();

    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final List<String> DEFAULT_MOB_TYPES = List.of(
            "minecraft:zombie",
            "minecraft:zombified_piglin",
            "minecraft:husk",
            "minecraft:drowned",
            "minecraft:zombie_villager"
    );

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .setStrictness(Strictness.LENIENT)
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("no_mob_leaders.json");

    public static volatile ModConfig INSTANCE = new ModConfig();
    private static volatile ConfigCache CACHE = ConfigCache.EMPTY;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public List<String> disabledMobTypes = new ArrayList<>(DEFAULT_MOB_TYPES);
    public boolean removeHealthBonus = true;
    public boolean removeReinforcementBonus = true;

    public static void load() {
        synchronized (LOCK) {
            if (Files.exists(CONFIG_PATH)) {
                boolean needsResave;

                try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8);
                     JsonReader jsonReader = new JsonReader(reader)) {
                    jsonReader.setStrictness(Strictness.LENIENT);

                    ModConfig loaded = GSON.fromJson(jsonReader, ModConfig.class);
                    if (loaded == null) {
                        LOGGER.warn("[NoMobLeaders] Config file was empty. Restoring default settings.");
                        INSTANCE = new ModConfig();
                        needsResave = true;
                    } else {
                        INSTANCE = loaded;
                        needsResave = INSTANCE.validateAndSanitize();
                    }
                } catch (Exception e) {
                    LOGGER.error("[NoMobLeaders] Failed to parse config file: {}. Restoring defaults and backing up corrupted file.", e.getMessage());
                    backupCorruptedConfig();
                    INSTANCE = new ModConfig();
                    needsResave = true;
                }

                if (needsResave) {
                    save();
                }
            } else {
                INSTANCE = new ModConfig();
                INSTANCE.validateAndSanitize();
                save();
            }

            rebuildCache();
        }
    }

    public static void save() {
        synchronized (LOCK) {
            try {
                Path parent = CONFIG_PATH.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                Path tempPath = CONFIG_PATH.resolveSibling("no_mob_leaders.json.tmp");
                try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                    GSON.toJson(INSTANCE, writer);
                    writer.flush();
                }

                try {
                    Files.move(tempPath, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
                    Files.move(tempPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                LOGGER.error("[NoMobLeaders] Failed to save configuration file: {}", e.getMessage(), e);
            }
        }
    }

    public boolean validateAndSanitize() {
        boolean modified = false;

        if (this.configVersion != CURRENT_CONFIG_VERSION) {
            this.configVersion = CURRENT_CONFIG_VERSION;
            modified = true;
        }

        if (this.disabledMobTypes == null) {
            this.disabledMobTypes = new ArrayList<>(DEFAULT_MOB_TYPES);
            modified = true;
        } else {
            Set<String> sanitizedSet = new LinkedHashSet<>();
            for (String entry : this.disabledMobTypes) {
                if (entry != null && !entry.isBlank()) {
                    sanitizedSet.add(entry.trim().toLowerCase(Locale.ROOT));
                }
            }

            if (sanitizedSet.size() != this.disabledMobTypes.size()) {
                this.disabledMobTypes = new ArrayList<>(sanitizedSet);
                modified = true;
            }
        }

        return modified;
    }

    private static void backupCorruptedConfig() {
        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            Path backupPath = CONFIG_PATH.resolveSibling("no_mob_leaders.json.corrupted_" + timestamp);
            Files.copy(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("[NoMobLeaders] Corrupted configuration saved to: {}", backupPath.getFileName());
        } catch (IOException e) {
            LOGGER.error("[NoMobLeaders] Could not create backup of corrupted configuration file: {}", e.getMessage());
        }
    }

    public static void rebuildCache() {
        synchronized (LOCK) {
            ModConfig config = INSTANCE;
            Set<EntityType<?>> types = Collections.newSetFromMap(new IdentityHashMap<>());
            List<TagKey<EntityType<?>>> tags = new ArrayList<>();

            if (config.disabledMobTypes != null) {
                for (String rawEntry : config.disabledMobTypes) {
                    if (rawEntry == null || rawEntry.isBlank()) {
                        continue;
                    }

                    String entry = rawEntry.trim().toLowerCase(Locale.ROOT);
                    if (entry.startsWith("#")) {
                        String tagString = entry.substring(1).trim();
                        if (!tagString.contains(":")) {
                            tagString = "minecraft:" + tagString;
                        }
                        Identifier tagId = Identifier.tryParse(tagString);
                        if (tagId != null) {
                            tags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                        } else {
                            LOGGER.warn("[NoMobLeaders] Invalid tag syntax in configuration: '{}'", entry);
                        }
                    } else {
                        if (!entry.contains(":")) {
                            entry = "minecraft:" + entry;
                        }
                        Identifier id = Identifier.tryParse(entry);
                        if (id != null) {
                            var entityOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
                            if (entityOpt.isPresent()) {
                                types.add(entityOpt.get());
                            } else {
                                LOGGER.warn("[NoMobLeaders] Entity type '{}' was not found in registry (check for typos or missing mod).", entry);
                            }
                        } else {
                            LOGGER.warn("[NoMobLeaders] Invalid entity identifier syntax in configuration: '{}'", entry);
                        }
                    }
                }
            }

            CACHE = new ConfigCache(types, List.copyOf(tags));
        }
    }

    public static boolean isMobDisabled(EntityType<?> type) {
        if (type == null) {
            return false;
        }

        ConfigCache cache = CACHE;
        if (cache.types.contains(type)) {
            return true;
        }

        if (!cache.tags.isEmpty()) {
            var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
            for (TagKey<EntityType<?>> tag : cache.tags) {
                if (holder.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    private record ConfigCache(
            Set<EntityType<?>> types,
            List<TagKey<EntityType<?>>> tags
    ) {
        public static final ConfigCache EMPTY = new ConfigCache(Set.of(), List.of());
    }
}