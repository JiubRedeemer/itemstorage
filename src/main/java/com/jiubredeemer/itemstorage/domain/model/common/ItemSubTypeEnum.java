package com.jiubredeemer.itemstorage.domain.model.common;

import lombok.Getter;

@Getter
public enum ItemSubTypeEnum {
    SHW("Простое рукопашное"),
    SRW("Простое дальнобойное"),
    AHW("Воинское рукопашное"),
    ARW("Воинское дальнобойное"),
    EHW("Экзотическое рукопашное"),
    ERW("Экзотическое дальнобоайное"),

    HEAVY_ARMOR("Тяжелый доспех"),
    MEDIUM_ARMOR("Средний доспех"),
    LIGHT_ARMOR("Легкий доспех"),
    SHIELD("Щит"),

    NO("");

    private final String name;

    ItemSubTypeEnum(String name) {
        this.name = name;
    }
}
