package com.allfire.regionbetter.flags;

import com.sk89q.worldguard.protection.flags.StateFlag;

/**
 * Флаг для отображения границ региона
 * Срабатывает когда игрок подходит к границе региона
 */
public class RegionBetterViewFlag extends StateFlag {

    public RegionBetterViewFlag(String name) {
        super(name, false); // false = по умолчанию выключен
    }
}