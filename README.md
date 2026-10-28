# Compukters: Connected

A standalone optional NeoForge addon for Create: Connected 1.3.3, Minecraft 1.21.1 and the local Compukters SDK.
It has its own repository, independent Gradle build and Guest bundle;
the base mod has no dependency on Connected. This development version exposes native kinetic batteries.

Add `"connected"` to a Guest project's addons. Bind and name the native battery through the configurator.
`connected.battery.KineticBattery.named("starter").state()` reports native charge/capacity in su-ticks
(72000 per su-hour), server tick, discharge mode, actual/generated RPM, native consumed stress and whether
this program has selected a mode. `setDischarging(true)` selects native discharge; `false` selects charging.
The engine's native limits, actual network demand and finite charge remain authoritative.

Mode control overrides redstone only for the exact program-owned peripheral lease. `close()`, program exit,
network loss and reset restore ordinary native redstone without erasing charge. No charge setter is exported.
Saved control sessions are unsupported and cold boot retains only the native battery's own persisted charge.

Build with `./gradlew-sandbox-dev-parallel-summary check`
and an absolute `-PcompuktersWorkspace` pointing at the prepared Compukters task checkout. Prepare its SDK first with
`stageAddonDevelopmentDependencies`. The addon also supports the published SDK selection settings.
