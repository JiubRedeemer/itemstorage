package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ShopItemRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ShopRepository;
import com.jiubredeemer.itemstorage.domain.model.shop.ShopDto;
import com.jiubredeemer.itemstorage.domain.model.shop.ShopItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopItemRepository shopItemRepository;
    private final ItemRepository itemRepository;

    public List<ShopDto> getShopsForRoom(UUID roomId) {
        return shopRepository.findByRoom(roomId);
    }

    public ShopDto getShop(UUID id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
    }

    public ShopDto createShop(UUID roomId, UUID userId, ShopDto dto) {
        return shopRepository.create(roomId, userId, dto);
    }

    public ShopDto updateShop(UUID id, ShopDto dto) {
        getShop(id);
        return shopRepository.update(id, dto);
    }

    public void deleteShop(UUID id) {
        getShop(id);
        shopRepository.deleteById(id);
    }

    /**
     * Витрина магазина: позиции с ценами + разрешённый предмет каталога/комнаты.
     */
    public List<ShopItemDto> getShopItems(UUID shopId) {
        getShop(shopId);
        List<ShopItemDto> items = shopItemRepository.findByShopId(shopId);
        items.forEach(si -> itemRepository.findById(si.getItemId()).ifPresent(si::setItem));
        return items;
    }

    public ShopItemDto saveShopItem(UUID shopId, ShopItemDto dto) {
        getShop(shopId);
        if (dto.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required");
        }
        ShopItemDto saved = shopItemRepository.upsert(shopId, dto);
        itemRepository.findById(saved.getItemId()).ifPresent(saved::setItem);
        return saved;
    }

    public void deleteShopItem(UUID shopItemId) {
        shopItemRepository.deleteById(shopItemId);
    }
}
