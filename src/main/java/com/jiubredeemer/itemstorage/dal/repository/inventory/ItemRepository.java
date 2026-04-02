package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.dal.configuration.LicenseMode;
import com.jiubredeemer.itemstorage.dal.entity.tables.records.ItemSkillRecord;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemSkillDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ItemRepository {
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final LicenseMode licenseMode;

    public List<ItemDto> findAll() {
        List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
                .fetchInto(ItemDto.class);
        enrichSkills(itemDtos);
        return itemDtos;
    }

    public Optional<ItemDto> findById(UUID id) {
        Optional<ItemDto> itemDto = dsl.selectFrom(ITEMS)
                .where(ITEMS.ID.eq(id))
                .fetchOptionalInto(ItemDto.class);
        itemDto.ifPresent(itemDtoPresent -> enrichSkills(Collections.singletonList(itemDtoPresent)));
        return itemDto;
    }

    public List<ItemDto> findByIds(List<UUID> ids) {
        List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
                .where(ITEMS.ID.in(ids))
                .fetchInto(ItemDto.class);
        enrichSkills(itemDtos);
        return itemDtos;
    }

    public List<ItemDto> searchByNameRoomAndCommunityItems(
            String searchQuery,
            UUID roomId,
            UUID userId,
            Timestamp lastSeenCreatedAt,
            UUID lastSeenId,
            int limit
    ) {
        var condition = DSL.condition("1=1");

        if (searchQuery != null && !searchQuery.isBlank()) {
            var searchPattern = "%" + searchQuery + "%";
            condition = condition.and(
                    DSL.field("items.name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field("items.name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
        }

        if (roomId != null) {
            if (licenseMode.getCcBy4()) {
                condition = condition.and(ITEMS.ROOM_ID.eq(roomId));
            } else {
                condition = condition.and(ITEMS.ROOM_ID.eq(roomId).or(ITEMS.ROOM_ID.isNull()));
            }
        } else {
            condition = condition.and("1=0");
        }

        if (userId != null) {
            condition = condition.and(
                    (ITEMS.CREATOR_ID.eq(userId).and(ITEMS.VISIBLE_FOR_PLAYERS.eq(false)))
                            .or(ITEMS.VISIBLE_FOR_PLAYERS.eq(true).or(ITEMS.VISIBLE_FOR_PLAYERS.isNull()))
            );
        }

        // Добавляем seek-пагинацию по created_at + id
        if (lastSeenCreatedAt != null && lastSeenId != null) {
            condition = condition.and(
                    DSL.row(ITEMS.CREATED_AT, ITEMS.ID)
                            .gt(DSL.row(lastSeenCreatedAt.toLocalDateTime(), lastSeenId))
            );
        }

        final List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
                .where(condition)
                .orderBy(ITEMS.CREATED_AT.asc(), ITEMS.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);

        enrichSkills(itemDtos);

        return itemDtos;
    }

    public List<ItemDto> searchByNameRoomAndCommunityItemsOwnedByUser(
            String searchQuery,
            UUID roomId,
            UUID userId,
            Timestamp lastSeenCreatedAt,
            UUID lastSeenId,
            int limit
    ) {
        var condition = DSL.condition("1=1");

        if (searchQuery != null && !searchQuery.isBlank()) {
            var searchPattern = "%" + searchQuery + "%";
            condition = condition.and(
                    DSL.field("items.name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field("items.name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
        }

        if (roomId != null) {
            condition = condition.and(ITEMS.ROOM_ID.eq(roomId).or(ITEMS.ROOM_ID.isNull()));
        } else {
            condition = condition.and(ITEMS.ROOM_ID.isNull());
        }

        condition = condition.and(ITEMS.CREATOR_ID.eq(userId));


        // Добавляем seek-пагинацию по created_at + id
        if (lastSeenCreatedAt != null && lastSeenId != null) {
            condition = condition.and(
                    DSL.row(ITEMS.CREATED_AT, ITEMS.ID)
                            .gt(DSL.row(lastSeenCreatedAt.toLocalDateTime(), lastSeenId))
            );
        }

        final List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
                .where(condition)
                .orderBy(ITEMS.CREATED_AT.asc(), ITEMS.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);

        enrichSkills(itemDtos);

        return itemDtos;
    }

    public List<ItemSkillDto> findSkillsForItems(List<UUID> itemIds) {
        return dsl.selectFrom(ITEM_SKILL)
                .where(ITEM_SKILL.ITEM_ID.in(itemIds))
                .fetchInto(ItemSkillDto.class);
    }

    public ItemSkillDto findSkillById(UUID id) {
        return dsl.selectFrom(ITEM_SKILL)
                .where(ITEM_SKILL.ID.eq(id))
                .fetchOneInto(ItemSkillDto.class);
    }

    public void create(ItemDto itemDto) {
        dsl.insertInto(ITEMS)
                .set(ITEMS.ID, itemDto.getId())
                .set(ITEMS.ROOM_ID, itemDto.getRoomId())
                .set(ITEMS.CREATOR_ID, itemDto.getCreatorId())
                .set(ITEMS.CREATED_AT, LocalDateTime.now())
                .set(ITEMS.VISIBLE_FOR_PLAYERS, itemDto.getVisibleForPlayers())
                .set(ITEMS.CREATOR, itemDto.getCreator())
                .set(ITEMS.CUSTOMIZATION, itemDto.getCustomization())
                .set(ITEMS.DESCRIPTION, itemDto.getDescription())
                .set(ITEMS.IMG_URL, itemDto.getImgUrl())
                .set(ITEMS.NAME, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getName())))
                .set(ITEMS.TYPE, itemDto.getType().name())
                .set(ITEMS.SUBTYPE, itemDto.getSubtype() != null ? itemDto.getSubtype().name() : null)
                .set(ITEMS.STATS, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getStats())))
                .set(ITEMS.RARITY, itemDto.getRarity().name())
                .execute();
    }

    public void createSkills(List<ItemSkillDto> itemSkillDtos) {
        List<ItemSkillRecord> recordsToSave = itemSkillDtos.stream().map(itemSkillDto -> {
            JSONB name;
            name = JSONB.valueOf(objectMapper.writeValueAsString(itemSkillDto.getName()));

            return new ItemSkillRecord(
                    UUID.randomUUID(),
                    itemSkillDto.getItemId(),
                    name,
                    itemSkillDto.getCastTime(),
                    itemSkillDto.getDistance(),
                    itemSkillDto.getDescription(),
                    itemSkillDto.getShortDescription(),
                    itemSkillDto.getCharges(),
                    itemSkillDto.getChargesRefill().name(),
                    itemSkillDto.getImgUrl());
        }).toList();
        dsl.batchInsert(recordsToSave).execute();
    }

    private void enrichSkills(List<ItemDto> itemDtos) {
        List<ItemSkillDto> skills = findSkillsForItems(itemDtos.stream().map(ItemDto::getId).collect(Collectors.toList()));
        itemDtos.forEach(itemDto -> itemDto.setSkills(skills
                .stream()
                .filter(skillDto -> skillDto.getItemId().equals(itemDto.getId()))
                .collect(Collectors.toList())));
    }


    public void deleteById(UUID itemId) {
        dsl.transaction(transaction -> {
            List<InventoryItemDto> inventoryItemDtos =
                    DSL.using(transaction).selectFrom(INVENTORY_ITEM)
                            .where(INVENTORY_ITEM.ITEM_ID.eq(itemId))
                            .fetchInto(InventoryItemDto.class);
            DSL.using(transaction).delete(INVENTORY_ITEM_SKILL).where(INVENTORY_ITEM_SKILL.INVENTORY_ITEM_ID
                            .in(inventoryItemDtos.stream().map(InventoryItemDto::getId).toList()))
                    .execute();
            DSL.using(transaction).deleteFrom(INVENTORY_ITEM).where(INVENTORY_ITEM.ITEM_ID.eq(itemId)).execute();
            DSL.using(transaction).deleteFrom(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(itemId)).execute();
            DSL.using(transaction).deleteFrom(ITEMS).where(ITEMS.ID.eq(itemId)).execute();
        });
    }
}
