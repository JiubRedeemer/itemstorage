package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.InventoryRepository;
import com.jiubredeemer.itemstorage.domain.model.bonus.EquippedItemsStatsResponse;
import com.jiubredeemer.itemstorage.domain.model.item.ItemStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemStatsService {
    private final InventoryRepository inventoryRepository;

    public EquippedItemsStatsResponse getEquippedItemsStats(UUID roomId, UUID characterId) {
        final List<ItemStatsDto> equippedItemStats = inventoryRepository.getEquippedItemStats(roomId, characterId);
        final EquippedItemsStatsResponse response = new EquippedItemsStatsResponse();
        response.setArmoryClassBonus(equippedItemStats.stream()
                .filter(stat -> stat.getArmoryClass() != null && !(stat.getArmoryClass().getValue().toString().equals("-1000") || stat.getArmoryClass().getValue().toString().equals("0")))
                .map(ItemStatsDto::getArmoryClass)
                .toList());
        response.setSpeedBonus(equippedItemStats.stream()
                .filter(stat -> stat.getSpeed() != null && !(stat.getSpeed().getValue().toString().equals("-1000") || stat.getSpeed().getValue().toString().equals("0")))
                .map(ItemStatsDto::getSpeed)
                .toList());
        response.setHpBonus(equippedItemStats.stream()
                .filter(stat -> stat.getHp() != null && !(stat.getHp().getValue().toString().equals("-1000") || stat.getHp().getValue().toString().equals("0")))
                .map(ItemStatsDto::getHp)
                .toList());
        return response;
    }
}
