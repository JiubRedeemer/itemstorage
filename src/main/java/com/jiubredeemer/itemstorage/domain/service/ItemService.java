package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.InventoryRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemTagRepository;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemTagDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final ItemTagRepository itemTagRepository;

    public List<ItemDto> fetchAllItems() {
        return itemRepository.findAll();
    }

    public List<ItemDto> searchByNameRoomAndCommunityItems(String searchQuery,
                                                           UUID roomId,
                                                           UUID userId,
                                                           LocalDateTime lastSeenCreatedAt,
                                                           UUID lastSeenId,
                                                           int limit,
                                                           String ruleType,
                                                           String type,
                                                           String subtype,
                                                           String rarity,
                                                           List<String> tags,
                                                           Boolean customization,
                                                           Boolean hasSkills) {
        return itemRepository.searchByNameRoomAndCommunityItems(searchQuery, roomId, userId, lastSeenCreatedAt, lastSeenId, limit, ruleType, type, subtype, rarity, tags, customization, hasSkills);
    }

    public List<ItemDto> searchByNameRoomAndCommunityItemsOwnedUsers(String searchQuery,
                                                           UUID roomId,
                                                           UUID userId,
                                                           LocalDateTime lastSeenCreatedAt,
                                                           UUID lastSeenId,
                                                           int limit,
                                                           String type,
                                                           String subtype,
                                                           String rarity,
                                                           List<String> tags,
                                                           Boolean customization,
                                                           Boolean hasSkills) {
        return itemRepository.searchByNameRoomAndCommunityItemsOwnedByUser(searchQuery, roomId, userId, lastSeenCreatedAt, lastSeenId, limit, type, subtype, rarity, tags, customization, hasSkills);
    }

    public List<String> getDistinctTags() {
        return itemRepository.findDistinctTags();
    }

    public List<ItemTagDto> getTagsForRoom(UUID roomId) {
        return itemTagRepository.findByRoomId(roomId);
    }

    public ItemTagDto createTag(String name, UUID roomId) {
        return itemTagRepository.create(name, roomId);
    }

    public ItemTagDto updateTagDescription(UUID tagId, String description) {
        return itemTagRepository.update(tagId, description);
    }

    public ItemDto addItem(UUID roomId, UUID userId, ItemDto itemDto) {
        Optional<ItemDto> existing = itemRepository.findById(itemDto.getId());
        if (existing.isPresent()) {
            itemDto.setCreatorId(existing.get().getCreatorId());
            itemDto.setRoomId(existing.get().getRoomId());
            itemRepository.update(itemDto);
            itemRepository.deleteSkillsByItemId(itemDto.getId());
        } else {
            itemDto.setRoomId(roomId);
            itemDto.setCreatorId(userId);
            itemRepository.create(itemDto);
        }
        if (itemDto.getSkills() != null) {
            itemDto.getSkills().forEach(skill -> skill.setItemId(itemDto.getId()));
            itemRepository.createSkills(itemDto.getSkills());
        }
        return itemRepository.findById(itemDto.getId()).orElseThrow();
    }

    public ItemDto getItem(UUID itemId) {
        return itemRepository.findById(itemId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")
        );
    }

    public void deleteItem(UUID roomId, UUID userId, UUID itemId) {
        itemRepository.findById(itemId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")
        );
        inventoryRepository.deleteItemFromInventoryByItemId(itemId);
        itemRepository.deleteById(itemId);
    }
}
