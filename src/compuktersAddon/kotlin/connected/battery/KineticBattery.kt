/* Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0. */
package connected.battery

import compukter.peripheral.Peripheral
import compukter.peripheral.TypedPeripheralProvider

/** Native charge is measured in stress-unit ticks (one su-hour is 72000 units). */
public value class KineticBatteryState(
    public val gameTick: Long,
    public val charge: Double,
    public val capacity: Double,
    public val discharging: Boolean,
    public val speedRpm: Float,
    public val generatedSpeedRpm: Float,
    public val consumedStress: Float,
    public val controlled: Boolean,
)

/** An exact native battery. Control never manufactures charge or changes its native RPM/stress limits. */
public value class KineticBattery internal constructor(private val handle: Int) : Peripheral {
    public companion object : TypedPeripheralProvider<KineticBattery>("connected:kinetic_battery") {
        override fun wrap(handle: Int): KineticBattery = KineticBattery(handle)
    }
    public fun state(): KineticBatteryState = BatteryBindings.batteryState(handle)
    /** Overrides native redstone until close/program exit. False selects native charging mode. */
    public fun setDischarging(enabled: Boolean) { BatteryBindings.setDischarging(handle, enabled) }
    /** Releases program control and restores native redstone, retaining all charge. */
    public fun close() { BatteryBindings.batteryClose(handle) }
}
private object BatteryBindings {
    external fun batteryState(handle: Int): KineticBatteryState
    external fun setDischarging(handle: Int, enabled: Boolean)
    external fun batteryClose(handle: Int)
}
