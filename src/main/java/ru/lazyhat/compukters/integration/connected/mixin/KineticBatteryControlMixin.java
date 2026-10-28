/* Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0. */
package ru.lazyhat.compukters.integration.connected.mixin;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.lazyhat.compukters.integration.connected.BatteryControl;
@Mixin(value = KineticBatteryBlockEntity.class, remap = false)
public abstract class KineticBatteryControlMixin implements BatteryControl {
    @Unique private int compukters$power = -1;
    public int compukters$getPower() { return compukters$power; }
    public void compukters$setPower(int power) { compukters$power = power; }
}
