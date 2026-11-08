// Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0.
package connected.battery

import ru.lazyhat.compukters.api.contracts.GuestCall
import ru.lazyhat.compukters.api.contracts.GuestCommand
import ru.lazyhat.compukters.api.contracts.GuestDevice
import ru.lazyhat.compukters.api.contracts.GuestRead
import ru.lazyhat.compukters.api.contracts.GuestRecord

/** Native charge is measured in stress-unit ticks (one su-hour is 72000 units). */
@GuestRecord
value class KineticBatteryState(
    val gameTick: Long,
    val charge: Double,
    val capacity: Double,
    val discharging: Boolean,
    val speedRpm: Float,
    val generatedSpeedRpm: Float,
    val consumedStress: Float,
    val controlled: Boolean,
)

/** An exact native battery. Control never manufactures charge or changes its native RPM/stress limits. */
@GuestDevice("connected:kinetic_battery", bindings = "BatteryBindings")
interface KineticBattery {
    @GuestRead(binding = "batteryState")
    fun state(): KineticBatteryState

    /** Overrides native redstone until close/program exit. False selects native charging mode. */
    @GuestCommand(binding = "setDischarging")
    fun setDischarging(enabled: Boolean): Unit

    /** Releases program control and restores native redstone, retaining all charge. */
    @GuestCall(binding = "batteryClose")
    fun close(): Unit
}
