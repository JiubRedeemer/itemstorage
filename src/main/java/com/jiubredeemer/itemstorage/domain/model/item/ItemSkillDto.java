package com.jiubredeemer.itemstorage.domain.model.item;

import com.jiubredeemer.itemstorage.domain.model.common.MultilingualField;
import lombok.Data;

import java.util.UUID;

@Data
public class ItemSkillDto {
    private UUID id;
    private UUID itemId;
    private MultilingualField name;
    private String description;
    private String shortDescription;
    private Integer charges;
    private String castTime;
    private String distance;
    private ChargesRefillEnum chargesRefill;
    private String imgUrl;
}
