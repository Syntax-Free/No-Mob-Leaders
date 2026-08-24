package net.syntaxfree.no_mob_leaders.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("no_mob_leaders.json");

    public static volatile ModConfig INSTANCE = new ModConfig();

    public List<String> disabledMobTypes = new ArrayList<>(List.of(
            "minecraft:zombie",
            "minecraft:zombified_piglin",
            "minecraft:husk",
            "minecraft:drowned",
            "minecraft:zombie_villager"
    ));

    public boolean removeHealthBonus = true;
    public boolean removeReinforcementBonus = true;

    // Fast cached set using IdentityHashMap (reference equality for registered EntityType singletons)
    private final transient Set<EntityType<?>> disabledEntityTypes = Collections.newSetFromMap(new IdentityHashMap<>());

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                INSTANCE = loaded != null ? loaded : new ModConfig();
            } catch (Exception e) {
                System.err.println("[NoMobLeaders] Failed to load config file: " + e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
        INSTANCE.rebuildCache();
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[NoMobLeaders] Failed to save config file: " + e.getMessage());
        }
    }

    public void rebuildCache() {
        disabledEntityTypes.clear();
        if (disabledMobTypes == null) {
            return;
        }

        for (String rawEntry : disabledMobTypes) {
            if (rawEntry == null || rawEntry.isBlank()) {
                continue;
            }

            String entry = rawEntry.trim().toLowerCase();
            if (!entry.contains(":")) {
                entry = "minecraft:" + entry;
            }

            Identifier id = Identifier.tryParse(entry);
            if (id != null) {
                BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(disabledEntityTypes::add);
            }
        }
    }

    public boolean isMobDisabled(EntityType<?> type) {
        return type != null && disabledEntityTypes.contains(type);
    }
}