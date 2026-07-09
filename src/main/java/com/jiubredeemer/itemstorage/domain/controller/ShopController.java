package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.shop.ShopDto;
import com.jiubredeemer.itemstorage.domain.model.shop.ShopItemDto;
import com.jiubredeemer.itemstorage.domain.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/items/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/rooms/{roomId}")
    public List<ShopDto> getShopsForRoom(@PathVariable UUID roomId) {
        return shopService.getShopsForRoom(roomId);
    }

    @GetMapping("/{shopId}")
    public ShopDto getShop(@PathVariable UUID shopId) {
        return shopService.getShop(shopId);
    }

    @PostMapping("/{userId}")
    public ShopDto createShop(@PathVariable UUID userId, @RequestBody ShopDto dto) {
        return shopService.createShop(dto.getRoomId(), userId, dto);
    }

    @PutMapping("/{shopId}/{userId}")
    public ShopDto updateShop(@PathVariable UUID shopId, @PathVariable UUID userId, @RequestBody ShopDto dto) {
        return shopService.updateShop(shopId, dto);
    }

    @DeleteMapping("/{shopId}/{userId}")
    public void deleteShop(@PathVariable UUID shopId, @PathVariable UUID userId) {
        shopService.deleteShop(shopId);
    }

    @GetMapping("/{shopId}/items")
    public List<ShopItemDto> getShopItems(@PathVariable UUID shopId) {
        return shopService.getShopItems(shopId);
    }

    @PutMapping("/{shopId}/items/{userId}")
    public ShopItemDto saveShopItem(@PathVariable UUID shopId, @PathVariable UUID userId,
                                    @RequestBody ShopItemDto dto) {
        return shopService.saveShopItem(shopId, dto);
    }

    @DeleteMapping("/items/{shopItemId}")
    public void deleteShopItem(@PathVariable UUID shopItemId) {
        shopService.deleteShopItem(shopItemId);
    }
}
