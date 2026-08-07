package com.captainratax.simplenobeambeacon.sponge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BeaconBlockEntity.class, remap = false)
abstract class BeaconBlockEntityMixin {
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getLightDampening()I"
            ),
            remap = false
    )
    private static int simpleNoBeamBeacon$allowTintedGlassOnServer(
            BlockState scannedState,
            Operation<Integer> original,
            Level level,
            BlockPos beaconPos,
            BlockState beaconState,
            BeaconBlockEntity beacon
    ) {
        if (!level.isClientSide() && scannedState.is(Blocks.TINTED_GLASS)) {
            return 0;
        }
        return original.call(scannedState);
    }
}
