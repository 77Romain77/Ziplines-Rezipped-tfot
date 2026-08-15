package com.evandev.zipline.logic;

import com.evandev.zipline.Cable;
import com.evandev.zipline.Cables;
import com.evandev.zipline.client.ZiplineClient;
import com.evandev.zipline.config.ModConfig;
import com.evandev.zipline.duck.ZiplinePlayerDuck;
import com.evandev.zipline.mixin.LivingEntityAccessor;
import com.evandev.zipline.registry.ZiplineSoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.Mth;

public class ZiplineLogic {
    private static final double ATTACH_THRESHOLD_PADDING = 1.01;
    private static final double REATTACH_DISTANCE_TOLERANCE = 0.05;
    private static final double SAME_CABLE_EPSILON_SQR = 0.0001;
    private static final double TICKS_PER_SECOND = 20.0;

    public static void inventoryTick(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        ZiplinePlayerDuck duck = (ZiplinePlayerDuck) player;

        if (player.onGround() && duck.zipline$getLastExitedCable() != null) {
            clearExitRestriction(duck);
        }

        if (duck.zipline$isActuallyUsing() && !player.isUsingItem()) {
            interruptUsing(player, duck);
        }
    }

    public static void tick(Level level, LivingEntity livingEntity, ItemStack stack) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        if (!level.isClientSide) {
            Vec3 offsetPlayerPos = player.position().add(0, ModConfig.get().hangOffset, 0);
            Cable cable = Cables.getClosestCable(level, offsetPlayerPos, ModConfig.get().snapRadius);

            if (cable != null) {
                Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
                if (closestPoint.distanceToSqr(offsetPlayerPos) < 0.25) {
                    player.fallDistance = 0.0F;

                    if (ModConfig.get().consumeDurability && player.tickCount % 40 == 0) {
                        EquipmentSlot slot = player.getOffhandItem() == stack ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                        stack.hurtAndBreak(1, player, (entity) -> entity.broadcastBreakEvent(slot));
                    }
                }
            }
            return;
        }

        if (!player.isLocalPlayer()) {
            return;
        }

        ZiplinePlayerDuck duck = (ZiplinePlayerDuck) player;
        if (!duck.zipline$isActuallyUsing()) {
            attemptAttach(player, duck);
        } else {
            ziplineTick(player, duck, stack);
        }
    }

    private static void attemptAttach(Player player, ZiplinePlayerDuck duck) {
        if (player.onGround()) {
            clearExitRestriction(duck);
            return;
        }

        Vec3 playerPos = player.position();
        Vec3 offsetPlayerPos = playerPos.add(0, ModConfig.get().hangOffset, 0);

        Cable cable = Cables.getClosestCable(player.level(), offsetPlayerPos, ModConfig.get().snapRadius);

        if (cable == null || !cable.isValid()) {
            return;
        }

        Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
        Vec3 playerAttachPos = closestPoint.add(0, -ModConfig.get().hangOffset, 0);
        double progress = cable.getProgress(offsetPlayerPos);

        if (isBlockedUphillReattach(duck, cable, progress)) {
            return;
        }

        if (closestPoint.y > playerPos.y + ATTACH_THRESHOLD_PADDING * ModConfig.get().hangOffset
                && !isInvalidPosition(player, playerAttachPos.subtract(playerPos))) {
            enable(player, duck, cable, offsetPlayerPos);
        }
    }

    private static void enable(Player player, ZiplinePlayerDuck duck, Cable cable, Vec3 offsetPlayerPos) {
        duck.zipline$setActuallyUsing(true);
        duck.zipline$setCable(cable);
        duck.zipline$setAttachTicks(0);
        duck.zipline$setWasJumpingAtAttach(((LivingEntityAccessor) player).zipline$isJumping());
        duck.zipline$setLastDir(Vec3.ZERO);

        double initialSpeed = Math.min(player.getDeltaMovement().length(), 0.5);
        duck.zipline$setSpeed(initialSpeed);

        double progress = cable.getProgress(offsetPlayerPos);
        duck.zipline$setProgress(progress);

        int downhillDirection = getDownhillDirection(cable);
        int dirFactor;
        if (ModConfig.get().downhillOnly && downhillDirection != 0) {
            dirFactor = downhillDirection;
        } else {
            dirFactor = player.getLookAngle().dot(cable.direction(progress)) >= 0 ? 1 : -1;
        }
        duck.zipline$setDirectionFactor(dirFactor);
        clearExitRestriction(duck);

        double futureT = Mth.clamp(progress + dirFactor * 0.1 / Math.max(1.0, cable.length()), 0.0, 1.0);
        Vec3 delta = cable.getPoint(futureT).subtract(offsetPlayerPos);

        float rawYaw = (float) (Mth.atan2(delta.z, delta.x) * 57.2957763671875 - player.getYRot());
        float clampedYaw = Mth.clamp(Mth.wrapDegrees(rawYaw), -15.0F, 15.0F) * 0.3F;

        ZiplineClient.ziplineTilt(clampedYaw);
        player.playSound(ZiplineSoundEvents.ZIPLINE_ATTACH.get(), 0.6f, 1);
    }

    private static void ziplineTick(Player player, ZiplinePlayerDuck duck, ItemStack stack) {
        int attachTicks = duck.zipline$getAttachTicks();
        duck.zipline$setAttachTicks(attachTicks + 1);

        if (player.isShiftKeyDown()) {
            detach(player, stack, false);
            player.stopUsingItem();
            return;
        }

        boolean isJumping = ((LivingEntityAccessor) player).zipline$isJumping();
        if (duck.zipline$wasJumpingAtAttach() && !isJumping) {
            duck.zipline$setWasJumpingAtAttach(false);
        }

        boolean canJumpDismount = attachTicks >= 5 && !duck.zipline$wasJumpingAtAttach();
        if (isJumping && canJumpDismount) {
            detach(player, stack, true);
            player.stopUsingItem();
            return;
        }

        if (player.onGround()) {
            interruptUsing(player, duck);
            clearExitRestriction(duck);
            return;
        }

        if (stack.isEmpty()) {
            interruptUsing(player, duck);
            return;
        }

        Cable cable = duck.zipline$getCable();
        if (cable == null || !cable.isValid()) {
            interruptUsing(player, duck);
            return;
        }

        double oldProgress = duck.zipline$getProgress();
        double velocity = duck.zipline$getSpeed() * duck.zipline$getDirectionFactor();
        int downhillDirection = getDownhillDirection(cable);
        boolean forceDownhill = ModConfig.get().downhillOnly && downhillDirection != 0;

        if (ModConfig.get().realisticPhysics) {
            double deltaT = 0.1 / Math.max(1.0, cable.length());
            double tForward = Math.min(1.0, oldProgress + deltaT);
            double tBackward = Math.max(0.0, oldProgress - deltaT);

            Vec3 pForward = cable.getPoint(tForward);
            Vec3 pBackward = cable.getPoint(tBackward);
            Vec3 tangent = pForward.subtract(pBackward).normalize();

            double gravityStrength = Math.max(0.0, ModConfig.get().gravityStrength);
            double velocityRetention = Mth.clamp(ModConfig.get().velocityRetention, 0.0, 1.0);
            double acceleration = -gravityStrength * tangent.y;

            velocity += acceleration;
            velocity *= velocityRetention;

            if (Math.abs(velocity) < 0.01 && Math.abs(tangent.y) < 0.1) {
                velocity = 0;
            }
        } else {
            int intendedDir;
            if (forceDownhill) {
                intendedDir = downhillDirection;
            } else {
                intendedDir = player.getLookAngle().dot(cable.direction(oldProgress)) >= 0 ? 1 : -1;
            }
            velocity = Mth.lerp(0.05, velocity, 1.6 * intendedDir);
        }

        if (forceDownhill && velocity * downhillDirection < 0) {
            velocity = 0;
        }

        double speedMultiplier = ModConfig.get().speedMultiplier;
        if (forceDownhill) {
            speedMultiplier = Math.abs(speedMultiplier);
        }

        double maxSpeedBlocksPerSecond = ModConfig.get().maxSpeed;
        if (maxSpeedBlocksPerSecond > 0 && Math.abs(speedMultiplier) > 0.000001) {
            double maxSpeedPerTick = maxSpeedBlocksPerSecond / TICKS_PER_SECOND;
            double maxInternalSpeed = maxSpeedPerTick / Math.abs(speedMultiplier);
            velocity = Mth.clamp(velocity, -maxInternalSpeed, maxInternalSpeed);
        }

        int directionFactor;
        if (velocity > 0) {
            directionFactor = 1;
        } else if (velocity < 0) {
            directionFactor = -1;
        } else if (forceDownhill) {
            directionFactor = downhillDirection;
        } else {
            directionFactor = duck.zipline$getDirectionFactor();
        }

        duck.zipline$setSpeed(Math.abs(velocity));
        duck.zipline$setDirectionFactor(directionFactor);

        double moveDelta = (velocity * speedMultiplier) / Math.max(0.000001, cable.length());
        double newProgress = Mth.clamp(oldProgress + moveDelta, 0.0, 1.0);

        duck.zipline$setProgress(newProgress);

        Vec3 newPosition = cable.getPoint(newProgress);
        Vec3 newOffsetPosition = new Vec3(newPosition.x, newPosition.y - ModConfig.get().hangOffset, newPosition.z);

        Vec3 oldPosition = cable.getPoint(oldProgress);
        Vec3 lastDir = newPosition.subtract(oldPosition);
        duck.zipline$setLastDir(lastDir);

        if (isInvalidPosition(player, lastDir)) {
            duck.zipline$setSpeed(0);
            duck.zipline$setProgress(oldProgress);
            newProgress = oldProgress;

            newPosition = oldPosition;
            newOffsetPosition = new Vec3(newPosition.x, newPosition.y - ModConfig.get().hangOffset, newPosition.z);
            duck.zipline$setLastDir(Vec3.ZERO);
        }

        player.setPos(newOffsetPosition);
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0.0F;
        player.playSound(ZiplineSoundEvents.ZIPLINE_USE.get(), 1.0F, .3f + (float) duck.zipline$getSpeed());

        if (newProgress >= 1.0 || newProgress <= 0.0) {
            handleCableSwitch(player, duck, cable, directionFactor, duck.zipline$getLastDir());
        }
    }

    private static void handleCableSwitch(Player player, ZiplinePlayerDuck duck, Cable currentCable, int dirFactor, Vec3 lastDir) {
        CollectionCandidate best = null;
        Vec3 exitPos = currentCable.getPoint(dirFactor == 1 ? 1.0 : 0.0);
        Vec3 playerDir = player.getLookAngle();
        Vec3 movementDir = lastDir.lengthSqr() > 0.000001 ? lastDir.normalize() : Vec3.ZERO;

        for (Cable next : currentCable.getNext(dirFactor == 1)) {
            if (sameCable(currentCable, next)) {
                continue;
            }

            double distToStart = exitPos.distanceToSqr(next.getPoint(0));
            double distToEnd = exitPos.distanceToSqr(next.getPoint(1));
            boolean startAtBeginning = distToStart <= distToEnd;
            int travelDir = startAtBeginning ? 1 : -1;

            int nextDownhillDirection = getDownhillDirection(next);
            if (ModConfig.get().downhillOnly && nextDownhillDirection != 0 && travelDir != nextDownhillDirection) {
                continue;
            }

            double entryProgress = startAtBeginning ? 0.0 : 1.0;
            Vec3 travelDirection = next.direction(entryProgress).scale(travelDir).normalize();
            double alignment = movementDir == Vec3.ZERO ? 1.0 : travelDirection.dot(movementDir);

            if (alignment <= ModConfig.get().maxTurnAngle) {
                continue;
            }

            double lookDotProduct = travelDirection.dot(playerDir);
            if (best == null || lookDotProduct > best.lookDotProduct) {
                best = new CollectionCandidate(next, startAtBeginning, travelDir, lookDotProduct);
            }
        }

        if (best == null) {
            interruptUsing(player, duck);
            return;
        }

        duck.zipline$setCable(best.cable);
        duck.zipline$setProgress(best.startAtBeginning ? 0.0 : 1.0);
        duck.zipline$setDirectionFactor(best.travelDir);
    }

    private static void interruptUsing(Player player, ZiplinePlayerDuck duck) {
        if (!player.onGround()) {
            rememberExit(duck);
        }

        player.stopUsingItem();
        applyExitMomentum(player, duck, false);
        disable(duck);
        player.playSound(ZiplineSoundEvents.ZIPLINE_INTERRUPT.get(), 0.5f, 1);
    }

    public static void disable(ZiplinePlayerDuck duck) {
        duck.zipline$setCable(null);
        duck.zipline$setActuallyUsing(false);
        duck.zipline$setSpeed(0);
        duck.zipline$setAttachTicks(0);
        duck.zipline$setWasJumpingAtAttach(false);
    }

    public static void release(Player player, ItemStack stack) {
        boolean isJumping = ((LivingEntityAccessor) player).zipline$isJumping();
        boolean boostedExit = isJumping || !ModConfig.get().jumpRequiredToDismount;
        detach(player, stack, boostedExit);
    }

    private static void detach(Player player, ItemStack stack, boolean boostedExit) {
        ZiplinePlayerDuck duck = (ZiplinePlayerDuck) player;

        player.getCooldowns().addCooldown(stack.getItem(), ModConfig.get().releaseCooldown);

        if (!duck.zipline$isActuallyUsing()) {
            return;
        }

        rememberExit(duck);

        if (boostedExit) {
            double jumpY = 0.5 * ModConfig.get().exitJumpMultiplier;
            player.addDeltaMovement(new Vec3(0, jumpY, 0));
        }

        applyExitMomentum(player, duck, boostedExit);
        disable(duck);
    }

    private static void applyExitMomentum(LivingEntity livingEntity, ZiplinePlayerDuck duck, boolean boostedExit) {
        Vec3 lastDir = duck.zipline$getLastDir();
        if (lastDir != null) {
            livingEntity.addDeltaMovement(lastDir);
        }

        if (boostedExit && ModConfig.get().exitJumpUsesLookDirection) {
            Vec3 look = livingEntity.getLookAngle();
            Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
            livingEntity.addDeltaMovement(horizontalLook.scale(0.5));
        }
    }

    private static void rememberExit(ZiplinePlayerDuck duck) {
        if (!ModConfig.get().downhillOnly) {
            clearExitRestriction(duck);
            return;
        }

        Cable cable = duck.zipline$getCable();
        if (cable == null || getDownhillDirection(cable) == 0) {
            clearExitRestriction(duck);
            return;
        }

        duck.zipline$setLastExitedCable(cable);
        duck.zipline$setLastExitDownhillProgress(getDownhillProgress(cable, duck.zipline$getProgress()));
    }

    private static void clearExitRestriction(ZiplinePlayerDuck duck) {
        duck.zipline$setLastExitedCable(null);
        duck.zipline$setLastExitDownhillProgress(0.0);
    }

    private static boolean isBlockedUphillReattach(ZiplinePlayerDuck duck, Cable cable, double progress) {
        if (!ModConfig.get().downhillOnly) {
            return false;
        }

        Cable lastExitedCable = duck.zipline$getLastExitedCable();
        if (lastExitedCable == null || !sameCable(lastExitedCable, cable)) {
            return false;
        }

        int downhillDirection = getDownhillDirection(cable);
        if (downhillDirection == 0) {
            return false;
        }

        double candidateDownhillProgress = getDownhillProgress(cable, progress);
        double progressTolerance = REATTACH_DISTANCE_TOLERANCE / Math.max(1.0, cable.length());
        return candidateDownhillProgress + progressTolerance
                < duck.zipline$getLastExitDownhillProgress();
    }

    private static int getDownhillDirection(Cable cable) {
        double startY = cable.getPoint(0.0).y;
        double endY = cable.getPoint(1.0).y;
        double deltaY = endY - startY;

        if (Math.abs(deltaY) <= ModConfig.get().downhillHeightTolerance) {
            return 0;
        }

        return deltaY < 0 ? 1 : -1;
    }

    private static double getDownhillProgress(Cable cable, double progress) {
        return getDownhillDirection(cable) < 0 ? 1.0 - progress : progress;
    }

    private static boolean sameCable(Cable first, Cable second) {
        if (first == second || first.equals(second)) {
            return true;
        }

        Vec3 firstStart = first.getPoint(0.0);
        Vec3 firstEnd = first.getPoint(1.0);
        Vec3 secondStart = second.getPoint(0.0);
        Vec3 secondEnd = second.getPoint(1.0);

        boolean sameOrientation = firstStart.distanceToSqr(secondStart) <= SAME_CABLE_EPSILON_SQR
                && firstEnd.distanceToSqr(secondEnd) <= SAME_CABLE_EPSILON_SQR;
        boolean reverseOrientation = firstStart.distanceToSqr(secondEnd) <= SAME_CABLE_EPSILON_SQR
                && firstEnd.distanceToSqr(secondStart) <= SAME_CABLE_EPSILON_SQR;

        return sameOrientation || reverseOrientation;
    }

    private static boolean isInvalidPosition(Player player, Vec3 deltaPos) {
        AABB collisionBox = player.getBoundingBox().move(deltaPos);
        Iterable<VoxelShape> blockCollisions = player.level().getBlockCollisions(player, collisionBox);
        for (VoxelShape shape : blockCollisions) {
            if (!shape.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static final class CollectionCandidate {
        private final Cable cable;
        private final boolean startAtBeginning;
        private final int travelDir;
        private final double lookDotProduct;

        private CollectionCandidate(Cable cable, boolean startAtBeginning, int travelDir, double lookDotProduct) {
            this.cable = cable;
            this.startAtBeginning = startAtBeginning;
            this.travelDir = travelDir;
            this.lookDotProduct = lookDotProduct;
        }
    }
}
