package com.jiubredeemer.itemstorage.domain.model.item;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jiubredeemer.itemstorage.domain.model.common.DamageObject;
import com.jiubredeemer.itemstorage.domain.model.common.PriceObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ItemOptionsDto {
    private List<PriceObject> defaultPrice;
    private Long weight;
    private DamageObject damage;
    private String armorClass;
    private String armorClassMaxDexterityBonus;
    private String requirement;
    /** Tag names for display (populated on read) */
    private List<String> tags;
    /** Tag UUIDs for create/edit relations (accepted on write, not persisted in JSON) */
    private List<UUID> tagIds;
    private String stealthDisadvantage;
}
