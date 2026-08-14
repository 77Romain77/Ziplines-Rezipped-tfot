package com.evandev.zipline.mixin;

import com.evandev.zipline.Cable;
import com.evandev.zipline.config.ModConfig;
import com.evandev.zipline.duck.ZiplinePlayerDuck;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.evandev.zipline.logic.ZiplineLogic", remap = false)
public abstract class ZiplineLogicMixin {

    @Inject(
            method = "handleCableSwitch",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/evandev/zipline/logic/ZiplineLogic;interruptUsing(Lnet/minecraft/world/entity/player/Player;Lcom/evandev/zipline/duck/ZiplinePlayerDuck;)V"
            ),
            cancellable = true,
            remap = false
    )
    private static void zipline$respectAutoDetachAtEnd(
            Player player,
            ZiplinePlayerDuck duck,
            Cable currentCable,
            int dirFactor,
            Vec3 lastDir,
            CallbackInfo ci
    ) {
        if (!ModConfig.get().autoDetachAtEnd) {
            ci.cancel();
        }
    }
}
