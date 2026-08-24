package net.syntaxfree.no_mob_leaders;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.syntaxfree.no_mob_leaders.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NoMobLeaders implements ModInitializer {
    @SuppressWarnings("unused")
    public static final String MOD_ID = "no_mob_leaders";

    private static final Identifier VANILLA_LEADER_BONUS = Identifier.tryParse("minecraft:leader_zombie_bonus");
    private static final Identifier VANILLA_REINFORCEMENT_CALLER = Identifier.tryParse("minecraft:reinforcement_caller_charge");
    private static final Identifier VANILLA_REINFORCEMENT_CALLEE = Identifier.tryParse("minecraft:reinforcement_callee_charge");

    @Override
    @SuppressWarnings("unused")
    public void onInitialize() {
        ModConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> ModConfig.rebuildCache());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> ModConfig.rebuildCache());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("nomobleaders")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    ModConfig.load();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[No Mob Leaders] Config reloaded successfully!")
                                                    .withStyle(ChatFormatting.GREEN),
                                            true
                                    );
                                    return 1;
                                }))
        ));
    }

    @SuppressWarnings("resource")
    public static void handleZombieSpawn(Zombie zombie) {
        if (zombie == null || zombie.level().isClientSide()) {
            return;
        }

        if (!ModConfig.isMobDisabled(zombie.getType())) {
            return;
        }

        ModConfig config = ModConfig.INSTANCE;
        boolean removedHealthBonus = false;

        if (config.removeHealthBonus) {
            AttributeInstance maxHealthAttr = zombie.getAttribute(Attributes.MAX_HEALTH);
            if (removeLeaderModifiers(maxHealthAttr, config)) {
                removedHealthBonus = true;
            }
        }

        if (config.removeReinforcementBonus) {
            AttributeInstance reinforcementAttr = zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
            removeLeaderModifiers(reinforcementAttr, config);
        }

        if (config.removeDoorBreakingBonus && zombie.canBreakDoors()) {
            zombie.setCanBreakDoors(false);
        }

        if (removedHealthBonus) {
            float currentMaxHealth = zombie.getMaxHealth();
            if (zombie.getHealth() > currentMaxHealth) {
                zombie.setHealth(currentMaxHealth);
            }
        }
    }

    private static boolean removeLeaderModifiers(AttributeInstance attribute, ModConfig config) {
        if (attribute == null) {
            return false;
        }

        Set<AttributeModifier> modifiers = attribute.getModifiers();
        if (modifiers.isEmpty()) {
            return false;
        }

        List<Identifier> toRemove = null;
        for (AttributeModifier modifier : modifiers) {
            Identifier id = modifier.id();
            if (isTargetLeaderModifier(id, config)) {
                if (toRemove == null) {
                    toRemove = new ArrayList<>(2);
                }
                toRemove.add(id);
            }
        }

        if (toRemove != null) {
            for (Identifier id : toRemove) {
                attribute.removeModifier(id);
            }
            return true;
        }

        return false;
    }

    private static boolean isTargetLeaderModifier(Identifier id, ModConfig config) {
        if (id == null) {
            return false;
        }

        if (ModConfig.isCustomModifierTargeted(id)) {
            return true;
        }

        if (id.equals(VANILLA_LEADER_BONUS) || id.equals(VANILLA_REINFORCEMENT_CALLER) || id.equals(VANILLA_REINFORCEMENT_CALLEE)) {
            return true;
        }

        if (config.onlyRemoveVanillaModifiers) {
            if ("minecraft".equals(id.getNamespace())) {
                String path = id.getPath();
                return path.contains("leader") || path.contains("reinforcement");
            }
            return false;
        } else {
            String path = id.getPath();
            return path.contains("leader") || path.contains("reinforcement");
        }
    }
}