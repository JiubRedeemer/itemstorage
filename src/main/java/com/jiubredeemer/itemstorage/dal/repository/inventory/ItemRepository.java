package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.dal.configuration.LicenseMode;
import com.jiubredeemer.itemstorage.dal.entity.tables.records.ItemSkillRecord;
import com.jiubredeemer.itemstorage.domain.model.inventory.InventoryItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemSkillDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemTagDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ItemRepository {
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final LicenseMode licenseMode;
    private final ItemTagRepository itemTagRepository;

    public List<ItemDto> findAll() {
        List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
                .where(ITEMS.CREATOR_ID.isNull())
                .fetchInto(ItemDto.class);
        enrichSkills(itemDtos);
        return itemDtos;
    }

    public Optional<ItemDto> findById(UUID id) {
        Optional<ItemDto> itemDto = dsl.selectFrom(ITEMS_USER)
                .where(ITEMS_USER.ID.eq(id))
                .fetchOptionalInto(ItemDto.class);
        if (itemDto.isEmpty()) {
            itemDto = dsl.selectFrom(ITEMS)
                    .where(ITEMS.ID.eq(id))
                    .fetchOptionalInto(ItemDto.class);
        }
        if (itemDto.isEmpty()) {
            itemDto = dsl.selectFrom(ITEMS_24)
                    .where(ITEMS_24.ID.eq(id))
                    .fetchOptionalInto(ItemDto.class);
        }
        itemDto.ifPresent(itemDtoPresent -> enrichSkills(Collections.singletonList(itemDtoPresent)));
        return itemDto;
    }

    public List<ItemDto> findByIds(List<UUID> ids) {
        List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
                .where(ITEMS.ID.in(ids))
                .and(ITEMS.CREATOR_ID.isNull())
                .fetchInto(ItemDto.class);
        itemDtos.addAll(dsl.selectFrom(ITEMS_24)
                .where(ITEMS_24.ID.in(ids))
                .and(ITEMS_24.CREATOR_ID.isNull())
                .fetchInto(ItemDto.class));
        itemDtos.addAll(dsl.selectFrom(ITEMS_USER)
                .where(ITEMS_USER.ID.in(ids))
                .fetchInto(ItemDto.class));
        enrichSkills(itemDtos);
        return itemDtos;
    }

    public List<ItemDto> searchByNameRoomAndCommunityItems(
            String searchQuery,
            UUID roomId,
            UUID userId,
            LocalDateTime lastSeenCreatedAt,
            UUID lastSeenId,
            int limit,
            String ruleType,
            String type,
            String subtype,
            String rarity,
            List<String> tags,
            Boolean customization,
            Boolean hasSkills) {
        Table<?> rulesItemsTable = getRulesItemsTable(ruleType);
        String rulesItemsTableName = rulesItemsTable.getName();
        var baseItemsCondition = DSL.field(rulesItemsTableName + ".creator_id").isNull();
        var userItemsCondition = DSL.condition("1=1");

        if (searchQuery != null && !searchQuery.isBlank()) {
            var searchPattern = "%" + searchQuery + "%";
            baseItemsCondition = baseItemsCondition.and(
                    DSL.field(rulesItemsTableName + ".name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field(rulesItemsTableName + ".name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
            userItemsCondition = userItemsCondition.and(
                    DSL.field("items_user.name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field("items_user.name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
        }

        if (roomId != null) {
            if (licenseMode.getCcBy4()) {
                userItemsCondition = userItemsCondition.and(ITEMS_USER.ROOM_ID.eq(roomId));
            } else {
                userItemsCondition = userItemsCondition.and(ITEMS_USER.ROOM_ID.eq(roomId).or(ITEMS_USER.ROOM_ID.isNull()));
            }
        } else {
            userItemsCondition = userItemsCondition.and("1=0");
        }

        if (userId != null) {
            userItemsCondition = userItemsCondition.and(
                    (ITEMS_USER.CREATOR_ID.eq(userId).and(ITEMS_USER.VISIBLE_FOR_PLAYERS.eq(false)))
                            .or(ITEMS_USER.VISIBLE_FOR_PLAYERS.eq(true).or(ITEMS_USER.VISIBLE_FOR_PLAYERS.isNull()))
            );
        }

        // Добавляем seek-пагинацию по created_at + id
        if (lastSeenCreatedAt != null && lastSeenId != null) {
            baseItemsCondition = baseItemsCondition.and(
                    DSL.row(DSL.field(rulesItemsTableName + ".created_at", LocalDateTime.class), DSL.field(rulesItemsTableName + ".id", UUID.class))
                            .gt(DSL.row(lastSeenCreatedAt, lastSeenId))
            );
            userItemsCondition = userItemsCondition.and(
                    DSL.row(ITEMS_USER.CREATED_AT, ITEMS_USER.ID)
                            .gt(DSL.row(lastSeenCreatedAt, lastSeenId))
            );
        }

        // Фильтрация по type
        if (type != null && !type.isBlank()) {
            baseItemsCondition = baseItemsCondition.and(DSL.field(rulesItemsTableName + ".type", String.class).eq(type));
            userItemsCondition = userItemsCondition.and(ITEMS_USER.TYPE.eq(type));
        }

        // Фильтрация по subtype
        if (subtype != null && !subtype.isBlank()) {
            baseItemsCondition = baseItemsCondition.and(DSL.field(rulesItemsTableName + ".subtype", String.class).eq(subtype));
            userItemsCondition = userItemsCondition.and(ITEMS_USER.SUBTYPE.eq(subtype));
        }

        // Фильтрация по rarity
        if (rarity != null && !rarity.isBlank()) {
            baseItemsCondition = baseItemsCondition.and(DSL.field(rulesItemsTableName + ".rarity", String.class).eq(rarity));
            userItemsCondition = userItemsCondition.and(ITEMS_USER.RARITY.eq(rarity));
        }

        // Фильтрация по customization
        if (customization != null) {
            baseItemsCondition = baseItemsCondition.and(DSL.field(rulesItemsTableName + ".customization", Boolean.class).eq(customization));
            userItemsCondition = userItemsCondition.and(ITEMS_USER.CUSTOMIZATION.eq(customization));
        }

        // Фильтрация по hasSkills (AND: предмет должен иметь хотя бы один скилл)
        if (hasSkills != null) {
            var baseSkillsExists = DSL.exists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(DSL.field(rulesItemsTableName + ".id", UUID.class))));
            var userSkillsExists = DSL.exists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(ITEMS_USER.ID)));
            if (hasSkills) {
                baseItemsCondition = baseItemsCondition.and(baseSkillsExists);
                userItemsCondition = userItemsCondition.and(userSkillsExists);
            } else {
                baseItemsCondition = baseItemsCondition.and(DSL.notExists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(DSL.field(rulesItemsTableName + ".id", UUID.class)))));
                userItemsCondition = userItemsCondition.and(DSL.notExists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(ITEMS_USER.ID))));
            }
        }

        // Фильтрация по тегам (AND: предмет должен иметь ВСЕ теги)
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                baseItemsCondition = baseItemsCondition.and(
                        DSL.exists(DSL.select(DSL.val(1))
                                .from(DSL.table("itemstorage.item_tag_relation").as("itr_f"))
                                .join(DSL.table("itemstorage.item_tag").as("it_f"))
                                .on(DSL.field("it_f.id").eq(DSL.field("itr_f.tag_id")))
                                .where(DSL.field("itr_f.item_id").eq(DSL.field(rulesItemsTableName + ".id"))
                                        .and(DSL.field("it_f.name", String.class).eq(tag))))
                );
                userItemsCondition = userItemsCondition.and(
                        DSL.exists(DSL.select(DSL.val(1))
                                .from(DSL.table("itemstorage.item_tag_relation").as("itr_f"))
                                .join(DSL.table("itemstorage.item_tag").as("it_f"))
                                .on(DSL.field("it_f.id").eq(DSL.field("itr_f.tag_id")))
                                .where(DSL.field("itr_f.item_id").eq(ITEMS_USER.ID)
                                        .and(DSL.field("it_f.name", String.class).eq(tag))))
                );
            }
        }

        final List<ItemDto> baseItems = dsl.selectFrom(rulesItemsTable)
                .where(baseItemsCondition)
                .orderBy(DSL.field(rulesItemsTableName + ".created_at").asc(), DSL.field(rulesItemsTableName + ".id").asc())
                .limit(limit)
                .fetchInto(ItemDto.class);
        final List<ItemDto> userItems = dsl.selectFrom(ITEMS_USER)
                .where(userItemsCondition)
                .orderBy(ITEMS_USER.CREATED_AT.asc(), ITEMS_USER.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);
        final List<ItemDto> itemDtos = Stream.concat(baseItems.stream(), userItems.stream())
                .sorted((left, right) -> {
                    int compareCreatedAt = left.getCreatedAt().compareTo(right.getCreatedAt());
                    return compareCreatedAt != 0 ? compareCreatedAt : left.getId().compareTo(right.getId());
                })
                .limit(limit)
                .toList();

        enrichSkills(itemDtos);

        return itemDtos;
    }

    public List<ItemDto> searchByNameRoomAndCommunityItemsOwnedByUser(
            String searchQuery,
            UUID roomId,
            UUID userId,
            LocalDateTime lastSeenCreatedAt,
            UUID lastSeenId,
            int limit,
            String type,
            String subtype,
            String rarity,
            List<String> tags,
            Boolean customization,
            Boolean hasSkills
    ) {
        var condition = DSL.condition("1=1");

        if (searchQuery != null && !searchQuery.isBlank()) {
            var searchPattern = "%" + searchQuery + "%";
            condition = condition.and(
                    DSL.field("items_user.name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field("items_user.name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
        }

        if (roomId != null) {
            condition = condition.and(ITEMS_USER.ROOM_ID.eq(roomId).or(ITEMS_USER.ROOM_ID.isNull()));
        } else {
            condition = condition.and(ITEMS_USER.ROOM_ID.isNull());
        }

        condition = condition.and(ITEMS_USER.CREATOR_ID.eq(userId));

        // Добавляем seek-пагинацию по created_at + id
        if (lastSeenCreatedAt != null && lastSeenId != null) {
            condition = condition.and(
                    DSL.row(ITEMS_USER.CREATED_AT, ITEMS_USER.ID)
                            .gt(DSL.row(lastSeenCreatedAt, lastSeenId))
            );
        }

        // Фильтрация по type
        if (type != null && !type.isBlank()) {
            condition = condition.and(ITEMS_USER.TYPE.eq(type));
        }

        // Фильтрация по subtype
        if (subtype != null && !subtype.isBlank()) {
            condition = condition.and(ITEMS_USER.SUBTYPE.eq(subtype));
        }

        // Фильтрация по rarity
        if (rarity != null && !rarity.isBlank()) {
            condition = condition.and(ITEMS_USER.RARITY.eq(rarity));
        }

        // Фильтрация по customization
        if (customization != null) {
            condition = condition.and(ITEMS_USER.CUSTOMIZATION.eq(customization));
        }

        // Фильтрация по hasSkills
        if (hasSkills != null) {
            var skillsExists = DSL.exists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(ITEMS_USER.ID)));
            if (hasSkills) {
                condition = condition.and(skillsExists);
            } else {
                condition = condition.and(DSL.notExists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(ITEMS_USER.ID))));
            }
        }

        // Фильтрация по тегам (AND: предмет должен иметь ВСЕ теги)
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                condition = condition.and(
                        DSL.exists(DSL.select(DSL.val(1))
                                .from(DSL.table("itemstorage.item_tag_relation").as("itr_f"))
                                .join(DSL.table("itemstorage.item_tag").as("it_f"))
                                .on(DSL.field("it_f.id").eq(DSL.field("itr_f.tag_id")))
                                .where(DSL.field("itr_f.item_id").eq(ITEMS_USER.ID)
                                        .and(DSL.field("it_f.name", String.class).eq(tag))))
                );
            }
        }

        final List<ItemDto> itemDtos = dsl.selectFrom(ITEMS_USER)
                .where(condition)
                .orderBy(ITEMS_USER.CREATED_AT.asc(), ITEMS_USER.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);

        enrichSkills(itemDtos);

        return itemDtos;
    }

    public List<String> findDistinctTags() {
        return dsl.selectDistinct(DSL.field("name", String.class))
                .from(DSL.table("itemstorage.item_tag"))
                .where(DSL.field("room_id").isNull())
                .orderBy(DSL.field("name"))
                .fetch(DSL.field("name", String.class));
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
        List<UUID> tagIds = itemDto.getStats() != null ? itemDto.getStats().getTagIds() : null;
        if (itemDto.getStats() != null) {
            itemDto.getStats().setTags(null);
            itemDto.getStats().setTagIds(null);
        }
        dsl.insertInto(ITEMS_USER)
                .set(ITEMS_USER.ID, itemDto.getId())
                .set(ITEMS_USER.ROOM_ID, itemDto.getRoomId())
                .set(ITEMS_USER.CREATOR_ID, itemDto.getCreatorId())
                .set(ITEMS_USER.CREATED_AT, LocalDateTime.now())
                .set(ITEMS_USER.VISIBLE_FOR_PLAYERS, itemDto.getVisibleForPlayers())
                .set(ITEMS_USER.CREATOR, itemDto.getCreator())
                .set(ITEMS_USER.CUSTOMIZATION, itemDto.getCustomization())
                .set(ITEMS_USER.DESCRIPTION, itemDto.getDescription())
                .set(ITEMS_USER.IMG_URL, itemDto.getImgUrl())
                .set(ITEMS_USER.NAME, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getName())))
                .set(ITEMS_USER.TYPE, itemDto.getType().name())
                .set(ITEMS_USER.SUBTYPE, itemDto.getSubtype() != null ? itemDto.getSubtype().name() : null)
                .set(ITEMS_USER.STATS, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getStats())))
                .set(ITEMS_USER.RARITY, itemDto.getRarity().name())
                .execute();
        createTagRelations(itemDto.getId(), tagIds);
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
        enrichTags(itemDtos);
    }

    private void enrichTags(List<ItemDto> itemDtos) {
        List<UUID> ids = itemDtos.stream().map(ItemDto::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<UUID, List<ItemTagDto>> tagsByItemId = itemTagRepository.findDistinctByItemIds(ids);
        itemDtos.forEach(dto -> {
            if (dto.getStats() != null) {
                List<ItemTagDto> tags = tagsByItemId.get(dto.getId());
                if (tags != null && !tags.isEmpty()) {
                    dto.getStats().setTags(tags.stream().map(ItemTagDto::getName).collect(Collectors.toList()));
                    dto.getStats().setTagIds(tags.stream().map(ItemTagDto::getId).collect(Collectors.toList()));
                } else {
                    dto.getStats().setTags(null);
                    dto.getStats().setTagIds(null);
                }
            }
        });
    }

    public void update(ItemDto itemDto) {
        List<UUID> tagIds = itemDto.getStats() != null ? itemDto.getStats().getTagIds() : null;
        if (itemDto.getStats() != null) {
            itemDto.getStats().setTags(null);
            itemDto.getStats().setTagIds(null);
        }
        dsl.update(ITEMS_USER)
                .set(ITEMS_USER.VISIBLE_FOR_PLAYERS, itemDto.getVisibleForPlayers())
                .set(ITEMS_USER.CREATOR, itemDto.getCreator())
                .set(ITEMS_USER.CUSTOMIZATION, itemDto.getCustomization())
                .set(ITEMS_USER.DESCRIPTION, itemDto.getDescription())
                .set(ITEMS_USER.IMG_URL, itemDto.getImgUrl())
                .set(ITEMS_USER.NAME, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getName())))
                .set(ITEMS_USER.TYPE, itemDto.getType().name())
                .set(ITEMS_USER.SUBTYPE, itemDto.getSubtype() != null ? itemDto.getSubtype().name() : null)
                .set(ITEMS_USER.STATS, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getStats())))
                .set(ITEMS_USER.RARITY, itemDto.getRarity().name())
                .where(ITEMS_USER.ID.eq(itemDto.getId()))
                .execute();
        itemTagRepository.deleteTagRelationsByItemId(itemDto.getId());
        createTagRelations(itemDto.getId(), tagIds);
    }

    public void deleteSkillsByItemId(UUID itemId) {
        dsl.deleteFrom(ITEM_SKILL)
                .where(ITEM_SKILL.ITEM_ID.eq(itemId))
                .execute();
    }

    public void createTagRelations(UUID itemId, List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        tagIds.forEach(tagId -> itemTagRepository.addTagRelation(itemId, tagId));
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
            DSL.using(transaction).deleteFrom(DSL.table("itemstorage.item_tag_relation")).where(DSL.field("item_id", UUID.class).eq(itemId)).execute();
            DSL.using(transaction).deleteFrom(ITEMS_USER).where(ITEMS_USER.ID.eq(itemId)).execute();
        });
    }

    public void migrateLegacyCreatorItemsToItemsUser() {
        dsl.transaction(transaction -> {
            DSLContext tx = DSL.using(transaction);
            tx.execute("ALTER TABLE IF EXISTS inventory_item DROP CONSTRAINT IF EXISTS fkinventory_686023");
            tx.execute("ALTER TABLE IF EXISTS item_skill DROP CONSTRAINT IF EXISTS fkitem_skill469967");
            tx.execute("ALTER TABLE IF EXISTS item_stats DROP CONSTRAINT IF EXISTS item_stats_items_fk");
            tx.insertInto(ITEMS_USER,
                            ITEMS_USER.ID,
                            ITEMS_USER.NAME,
                            ITEMS_USER.TYPE,
                            ITEMS_USER.SUBTYPE,
                            ITEMS_USER.CUSTOMIZATION,
                            ITEMS_USER.RARITY,
                            ITEMS_USER.DESCRIPTION,
                            ITEMS_USER.STATS,
                            ITEMS_USER.CREATED_AT,
                            ITEMS_USER.ROOM_ID,
                            ITEMS_USER.CREATOR_ID,
                            ITEMS_USER.IMG_URL,
                            ITEMS_USER.VISIBLE_FOR_PLAYERS,
                            ITEMS_USER.CREATOR,
                            ITEMS_USER.DESCRIPTION_ENG)
                    .select(tx.select(
                                    ITEMS.ID,
                                    ITEMS.NAME,
                                    ITEMS.TYPE,
                                    ITEMS.SUBTYPE,
                                    ITEMS.CUSTOMIZATION,
                                    ITEMS.RARITY,
                                    ITEMS.DESCRIPTION,
                                    ITEMS.STATS,
                                    ITEMS.CREATED_AT,
                                    ITEMS.ROOM_ID,
                                    ITEMS.CREATOR_ID,
                                    ITEMS.IMG_URL,
                                    ITEMS.VISIBLE_FOR_PLAYERS,
                                    ITEMS.CREATOR,
                                    ITEMS.DESCRIPTION_ENG)
                            .from(ITEMS)
                            .where(ITEMS.CREATOR_ID.isNotNull())
                            .and(ITEMS.ID.notIn(tx.select(ITEMS_USER.ID).from(ITEMS_USER))))
                    .execute();
            tx.deleteFrom(ITEMS).where(ITEMS.CREATOR_ID.isNotNull()).execute();
        });
    }

    private Table<?> getRulesItemsTable(String ruleType) {
        if (ruleType != null && (ruleType.contains("24") || "2024".equalsIgnoreCase(ruleType))) {
            return ITEMS_24;
        }
        return ITEMS;
    }
}
