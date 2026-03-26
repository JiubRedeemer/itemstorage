package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.InventoryRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemRepository;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;

    public List<ItemDto> fetchAllItems() {
        return itemRepository.findAll();
    }

    public List<ItemDto> searchByNameRoomAndCommunityItems(String searchQuery,
                                                           UUID roomId,
                                                           UUID userId,
                                                           Timestamp lastSeenCreatedAt,
                                                           UUID lastSeenId,
                                                           int limit) {
        return itemRepository.searchByNameRoomAndCommunityItems(searchQuery, roomId, userId, lastSeenCreatedAt, lastSeenId, limit);
    }
    public List<ItemDto> searchByNameRoomAndCommunityItemsOwnedUsers(String searchQuery,
                                                           UUID roomId,
                                                           UUID userId,
                                                           Timestamp lastSeenCreatedAt,
                                                           UUID lastSeenId,
                                                           int limit) {
        return itemRepository.searchByNameRoomAndCommunityItemsOwnedByUser(searchQuery, roomId, userId, lastSeenCreatedAt, lastSeenId, limit);
    }

    public ItemDto addItem(UUID roomId, UUID userId, ItemDto itemDto) {
        itemDto.setRoomId(roomId);
        itemDto.setCreatorId(userId);
        itemRepository.create(itemDto);
        itemDto.getSkills().forEach(skill -> skill.setItemId(itemDto.getId()));
        itemRepository.createSkills(itemDto.getSkills());
        return itemRepository.findById(itemDto.getId()).orElseThrow();
    }

    public void deleteItem(UUID roomId, UUID userId, UUID itemId) {
        ItemDto item = itemRepository.findById(itemId).orElseThrow();
        if(!userId.equals(item.getCreatorId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        inventoryRepository.deleteItemFromInventoryByItemId(itemId);
        itemRepository.deleteById(itemId);
    }
}
