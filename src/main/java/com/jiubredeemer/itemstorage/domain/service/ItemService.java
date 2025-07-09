package com.jiubredeemer.itemstorage.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemRepository;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    public List<ItemDto> fetchAllItems() {
        return itemRepository.findAll();
    }

    public List<ItemDto> searchByNameRoomAndCommunityItems(String searchQuery,
                                                           UUID roomId,
                                                           UUID userId,
                                                           LocalDateTime lastSeenCreatedAt,
                                                           UUID lastSeenId,
                                                           int limit) {
        return itemRepository.searchByNameRoomAndCommunityItems(searchQuery, roomId, userId, lastSeenCreatedAt, lastSeenId, limit);
    }

    public ItemDto addItem(UUID roomId, UUID userId, ItemDto itemDto) {
        itemDto.setRoomId(roomId);
        itemDto.setCreatorId(userId);
        try {
            itemRepository.create(itemDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return itemRepository.findById(itemDto.getId()).orElseThrow();
    }
}
