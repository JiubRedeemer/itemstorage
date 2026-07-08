package com.jiubredeemer.itemstorage.domain.model.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jiubredeemer.itemstorage.domain.model.common.ItemSubTypeEnum;
import com.jiubredeemer.itemstorage.domain.model.common.ItemTypeEnum;
import com.jiubredeemer.itemstorage.domain.model.common.MultilingualField;
import com.jiubredeemer.itemstorage.domain.model.common.RarityEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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
    private ItemOptionsDto stats;
    private List<ItemSkillDto> skills;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
    private UUID roomId;
    private UUID creatorId;
    private String imgUrl;
    private Boolean visibleForPlayers;
    private String creator;
    private Boolean hiddenStats;
    private UUID unidentifiedItemId;
    private UUID itemBundleId;
    private String itemBundleName;
}
