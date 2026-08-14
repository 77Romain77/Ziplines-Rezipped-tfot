package com.evandev.zipline.network;

import com.evandev.zipline.config.ModConfig;
import net.minecraft.network.FriendlyByteBuf;

public record ConfigSyncPayload(
        double snapRadius,
        double clickReach,
        boolean useAnywhere,
        double maxTurnAngle,
        double hangOffset,
        double speedMultiplier,
        boolean realisticPhysics,
        double maxSpeed,
        boolean downhillOnly,
        double downhillHeightTolerance,
        boolean autoDetachAtEnd,
        double exitJumpMultiplier,
        boolean exitJumpUsesLookDirection,
        boolean consumeDurability,
        int releaseCooldown,
        boolean jumpRequiredToDismount
) {

    public ConfigSyncPayload(FriendlyByteBuf buf) {
        this(
                buf.readDouble(), buf.readDouble(), buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readBoolean(), buf.readDouble(), buf.readBoolean(),
                buf.readDouble(), buf.readBoolean(), buf.readDouble(),
                buf.readBoolean(), buf.readBoolean(), buf.readInt(),
                buf.readBoolean()
        );
    }

    public static ConfigSyncPayload fromModConfig(ModConfig config) {
        return new ConfigSyncPayload(
                config.snapRadius, config.clickReach, config.useAnywhere,
                config.maxTurnAngle, config.hangOffset, config.speedMultiplier,
                config.realisticPhysics, config.maxSpeed, config.downhillOnly,
                config.downhillHeightTolerance, config.autoDetachAtEnd,
                config.exitJumpMultiplier, config.exitJumpUsesLookDirection,
                config.consumeDurability, config.releaseCooldown,
                config.jumpRequiredToDismount
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(snapRadius);
        buf.writeDouble(clickReach);
        buf.writeBoolean(useAnywhere);
        buf.writeDouble(maxTurnAngle);
        buf.writeDouble(hangOffset);
        buf.writeDouble(speedMultiplier);
        buf.writeBoolean(realisticPhysics);
        buf.writeDouble(maxSpeed);
        buf.writeBoolean(downhillOnly);
        buf.writeDouble(downhillHeightTolerance);
        buf.writeBoolean(autoDetachAtEnd);
        buf.writeDouble(exitJumpMultiplier);
        buf.writeBoolean(exitJumpUsesLookDirection);
        buf.writeBoolean(consumeDurability);
        buf.writeInt(releaseCooldown);
        buf.writeBoolean(jumpRequiredToDismount);
    }

    public ModConfig toModConfig() {
        ModConfig config = new ModConfig();
        config.snapRadius = this.snapRadius;
        config.clickReach = this.clickReach;
        config.useAnywhere = this.useAnywhere;
        config.maxTurnAngle = this.maxTurnAngle;
        config.hangOffset = this.hangOffset;
        config.speedMultiplier = this.speedMultiplier;
        config.realisticPhysics = this.realisticPhysics;
        config.maxSpeed = this.maxSpeed;
        config.downhillOnly = this.downhillOnly;
        config.downhillHeightTolerance = this.downhillHeightTolerance;
        config.autoDetachAtEnd = this.autoDetachAtEnd;
        config.exitJumpMultiplier = this.exitJumpMultiplier;
        config.exitJumpUsesLookDirection = this.exitJumpUsesLookDirection;
        config.consumeDurability = this.consumeDurability;
        config.releaseCooldown = this.releaseCooldown;
        config.jumpRequiredToDismount = this.jumpRequiredToDismount;
        return config;
    }
}
