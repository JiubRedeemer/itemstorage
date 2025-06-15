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

    public InventoryDto getInventoryByRoomIdAndCharacterId(UUID roomId, UUID characterId) {
        return inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseGet(() -> {
                    final InventoryDto inventoryDto = new InventoryDto();
                    inventoryDto.setId(UUID.randomUUID());
                    inventoryDto.setCharacterId(characterId);
                    inventoryDto.setRoomId(roomId);
                    inventoryDto.setTotalWeight(0L);
                    inventoryRepository.create(inventoryDto);
                    return inventoryDto;
                });
    }

    public InventoryDto equipItemByCharacterIdAndItemId(UUID roomId, UUID characterId, UUID itemId) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        final InventoryItemDto inventoryItemDto = inventoryDto.getItems().stream().filter(item -> item.getItemId().equals(itemId)).findAny().orElseThrow();
        inventoryRepository.changeInUseStatus(itemId, !inventoryItemDto.getInUse());
        return inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
    }

    public InventoryDto changeItemCount(UUID roomId, UUID characterId, UUID itemId, Long count) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        inventoryDto.getItems().stream().filter(item -> item.getId().equals(itemId)).findAny().orElseThrow();
        inventoryRepository.changeItemCount(itemId, count);
        return inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
    }

    public InventoryDto deleteItemFromInventory(UUID roomId, UUID characterId, UUID itemId) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        inventoryDto.getItems().stream().filter(item -> item.getId().equals(itemId)).findAny().orElseThrow();
        inventoryRepository.deleteItemFromInventory(itemId);
        return inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
    }

    public InventoryItemDto getItemByCharacterIdAndItemId(UUID roomId, UUID characterId, UUID itemId) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        return inventoryDto.getItems().stream().filter(item -> item.getId().equals(itemId)).findAny().orElseThrow();
    }

    public InventoryDto addItemToInventory(UUID roomId, UUID characterId, UUID itemId, Long count) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        inventoryDto.getItems().stream().filter(item -> item.getId().equals(itemId)).findAny().ifPresent(item -> {
            throw new IllegalStateException("Item already exists in inventory");
        });
        inventoryRepository.addItemToInventory(inventoryDto.getId(), itemId, count);
        return inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
    }
}
