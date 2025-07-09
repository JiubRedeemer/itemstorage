package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.bonus.EquippedItemsStatsResponse;
import com.jiubredeemer.itemstorage.domain.service.ItemStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/bonus/{roomId}/{characterId}")
@RequiredArgsConstructor
public class ItemStatsController {

    private final ItemStatsService itemStatsService;

    @GetMapping()
    public EquippedItemsStatsResponse getEquippedItemsStats(@PathVariable UUID roomId,
                                                            @PathVariable UUID characterId) {
        return itemStatsService.getEquippedItemsStats(roomId, characterId);
    }
}
