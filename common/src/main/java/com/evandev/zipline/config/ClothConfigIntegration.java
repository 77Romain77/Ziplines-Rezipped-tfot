package com.evandev.zipline.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();
        boolean isServer = config.isServerConfig;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(isServer ? Component.literal("Zipline Config (Server Enforced)") : Component.translatable("config.zipline.title"));

        if (!isServer) {
            builder.setSavingRunnable(ModConfig::save);
        }

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.zipline.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        if (isServer) {
            general.addEntry(entryBuilder.startTextDescription(
                    Component.literal("§cSettings are currently enforced by the server.")
            ).build());
            general.addEntry(entryBuilder.startTextDescription(
                    Component.literal("§cAny changes made here will not be saved.")
            ).build());
        }

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.snap_radius"), config.snapRadius)
                .setDefaultValue(2.0)
                .setTooltip(Component.translatable("config.zipline.option.snap_radius.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.snapRadius = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.click_reach"), config.clickReach)
                .setDefaultValue(3.0)
                .setTooltip(Component.translatable("config.zipline.option.click_reach.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.clickReach = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.use_anywhere"), config.useAnywhere)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.zipline.option.use_anywhere.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.useAnywhere = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.max_turn_angle"), config.maxTurnAngle)
                .setDefaultValue(0.707)
                .setTooltip(Component.translatable("config.zipline.option.max_turn_angle.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.maxTurnAngle = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.hang_offset"), config.hangOffset)
                .setDefaultValue(2.3)
                .setTooltip(Component.translatable("config.zipline.option.hang_offset.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.hangOffset = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.speed_multiplier"), config.speedMultiplier)
                .setDefaultValue(1.0)
                .setMin(0.0)
                .setTooltip(Component.translatable("config.zipline.option.speed_multiplier.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.speedMultiplier = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.realistic_physics"), config.realisticPhysics)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.zipline.option.realistic_physics.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.realisticPhysics = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.gravity_strength"), config.gravityStrength)
                .setDefaultValue(0.04)
                .setMin(0.0)
                .setMax(0.2)
                .setTooltip(Component.translatable("config.zipline.option.gravity_strength.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.gravityStrength = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.velocity_retention"), config.velocityRetention)
                .setDefaultValue(0.98)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.translatable("config.zipline.option.velocity_retention.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.velocityRetention = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.max_speed"), config.maxSpeed)
                .setDefaultValue(50.0)
                .setMin(0.0)
                .setTooltip(Component.translatable("config.zipline.option.max_speed.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.maxSpeed = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.downhill_only"), config.downhillOnly)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.zipline.option.downhill_only.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.downhillOnly = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.downhill_height_tolerance"), config.downhillHeightTolerance)
                .setDefaultValue(0.5)
                .setMin(0.0)
                .setTooltip(Component.translatable("config.zipline.option.downhill_height_tolerance.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.downhillHeightTolerance = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.auto_detach_at_end"), config.autoDetachAtEnd)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.zipline.option.auto_detach_at_end.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.autoDetachAtEnd = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.end_detection_distance"), config.endDetectionDistance)
                .setDefaultValue(2.0)
                .setMin(0.0)
                .setMax(10.0)
                .setTooltip(Component.translatable("config.zipline.option.end_detection_distance.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.endDetectionDistance = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.exit_jump_multiplier"), config.exitJumpMultiplier)
                .setDefaultValue(1.4)
                .setTooltip(Component.translatable("config.zipline.option.exit_jump_multiplier.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.exitJumpMultiplier = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.exit_jump_uses_look_direction"), config.exitJumpUsesLookDirection)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.zipline.option.exit_jump_uses_look_direction.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.exitJumpUsesLookDirection = newValue; })
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("config.zipline.option.exit_jump_look_boost"), config.exitJumpLookBoost)
                .setDefaultValue(0.5)
                .setMin(0.0)
                .setTooltip(Component.translatable("config.zipline.option.exit_jump_look_boost.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.exitJumpLookBoost = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.consume_durability"), config.consumeDurability)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.zipline.option.consume_durability.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.consumeDurability = newValue; })
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.zipline.option.release_cooldown"), config.releaseCooldown)
                .setDefaultValue(10)
                .setMin(0)
                .setTooltip(Component.translatable("config.zipline.option.release_cooldown.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.releaseCooldown = newValue; })
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.zipline.option.jump_required_to_dismount"), config.jumpRequiredToDismount)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.zipline.option.jump_required_to_dismount.tooltip"))
                .setSaveConsumer(newValue -> { if (!isServer) config.jumpRequiredToDismount = newValue; })
                .build());

        return builder.build();
    }
}
