package com.jiubredeemer.itemstorage.domain.model.common;

import lombok.Getter;

@Getter
public enum DamageTypeEnum {
    CRUSHING("Дробящий"),
    STABBING("Колющий"),
    CHOPPING("Рубящий"),
    NO("");
    final String name;

    DamageTypeEnum(String name) {
        this.name = name;
    }
}
