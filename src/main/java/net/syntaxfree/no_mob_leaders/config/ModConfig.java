package net.syntaxfree.no_mob_leaders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("no_mob_leaders.json");

    public static volatile ModConfig INSTANCE = new ModConfig();
    private static volatile ConfigCache CACHE = ConfigCache.EMPTY;

    public List<String> disabledMobTypes = new ArrayList<>(List.of(
            "minecraft:zombie",
            "minecraft:zombified_piglin",
            "minecraft:husk",
            "minecraft:drowned",
            "minecraft:zombie_villager"
    ));

    public boolean removeHealthBonus = true;
    public boolean removeReinforcementBonus = true;
    public boolean removeDoorBreakingBonus = false;
    public boolean onlyRemoveVanillaModifiers = true;

    public List<String> customModifierIds = new ArrayList<>();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                INSTANCE = (loaded != null) ? loaded : new ModConfig();
            } catch (Exception e) {
                System.err.println("[NoMobLeaders] Failed to parse config file, falling back to defaults: " + e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
        rebuildCache();
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[NoMobLeaders] Failed to save config file: " + e.getMessage());
        }
    }

    public static void rebuildCache() {
        ModConfig config = INSTANCE;
        Set<EntityType<?>> types = Collections.newSetFromMap(new IdentityHashMap<>());
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        Set<Identifier> customModifiers = new HashSet<>();

        if (config.disabledMobTypes != null) {
            for (String rawEntry : config.disabledMobTypes) {
                if (rawEntry == null || rawEntry.isBlank()) {
                    continue;
                }

                String entry = rawEntry.trim().toLowerCase();
                if (entry.startsWith("#")) {
                    String tagString = entry.substring(1).trim();
                    if (!tagString.contains(":")) {
                        tagString = "minecraft:" + tagString;
                    }
                    Identifier tagId = Identifier.tryParse(tagString);
                    if (tagId != null) {
                        tags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                    }
                } else {
                    if (!entry.contains(":")) {
                        entry = "minecraft:" + entry;
                    }
                    Identifier id = Identifier.tryParse(entry);
                    if (id != null) {
                        BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(types::add);
                    }
                }
            }
        }

        if (config.customModifierIds != null) {
            for (String modIdRaw : config.customModifierIds) {
                if (modIdRaw != null && !modIdRaw.isBlank()) {
                    Identifier id = Identifier.tryParse(modIdRaw.trim());
                    if (id != null) {
                        customModifiers.add(id);
                    }
                }
            }
        }

        CACHE = new ConfigCache(types, List.copyOf(tags), Set.copyOf(customModifiers));
    }

    public static boolean isMobDisabled(EntityType<?> type) {
        if (type == null) {
            return false;
        }

        ConfigCache cache = CACHE;
        if (cache.types.contains(type)) {
            return true;
        }

        for (TagKey<EntityType<?>> tag : cache.tags) {
            if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCustomModifierTargeted(Identifier modifierId) {
        return modifierId != null && CACHE.customModifierIds.contains(modifierId);
    }

    private record ConfigCache(
            Set<EntityType<?>> types,
            List<TagKey<EntityType<?>>> tags,
            Set<Identifier> customModifierIds
    ) {
        public static final ConfigCache EMPTY = new ConfigCache(Set.of(), List.of(), Set.of());
    }
}