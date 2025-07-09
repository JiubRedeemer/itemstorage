package com.jiubredeemer.itemstorage.domain.model.item;

import com.jiubredeemer.itemstorage.domain.model.common.ItemStatsEnum;
import lombok.Data;

import java.util.List;

@Data
public class StatsDto {
    private ItemStatsEnum name;
    private List<String> options;
    private Object value;
}
