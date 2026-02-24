package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryDto;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ChargesRefillEnum;
import com.jiubredeemer.itemstorage.domain.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/{roomId}/inventory/{characterId}")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("")
    public InventoryDto findByCharacterId(@PathVariable UUID roomId, @PathVariable UUID characterId) {
        return inventoryService.getInventoryByRoomIdAndCharacterId(roomId, characterId);
    }

    @GetMapping("/items/{itemId}")
    public InventoryItemDto findItemById(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId) {
        return inventoryService.getItemByCharacterIdAndItemId(roomId, characterId, itemId);
    }

    @PatchMapping("/equip/{itemId}")
    public InventoryDto equipItemByCharacterIdAndItemId(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId) {
        return inventoryService.equipItemByCharacterIdAndItemId(roomId, characterId, itemId);
    }

    @PatchMapping("/items/{itemId}/attack/bonus/{value}")
    public InventoryDto addBonusAttack(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId, @PathVariable Long value) {
        return inventoryService.addBonusAttack(roomId, characterId, itemId, value);
    }

    @PatchMapping("/items/{itemId}/damage/bonus/{value}")
    public InventoryDto addBonusDamage(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId, @PathVariable Long value) {
        return inventoryService.addBonusDamage(roomId, characterId, itemId, value);
    }

    @PatchMapping("/{itemId}/count/{count}")
    public InventoryDto changeItemCount(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId, @PathVariable Long count) {
        return inventoryService.changeItemCount(roomId, characterId, itemId, count);
    }

    @DeleteMapping("/{itemId}")
    public InventoryDto deleteItemFromInventory(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId) {
        return inventoryService.deleteItemFromInventory(roomId, characterId, itemId);
    }

    @PutMapping("/{itemId}/{count}")
    public InventoryDto deleteItemFromInventory(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId, @PathVariable Long count) {
        return inventoryService.addItemToInventory(roomId, characterId, itemId, count);
    }

    @PostMapping("/items/{itemId}/skills/{skillId}/use")
    public void useInventoryItemSkill(@PathVariable UUID roomId, @PathVariable UUID characterId, @PathVariable UUID itemId, @PathVariable UUID skillId) {
        inventoryService.useSkill(roomId, characterId, itemId, skillId);
    }

    @PostMapping("/rest/{restType}")
    public ResponseEntity<Void> characterRest(
            @PathVariable UUID roomId,
            @PathVariable UUID characterId,
            @PathVariable ChargesRefillEnum restType
    ) {
        inventoryService.characterRest(roomId, characterId, restType);
        return ResponseEntity.ok().build();
    }
}
