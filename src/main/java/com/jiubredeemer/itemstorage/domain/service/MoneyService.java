package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.InventoryRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.MoneyRepository;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryDto;
import com.jiubredeemer.itemstorage.domain.model.money.MoneyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoneyService {
    private final InventoryRepository inventoryRepository;
    private final MoneyRepository moneyRepository;

    public MoneyDto findByCharacterId(UUID characterId) {
        final InventoryDto inventoryByCharacterIdFull = inventoryRepository.findInventoryByCharacterIdFull(characterId).orElseThrow();
        return moneyRepository.findByInventoryId(inventoryByCharacterIdFull.getId())
                .orElseGet(() -> moneyRepository.create(inventoryByCharacterIdFull.getId(), new MoneyDto()).orElseThrow());
    }

    public MoneyDto changeMoneyCount(UUID characterId, MoneyDto moneyDto) {
        final InventoryDto inventoryByCharacterIdFull = inventoryRepository.findInventoryByCharacterIdFull(characterId).orElseThrow();
        return moneyRepository.changeCount(inventoryByCharacterIdFull.getId(), moneyDto)
                .orElseGet(() -> moneyRepository.create(inventoryByCharacterIdFull.getId(), moneyDto).orElseThrow());
    }
}
