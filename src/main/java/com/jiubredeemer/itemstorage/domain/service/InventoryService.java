package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.InventoryRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemRepository;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryDto;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ChargesRefillEnum;
import com.jiubredeemer.itemstorage.domain.model.item.InventoryItemSkillDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemSkillDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ItemRepository itemRepository;

    public InventoryService(InventoryRepository inventoryRepository, ItemRepository itemRepository) {
        this.inventoryRepository = inventoryRepository;
        this.itemRepository = itemRepository;
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
        final InventoryItemDto inventoryItemDto = inventoryDto.getItems().stream().filter(item -> item.getId().equals(itemId)).findAny().orElseThrow();
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
        final ItemDto itemDto = itemRepository.findById(itemId).orElseThrow();
        final InventoryItemDto inventoryItemDto = inventoryRepository.addItemToInventory(inventoryDto.getId(), itemId, count);
        createInventoryItemSkills(itemDto.getSkills(), inventoryItemDto.getId());
        return inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
    }

    private List<InventoryItemSkillDto> createInventoryItemSkills(List<ItemSkillDto> skills, UUID inventoryItemId) {
        return inventoryRepository.createInventoryItemSkills(skills, inventoryItemId);
    }

    public void useSkill(UUID roomId, UUID characterId, UUID itemId, UUID skillId) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        inventoryDto.getItems().stream().filter(item -> item.getId().equals(itemId)).findAny().orElseThrow();
        inventoryRepository.useSkill(itemId, skillId);
    }

    public void characterRest(UUID roomId, UUID characterId, ChargesRefillEnum restType) {
        final InventoryDto inventoryDto = inventoryRepository.findInventoryByCharacterIdFull(roomId, characterId)
                .orElseThrow();
        inventoryDto.getItems().forEach(item -> {
            if (item.getSkills() == null) return;
            item.getSkills().forEach(skill -> {
                if (restType.equals(skill.getSkill().getChargesRefill()) ||
                        (ChargesRefillEnum.LONG_REST.equals(restType) && ChargesRefillEnum.SHORT_REST.equals(skill.getSkill().getChargesRefill()))) {
                    skill.setCurrentCharges(skill.getSkill().getCharges());
                    inventoryRepository.updateInventoryItemSkill(skill);
                }
            });
        });
    }
}
