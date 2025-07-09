package com.jiubredeemer.itemstorage.domain.model.item;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jiubredeemer.itemstorage.domain.model.common.DamageObject;
import com.jiubredeemer.itemstorage.domain.model.common.PriceObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<String> tags;
}
