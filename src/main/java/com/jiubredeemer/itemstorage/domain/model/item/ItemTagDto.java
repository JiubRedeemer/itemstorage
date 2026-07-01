package com.jiubredeemer.itemstorage.domain.model.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemTagDto {
    private UUID id;
    private String name;
    private String description;
    private UUID roomId;
}
