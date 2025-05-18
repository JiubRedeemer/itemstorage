package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.SearchItemParams;
import com.jiubredeemer.itemstorage.domain.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> fetchAllItems() {
        return itemService.fetchAllItems();
    }

    @PostMapping("{roomId}/search")
    public List<ItemDto> searchByNameRoomAndCommunityItems(@PathVariable UUID roomId,
                                                           @RequestBody SearchItemParams searchItemParams) {
        return itemService.searchByNameRoomAndCommunityItems(searchItemParams.getSearchQuery(),
                roomId,
                searchItemParams.getLastSeenCreatedAt(),
                searchItemParams.getLastSeenId(),
                searchItemParams.getLimit());
    }
}
