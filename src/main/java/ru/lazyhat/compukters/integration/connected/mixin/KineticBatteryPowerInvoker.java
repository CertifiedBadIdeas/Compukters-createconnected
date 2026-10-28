/* Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0. */
package ru.lazyhat.compukters.integration.connected.mixin;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = KineticBatteryBlock.class, remap = false)
public interface KineticBatteryPowerInvoker {
    @Invoker("updatePower") void compukters$updatePower(BlockState state, Level level, BlockPos position);
}
