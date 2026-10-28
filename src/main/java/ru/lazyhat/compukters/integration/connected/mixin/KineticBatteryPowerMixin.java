/* Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0. */
package ru.lazyhat.compukters.integration.connected.mixin;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.lazyhat.compukters.integration.connected.BatteryControl;
@Mixin(value = KineticBatteryBlock.class, remap = false)
public abstract class KineticBatteryPowerMixin {
    @Inject(method = "calculatePower", at = @At("HEAD"), cancellable = true)
    private void compukters$programPower(BlockState state, Level level, BlockPos position, CallbackInfoReturnable<Integer> result) {
        if (!level.isClientSide && level.hasChunkAt(position)
            && level.getBlockEntity(position) instanceof BatteryControl control && control.compukters$getPower() >= 0) {
            result.setReturnValue(control.compukters$getPower());
        }
    }
}
