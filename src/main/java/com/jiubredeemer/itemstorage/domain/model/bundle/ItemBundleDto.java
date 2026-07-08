package com.jiubredeemer.itemstorage.domain.model.bundle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemBundleDto {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private String imgUrl;
    /**
     * null = системный (официальный) бандл.
     */
    private UUID ownerUserId;
    private Boolean isPublic;
    private Integer priceCrystals;
    /**
     * Куплен ли бандл запрашивающим пользователем (заполняется при листинге с viewer-контекстом).
     */
    private Boolean purchased;
    /**
     * Populated only when the bundle is listed in the context of a specific room
     * (null when listing bundles outside of any room context).
     */
    private Boolean enabled;
}
