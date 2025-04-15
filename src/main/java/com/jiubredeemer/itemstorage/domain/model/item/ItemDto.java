package com.jiubredeemer.itemstorage.domain.model.item;

import com.jiubredeemer.itemstorage.domain.model.common.ItemSubTypeEnum;
import com.jiubredeemer.itemstorage.domain.model.common.ItemTypeEnum;
import com.jiubredeemer.itemstorage.domain.model.common.MultilingualField;
import com.jiubredeemer.itemstorage.domain.model.common.RarityEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {
    private UUID id;
    private MultilingualField name;
    private ItemTypeEnum type;
    private ItemSubTypeEnum subtype;
    private Boolean customization;
    private RarityEnum rarity;
    private String description;
    private ItemStatsDto stats;
    private Timestamp createdAt;
    private UUID roomId;
    private UUID creatorId;
    private String imgUrl;
}
