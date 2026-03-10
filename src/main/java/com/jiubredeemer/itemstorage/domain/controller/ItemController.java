package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.SearchItemParams;
import com.jiubredeemer.itemstorage.domain.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items/{roomId}/{userId}")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> fetchAllItems() {
        return itemService.fetchAllItems();
    }

    @PostMapping("/search")
    public List<ItemDto> searchByNameRoomAndCommunityItems(@PathVariable UUID roomId,
                                                           @PathVariable UUID userId,
                                                           @RequestBody SearchItemParams searchItemParams) {
        return itemService.searchByNameRoomAndCommunityItems(searchItemParams.getSearchQuery(),
                roomId,
                userId,
                searchItemParams.getLastSeenCreatedAt(),
                searchItemParams.getLastSeenId(),
                searchItemParams.getLimit());
    }

    @PutMapping()
    public ItemDto addItem(@RequestBody ItemDto itemDto, @PathVariable UUID roomId, @PathVariable UUID userId) {
        return itemService.addItem(roomId, userId, itemDto);
    }

    @DeleteMapping("/{itemId}")
    public void deleteItem(@PathVariable UUID itemId, @PathVariable UUID roomId, @PathVariable UUID userId) {
        itemService.deleteItem(roomId, userId, itemId);
    }
}
