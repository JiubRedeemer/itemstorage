package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.dal.entity.tables.records.InventoryRecord;
import com.jiubredeemer.itemstorage.domain.model.common.ItemTypeEnum;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryDto;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemStatsDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.*;

@Repository
@RequiredArgsConstructor
public class InventoryRepository {
    private final DSLContext dsl;
    private final ItemRepository itemRepository;

    public InventoryDto create(InventoryDto inventoryDto) {
        dsl.insertInto(INVENTORY)
                .set(INVENTORY.ID, inventoryDto.getId())
                .set(INVENTORY.CHARACTER_ID, inventoryDto.getCharacterId())
                .set(INVENTORY.ROOM_ID, inventoryDto.getRoomId())
                .set(INVENTORY.TOTAL_WEIGHT, inventoryDto.getTotalWeight())
                .execute();
        return inventoryDto;
    }

    public Optional<InventoryDto> findInventoryById(UUID id) {
        return dsl.selectFrom(INVENTORY)
                .where(INVENTORY.ID.eq(id))
                .fetchOptional()
                .map(inventoryRecord -> inventoryRecord.into(InventoryDto.class));
    }

    public Optional<InventoryDto> findInventoryByIdFull(UUID id) {
        return dsl.selectFrom(INVENTORY)
                .where(INVENTORY.ID.eq(id))
                .fetchOptional()
                .map(this::mapInventoryRecordIntoFullDto);
    }

    public Optional<InventoryDto> findInventoryByCharacterIdFull(UUID roomId, UUID characterId) {
        return dsl.selectFrom(INVENTORY)
                .where(INVENTORY.CHARACTER_ID.eq(characterId)).and(INVENTORY.ROOM_ID.eq(roomId))
                .fetchOptional()
                .map(this::mapInventoryRecordIntoFullDto);
    }

    public List<InventoryItemDto> findAllInventoryItemsByInventoryId(UUID inventoryId) {
        final List<InventoryItemDto> inventoryItemDtos = dsl.selectFrom(INVENTORY_ITEM)
                .where(INVENTORY_ITEM.INVENTORY_ID.eq(inventoryId))
                .fetch()
                .map(inventoryItemRecord -> inventoryItemRecord.into(InventoryItemDto.class));
        final List<UUID> itemIds = inventoryItemDtos.stream().map(InventoryItemDto::getItemId).toList();
        final Map<UUID, ItemDto> itemsMap =
                itemRepository.findByIds(itemIds).stream().collect(Collectors.toMap(ItemDto::getId, itemDto -> itemDto));
        inventoryItemDtos.forEach(inventoryItemDto -> inventoryItemDto.setItem(itemsMap.get(inventoryItemDto.getItemId())));
        return inventoryItemDtos;
    }

    public Optional<InventoryItemDto> findInventoryItemById(UUID itemId) {
        final Optional<InventoryItemDto> inventoryItemDtos = dsl.selectFrom(INVENTORY_ITEM)
                .where(INVENTORY_ITEM.ID.eq(itemId))
                .fetchOptional()
                .map(inventoryItemRecord -> inventoryItemRecord.into(InventoryItemDto.class));
        final List<UUID> itemIds = inventoryItemDtos.stream().map(InventoryItemDto::getItemId).toList();
        final Map<UUID, ItemDto> itemsMap =
                itemRepository.findByIds(itemIds).stream().collect(Collectors.toMap(ItemDto::getId, itemDto -> itemDto));
        return inventoryItemDtos.map(inventoryItemDto -> {
            inventoryItemDto.setItem(itemsMap.get(inventoryItemDto.getItemId()));
            return inventoryItemDto;
        });
    }

    public Optional<InventoryItemDto> changeInUseStatus(UUID itemId, Boolean inUse) {
        dsl.update(INVENTORY_ITEM).set(INVENTORY_ITEM.IN_USE, inUse)
                .where(INVENTORY_ITEM.ID.eq(itemId))
                .execute();
        return findInventoryItemById(itemId);
    }

    public void deleteItemFromInventory(UUID itemId) {
        dsl.delete(INVENTORY_ITEM).where(INVENTORY_ITEM.ID.eq(itemId)).execute();
    }

    public Optional<InventoryItemDto> changeItemCount(UUID itemId, Long count) {
        dsl.update(INVENTORY_ITEM).set(INVENTORY_ITEM.COUNT, count)
                .where(INVENTORY_ITEM.ID.eq(itemId))
                .execute();
        return findInventoryItemById(itemId);
    }

    private InventoryDto mapInventoryRecordIntoFullDto(InventoryRecord inventoryRecord) {
        InventoryDto inventoryDto = inventoryRecord.into(InventoryDto.class);
        inventoryDto.setItems(findAllInventoryItemsByInventoryId(inventoryRecord.getId()));
        inventoryDto.setTotalWeight(inventoryDto.getItems().stream()
                .mapToLong(item -> item.getItem().getStats().getWeight() != null ?
                        item.getItem().getStats().getWeight() * item.getCount() : 0L)
                .sum());
        return inventoryDto;
    }

    public List<InventoryItemDto> findEquippedItemsByType(UUID inventoryId, ItemTypeEnum type) {
        return dsl.select(INVENTORY_ITEM)
                .from(INVENTORY_ITEM.join(ITEMS).on(INVENTORY_ITEM.ITEM_ID.eq(ITEMS.ID)))
                .where(INVENTORY_ITEM.INVENTORY_ID.eq(inventoryId)).and(INVENTORY_ITEM.IN_USE.eq(true)).and(ITEMS.TYPE.eq(type.name()))
                .fetch()
                .map(inventoryItemRecordRecord1 ->  inventoryItemRecordRecord1.into(InventoryItemDto.class));
    }

    public void addItemToInventory(UUID inventoryId, UUID itemId, Long count) {
        dsl.insertInto(INVENTORY_ITEM, INVENTORY_ITEM.ID, INVENTORY_ITEM.INVENTORY_ID, INVENTORY_ITEM.ITEM_ID, INVENTORY_ITEM.COUNT, INVENTORY_ITEM.IN_USE)
                .values(UUID.randomUUID(), inventoryId, itemId, count, false)
                .execute();
    }

    public List<ItemStatsDto> getEquippedItemStats(UUID roomId, UUID characterId) {
        Optional<InventoryDto> inventoryByCharacterIdFull = this.findInventoryByCharacterIdFull(roomId, characterId);
        if (inventoryByCharacterIdFull.isPresent()) {
            final List<InventoryItemDto> equippedItems = dsl.selectFrom(INVENTORY_ITEM)
                    .where(INVENTORY_ITEM.INVENTORY_ID.eq(inventoryByCharacterIdFull.get().getId()))
                    .and(INVENTORY_ITEM.IN_USE.eq(true))
                    .fetch()
                    .map(inventoryItemRecord -> inventoryItemRecord.into(InventoryItemDto.class));
            final List<UUID> equippedItemsIds = equippedItems.stream().map(InventoryItemDto::getItemId).toList();
            return dsl.selectFrom(ITEM_STATS)
                    .where(ITEM_STATS.ITEM_ID.in(equippedItemsIds))
                    .fetch()
                    .map(itemStatsRecord -> itemStatsRecord.into(ItemStatsDto.class));
        } else {
            throw new RuntimeException("Inventory not found");
        }
    }
}
