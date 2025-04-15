package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryDto;
import com.jiubredeemer.itemstorage.domain.model.money.MoneyDto;
import com.jiubredeemer.itemstorage.domain.service.InventoryService;
import com.jiubredeemer.itemstorage.domain.service.MoneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final MoneyService moneyService;

    @GetMapping("/{characterId}")
    public InventoryDto findByCharacterId(@PathVariable UUID characterId) {
        return inventoryService.getInventoryByRoomIdAndCharacterId(characterId);
    }

    @PatchMapping("/{characterId}/equip/{itemId}")
    public InventoryDto equipItemByCharacterIdAndItemId(@PathVariable UUID characterId, @PathVariable UUID itemId) {
        return inventoryService.equipItemByCharacterIdAndItemId(characterId, itemId);
    }

    @PatchMapping("/{characterId}/{itemId}/count/{count}")
    public InventoryDto changeItemCount(@PathVariable UUID characterId, @PathVariable UUID itemId, @PathVariable Long count) {
        return inventoryService.changeItemCount(characterId, itemId, count);
    }

    @GetMapping("/{characterId}/money")
    public MoneyDto findMoneyByCharacterId(@PathVariable UUID characterId) {
        return moneyService.findByCharacterId(characterId);
    }

    @PatchMapping("/{characterId}/money")
    public MoneyDto changeMoneyCount(@PathVariable UUID characterId, @RequestBody MoneyDto moneyDto) {
        return moneyService.changeMoneyCount(characterId, moneyDto);
    }
}
