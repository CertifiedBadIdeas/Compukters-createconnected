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
An unused host, or one whose battery handles have all been closed, supports an empty checkpoint so installing
Connected does not interrupt unrelated running programs during unload or ship transfer. Active battery
sessions remain unsupported: capture rejects them and cold boot retains the native battery's persisted charge.

Build with `./gradlew-sandbox-dev-parallel-summary check`
and an absolute `-PcompuktersWorkspace` pointing at the prepared Compukters task checkout. Prepare its SDK first with
`stageAddonDevelopmentDependencies`. The addon also supports the published SDK selection settings.

## Declarative Guest contracts

Guest API declarations live in `src/contract/kotlin`. The SDK generates the actual Guest Kotlin under
`build/generated/sources/compuktersGuest/kotlin` and packages those sources with the executable `.cagb` image.
Run `generateCompuktersGuestSources` to inspect the result. Check in `src/contract/addon.lock` as ABI history;
only `updateCompuktersAddonAbiLock` changes selectors/tombstones after an intentional contract edit.
Unit-returning `@GuestCommand` methods get ordinary calls and batch command factories from the same declaration.
The generated host validates declared parameter constraints and derives command eligibility.
See the [SDK authoring guide](https://github.com/CertifiedBadIdeas/Compukters/blob/dev/docs/developers/addon-development.md).
