package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.bundle.ItemBundleDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.service.BundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/items/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    /**
     * Видимые пользователю бандлы: системные + публичные + собственные, с поиском по названию.
     */
    @GetMapping("/visible/{userId}")
    public List<ItemBundleDto> getVisibleBundles(@PathVariable UUID userId,
                                                 @RequestParam(required = false) String search) {
        return bundleService.getVisibleBundles(userId, search);
    }

    @GetMapping("/own/{userId}")
    public List<ItemBundleDto> getOwnBundles(@PathVariable UUID userId) {
        return bundleService.getOwnBundles(userId);
    }

    @GetMapping("/{bundleId}")
    public ItemBundleDto getBundle(@PathVariable UUID bundleId) {
        return bundleService.getBundle(bundleId);
    }

    @PostMapping("/{userId}")
    public ItemBundleDto createBundle(@PathVariable UUID userId, @RequestBody ItemBundleDto dto) {
        return bundleService.createBundle(userId, dto);
    }

    @PutMapping("/{bundleId}/{userId}")
    public ItemBundleDto updateBundle(@PathVariable UUID bundleId, @PathVariable UUID userId,
                                      @RequestBody ItemBundleDto dto) {
        return bundleService.updateBundle(bundleId, userId, dto);
    }

    @DeleteMapping("/{bundleId}/{userId}")
    public void deleteBundle(@PathVariable UUID bundleId, @PathVariable UUID userId) {
        bundleService.deleteBundle(bundleId, userId);
    }

    @GetMapping("/{bundleId}/items")
    public List<ItemDto> getBundleItems(@PathVariable UUID bundleId) {
        return bundleService.getBundleItems(bundleId);
    }

    @PutMapping("/{bundleId}/items/{userId}")
    public ItemDto saveBundleItem(@PathVariable UUID bundleId, @PathVariable UUID userId, @RequestBody ItemDto itemDto) {
        return bundleService.saveBundleItem(bundleId, userId, itemDto);
    }

    @DeleteMapping("/items/{itemId}")
    public void deleteBundleItem(@PathVariable UUID itemId) {
        bundleService.deleteBundleItem(itemId);
    }

    /**
     * Импорт копий уже созданных пользователем предметов в бандл.
     */
    @PostMapping("/{bundleId}/import/{userId}")
    public void importItems(@PathVariable UUID bundleId, @PathVariable UUID userId, @RequestBody List<UUID> itemIds) {
        bundleService.importItems(bundleId, userId, itemIds);
    }

    /**
     * Все предметы, созданные пользователем (для выбора при импорте).
     */
    @GetMapping("/creator-items/{userId}")
    public List<ItemDto> getItemsCreatedByUser(@PathVariable UUID userId,
                                               @RequestParam(required = false) String search) {
        return bundleService.getItemsCreatedByUser(userId, search);
    }

    @PostMapping("/{bundleId}/purchase/{userId}")
    public ItemBundleDto recordPurchase(@PathVariable UUID bundleId, @PathVariable UUID userId) {
        return bundleService.recordPurchase(userId, bundleId);
    }

    @GetMapping("/rooms/{roomId}/{userId}")
    public List<ItemBundleDto> getBundlesForRoom(@PathVariable UUID roomId, @PathVariable UUID userId,
                                                 @RequestParam(required = false) String search) {
        return bundleService.getBundlesForRoom(roomId, userId, search);
    }

    @PutMapping("/rooms/{roomId}/{userId}/{bundleId}")
    public void enableBundleForRoom(@PathVariable UUID roomId, @PathVariable UUID userId, @PathVariable UUID bundleId) {
        bundleService.enableBundleForRoom(roomId, userId, bundleId);
    }

    @DeleteMapping("/rooms/{roomId}/{bundleId}")
    public void disableBundleForRoom(@PathVariable UUID roomId, @PathVariable UUID bundleId) {
        bundleService.disableBundleForRoom(roomId, bundleId);
    }
}
