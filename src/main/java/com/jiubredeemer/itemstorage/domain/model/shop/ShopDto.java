package com.jiubredeemer.itemstorage.domain.model.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopDto {
    private UUID id;
    private UUID roomId;
    /**
     * Ссылка на NPC-продавца (charactersheet), без FK. Может быть null.
     */
    private UUID npcId;
    private String name;
    private String description;
    private String imgUrl;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
