// Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0.
package ru.lazyhat.compukters.integration.connected

import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlock
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlockEntity
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.fluids.pump.PumpBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

@PrefixGameTestTemplate(false)
object BatteryGameTests {
    @JvmStatic
    @GameTest(batch = "kinetic_battery", template = "bastion/mobs/empty", templateNamespace = "minecraft", timeoutTicks = 400)
    fun nativeDischargeOverrideAndRelease(helper: GameTestHelper) {
        val pos = BlockPos(3, 3, 3)
        val batteryState =
            BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("create_connected:kinetic_battery"))
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST)
        helper.setBlock(pos, batteryState)
        val pump = pos.east().below().north()
        helper.setBlock(pos.east(), AllBlocks.LARGE_COGWHEEL.defaultState.setValue(BlockStateProperties.AXIS, Direction.Axis.X))
        helper.setBlock(pump, AllBlocks.MECHANICAL_PUMP.defaultState.setValue(PumpBlock.FACING, Direction.EAST))
        lateinit var endpoint: BatteryEndpoint
        var start = 0.0
        helper
            .startSequence()
            .thenExecuteAfter(5) {
                val battery = helper.getBlockEntity(pos) as KineticBatteryBlockEntity
                // A bounded native initial test stock; production Guest API has no charge setter.
                battery.setBatteryLevel(KineticBatteryBlockEntity.getMaxBatteryLevel() * 0.1)
                endpoint = BatteryEndpoint(battery) { true }
                endpoint.acquire()
                helper.assertTrue(!endpoint.state().controlled, "Read-only acquisition manufactured an actuator command")
                endpoint.setDischarging(true)
                start = endpoint.state().charge
            }.thenWaitUntil {
                val sample = endpoint.state()
                helper.assertTrue(
                    sample.discharging && sample.controlled && sample.generatedSpeedRpm != 0f,
                    "Command did not use native discharge",
                )
                helper.assertTrue(
                    (helper.getBlockEntity(pump) as KineticBlockEntity).speed != 0f,
                    "Native battery did not power the pump",
                )
            }.thenExecuteAfter(40) {
                helper.assertTrue(endpoint.state().charge < start, "Loaded pump did not consume finite native charge")
                endpoint.setDischarging(false)
                helper.setBlock(pos.below(), Blocks.REDSTONE_BLOCK)
            }.thenExecuteAfter(5) {
                helper.assertTrue(!endpoint.state().discharging, "Redstone bypassed the exact program's mode command")
                endpoint.release()
                helper.assertTrue(
                    KineticBatteryBlock.isDischarging(helper.level.getBlockState(helper.absolutePos(pos))),
                    "Release did not restore native redstone",
                )
                val retained = endpoint.state().charge
                helper.assertTrue(retained > 0.0 && retained <= start, "Mode control erased or created charge")
                helper.setBlock(pos, Blocks.AIR)
                helper.setBlock(pos, batteryState)
                helper.assertTrue(!endpoint.valid(), "Endpoint rebound to a replacement")
                endpoint.release()
                helper.assertTrue(
                    !(helper.getBlockEntity(pos) as KineticBatteryBlockEntity).let {
                        (it as BatteryControl).`compukters$getPower`() >=
                            0
                    },
                    "Replacement inherited control",
                )
            }.thenSucceed()
    }
}

@EventBusSubscriber(modid = "compukters_connected", bus = EventBusSubscriber.Bus.MOD)
object BatteryGameTestRegistration {
    @JvmStatic @SubscribeEvent
    fun register(event: RegisterGameTestsEvent) {
        event.register(BatteryGameTests::class.java)
    }
}
