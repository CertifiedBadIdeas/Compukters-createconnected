/* Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0. */
package ru.lazyhat.compukters.integration.connected;
/** Transient, never persisted; -1 delegates to native redstone. */
public interface BatteryControl {
    int compukters$getPower();
    void compukters$setPower(int power);
}
