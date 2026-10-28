// Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0.
package ru.lazyhat.compukters.integration.connected

import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlock
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlockEntity
import connected.ConnectedAddonContract
import connected.ConnectedCapabilityHandler
import connected.KineticBatteryState
import net.neoforged.fml.common.Mod
import ru.lazyhat.compukters.api.addon.AddonCallResult
import ru.lazyhat.compukters.api.addon.addonCompleted
import ru.lazyhat.compukters.api.addon.addonFailed
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersAddonHostFactory
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersAddonRegistry
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersComputerContext
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersLeasedPeripheralEndpoint
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersPeripheralAccessException
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersPeripheralContract
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersPeripheralDevice
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersPeripheralProvider
import ru.lazyhat.compukters.integration.connected.mixin.KineticBatteryPowerInvoker

@Mod("compukters_connected")
class ConnectedAddon {
    init {
        CompuktersAddonRegistry.register(
            ConnectedAddonContract.guestApi(ConnectedAddon::class.java),
            CompuktersAddonHostFactory { ConnectedAddonContract.host(BatteryHost(it)) },
            CompuktersPeripheralProvider { contact ->
                if (!contact.level.hasChunkAt(contact.position)) {
                    null
                } else {
                    (contact.level.getBlockEntity(contact.position) as? KineticBatteryBlockEntity)
                        ?.takeUnless { it.isRemoved }
                        ?.let { CompuktersPeripheralDevice(it.blockPos, "kinetic_battery") }
                }
            },
            listOf(BatteryEndpoint.contract),
        )
    }
}

internal class BatteryEndpoint(
    private val entity: KineticBatteryBlockEntity,
    private val reachable: () -> Boolean,
) : CompuktersLeasedPeripheralEndpoint {
    override val identity: Any get() = entity

    override fun valid(): Boolean {
        val level = entity.level ?: return false
        check(level.server?.isSameThread == true)
        return !entity.isRemoved && level.hasChunkAt(entity.blockPos) &&
            level.getBlockEntity(entity.blockPos) === entity && reachable()
    }

    override fun acquire() = Unit

    override fun release() {
        val level = entity.level ?: return
        (entity as BatteryControl).`compukters$setPower`(-1)
        // A replacement must never inherit the old program's actuator command.
        if (!entity.isRemoved && level.hasChunkAt(entity.blockPos) && level.getBlockEntity(entity.blockPos) === entity) refresh()
    }

    private fun refresh() {
        (entity.blockState.block as KineticBatteryPowerInvoker).`compukters$updatePower`(
            entity.blockState,
            checkNotNull(entity.level),
            entity.blockPos,
        )
    }

    fun setDischarging(enabled: Boolean) {
        (entity as BatteryControl).`compukters$setPower`(if (enabled) 15 else 0)
        refresh()
    }

    fun state() =
        KineticBatteryState(
            checkNotNull(entity.level).gameTime,
            entity.batteryLevel,
            KineticBatteryBlockEntity.getMaxBatteryLevel(),
            KineticBatteryBlock.isDischarging(entity.blockState),
            entity.speed,
            entity.generatedSpeed,
            entity.consumedStress,
            (entity as BatteryControl).`compukters$getPower`() >= 0,
        )

    companion object {
        val contract =
            CompuktersPeripheralContract("connected:kinetic_battery", "kinetic_battery", "connected.battery.KineticBattery") { location ->
                if (!location.level.hasChunkAt(location.position)) {
                    null
                } else {
                    (location.level.getBlockEntity(location.position) as? KineticBatteryBlockEntity)
                        ?.takeUnless { it.isRemoved }
                        ?.let { BatteryEndpoint(it) { location.isReachable() } }
                }
            }
    }
}

internal class BatteryHost(
    private val computer: CompuktersComputerContext,
) : ConnectedCapabilityHandler {
    private val endpoints = mutableSetOf<BatteryEndpoint>()

    override fun batteryState(handle: Int): AddonCallResult<KineticBatteryState> = withBattery(handle) { it.state() }

    override fun setDischarging(
        handle: Int,
        enabled: Boolean,
    ): AddonCallResult<Unit> = withBattery(handle) { it.setDischarging(enabled) }

    override fun batteryClose(handle: Int): AddonCallResult<Unit> =
        withBattery(handle) {
            computer.closePeripheral(BatteryEndpoint.contract, handle)
            endpoints.remove(it)
            Unit
        }

    override fun reset() {
        endpoints.forEach { it.release() }
        endpoints.clear()
    }

    private fun <T> withBattery(
        handle: Int,
        action: (BatteryEndpoint) -> T,
    ): AddonCallResult<T> {
        check(computer.level.server.isSameThread)
        return try {
            val endpoint = computer.peripheral(BatteryEndpoint.contract, handle)
            endpoints.add(endpoint)
            addonCompleted(action(endpoint))
        } catch (failure: CompuktersPeripheralAccessException) {
            addonFailed(failure.kind, failure.message)
        }
    }
}
