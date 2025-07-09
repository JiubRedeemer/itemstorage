package com.jiubredeemer.itemstorage.domain.model.bonus;

import com.jiubredeemer.itemstorage.domain.model.item.StatsDto;
import lombok.Data;

import java.util.List;

@Data
public class EquippedItemsStatsResponse {
    private List<StatsDto> armoryClassBonus;
    private List<StatsDto> speedBonus;
    private List<StatsDto> hpBonus;
}
