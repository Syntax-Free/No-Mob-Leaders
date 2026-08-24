package net.syntaxfree.no_mob_leaders;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

    @Override
    @SuppressWarnings("unused")
    public void onInitialize() {
        ModConfig.load();

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

        ModConfig config = ModConfig.INSTANCE;
        if (!config.isMobDisabled(zombie.getType())) {
            return;
        }

        boolean removedHealthBonus = false;

        if (config.removeHealthBonus) {
            AttributeInstance maxHealthAttr = zombie.getAttribute(Attributes.MAX_HEALTH);
            if (removeLeaderModifiers(maxHealthAttr)) {
                removedHealthBonus = true;
            }
        }

        if (config.removeReinforcementBonus) {
            AttributeInstance reinforcementAttr = zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
            removeLeaderModifiers(reinforcementAttr);
        }

        if (removedHealthBonus) {
            float currentMaxHealth = zombie.getMaxHealth();
            if (zombie.getHealth() > currentMaxHealth) {
                zombie.setHealth(currentMaxHealth);
            }
        }
    }

    private static boolean removeLeaderModifiers(AttributeInstance attribute) {
        if (attribute == null) {
            return false;
        }

        Set<AttributeModifier> modifiers = attribute.getModifiers();
        if (modifiers.isEmpty()) {
            return false;
        }

        List<Identifier> toRemove = findLeaderModifierIds(modifiers);
        if (toRemove != null) {
            for (Identifier id : toRemove) {
                attribute.removeModifier(id);
            }
            return true;
        }

        return false;
    }

    private static List<Identifier> findLeaderModifierIds(Set<AttributeModifier> modifiers) {
        List<Identifier> toRemove = null;
        for (AttributeModifier modifier : modifiers) {
            String path = modifier.id().getPath();
            if (path.contains("leader") || path.contains("bonus") || path.contains("reinforcement")) {
                if (toRemove == null) {
                    toRemove = new ArrayList<>(2);
                }
                toRemove.add(modifier.id());
            }
        }
        return toRemove;
    }
}