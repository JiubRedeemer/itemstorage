package com.jiubredeemer.itemstorage.domain.model.common;

import lombok.Getter;

@Getter
public enum DamageTypeEnum {
    CRUSHING("Дробящий"),
    STABBING("Колющий"),
    CHOPPING("Рубящий"),
    PIERCING("Колющий"),
    SLASHING("Рубящий"),
    BLUDGEONING("Дробящий"),
    ACID("Кислотный"),
    COLD("Холодом"),
    FIRE("Огненный"),
    FORCE("Силовой"),
    LIGHTNING("Молнией"),
    NECROTIC("Некротический"),
    POISON("Ядовитый"),
    PSYCHIC("Психический"),
    RADIANT("Сияющий"),
    THUNDER("Громовой"),
    NO("");
    final String name;

    DamageTypeEnum(String name) {
        this.name = name;
    }
}
