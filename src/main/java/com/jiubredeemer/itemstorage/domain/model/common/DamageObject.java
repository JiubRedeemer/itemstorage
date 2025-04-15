package com.jiubredeemer.itemstorage.domain.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamageObject {
    private String value;
    private DamageTypeEnum damageType;
}
