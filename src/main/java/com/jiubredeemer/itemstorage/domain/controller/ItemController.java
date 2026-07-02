package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemTagDto;
import com.jiubredeemer.itemstorage.domain.model.item.SearchItemParams;
import com.jiubredeemer.itemstorage.domain.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
                searchItemParams.getLimit(),
                searchItemParams.getRuleType(),
                searchItemParams.getType(),
                searchItemParams.getSubtype(),
                searchItemParams.getRarity(),
                searchItemParams.getTags(),
                searchItemParams.getCustomization(),
                searchItemParams.getHasSkills());
    }

    @PostMapping("/search/owned")
    public List<ItemDto> searchByNameRoomAndCommunityItemsOwnedUsers(@PathVariable UUID roomId,
                                                           @PathVariable UUID userId,
                                                           @RequestBody SearchItemParams searchItemParams) {
        return itemService.searchByNameRoomAndCommunityItemsOwnedUsers(searchItemParams.getSearchQuery(),
                roomId,
                userId,
                searchItemParams.getLastSeenCreatedAt(),
                searchItemParams.getLastSeenId(),
                searchItemParams.getLimit(),
                searchItemParams.getType(),
                searchItemParams.getSubtype(),
                searchItemParams.getRarity(),
                searchItemParams.getTags(),
                searchItemParams.getCustomization(),
                searchItemParams.getHasSkills());
    }

    @GetMapping("/tags")
    public List<ItemTagDto> getTagsForRoom(@PathVariable UUID roomId) {
        return itemService.getTagsForRoom(roomId);
    }

    @PostMapping("/tags")
    public ItemTagDto createTag(@PathVariable UUID roomId, @RequestBody Map<String, String> body) {
        return itemService.createTag(body.get("name"), roomId);
    }

    @PatchMapping("/tags/{tagId}")
    public ItemTagDto updateTagDescription(@PathVariable UUID tagId, @RequestBody Map<String, String> body) {
        return itemService.updateTagDescription(tagId, body.get("description"));
    }

    @GetMapping("/{itemId}")
    public ItemDto getItem(@PathVariable UUID itemId) {
        return itemService.getItem(itemId);
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
