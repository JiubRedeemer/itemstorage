package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.InventoryRepository;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryDto;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryItemDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryDto getInventoryByRoomIdAndCharacterId(UUID characterId) {
        return inventoryRepository.findInventoryByCharacterIdFull(characterId)
                .orElseThrow();
    }

    public InventoryDto equipItemByCharacterIdAndItemId(UUID characterId, UUID itemId) {
        final InventoryItemDto inventoryItemDto = inventoryRepository.findInventoryItemById(itemId)
                .orElseThrow();
        inventoryRepository.changeInUseStatus(itemId, !inventoryItemDto.getInUse());
        return inventoryRepository.findInventoryByCharacterIdFull(characterId)
                .orElseThrow();
    }

    public InventoryDto changeItemCount(UUID characterId, UUID itemId, Long count) {
        inventoryRepository.changeCount(itemId, count);
        return inventoryRepository.findInventoryByCharacterIdFull(characterId)
                .orElseThrow();
    }
}
