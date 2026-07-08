package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.dal.configuration.LicenseMode;
import com.jiubredeemer.itemstorage.dal.entity.tables.records.ItemSkillRecord;
import com.jiubredeemer.itemstorage.domain.model.bundle.ItemBundleDto;
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
import java.util.*;
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
        enrichBundles(itemDtos);
        return itemDtos;
    }

    public Optional<ItemDto> findById(UUID id) {
        Optional<ItemDto> itemDto = dsl.selectFrom(ITEMS_USER)
                .where(ITEMS_USER.ID.eq(id))
                .fetchOptionalInto(ItemDto.class);
        if (itemDto.isEmpty()) {
            itemDto = dsl.selectFrom(ITEM_BUNDLED)
                    .where(ITEM_BUNDLED.ID.eq(id))
                    .fetchOptionalInto(ItemDto.class);
        }
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
        itemDto.ifPresent(itemDtoPresent -> enrichBundles(Collections.singletonList(itemDtoPresent)));
        return itemDto;
    }

    public List<ItemDto> findByIds(List<UUID> ids) {
        List<ItemDto> itemDtos = new ArrayList<>();
//        List<ItemDto> itemDtos = dsl.selectFrom(ITEMS)
//                .where(ITEMS.ID.in(ids))
//                .and(ITEMS.CREATOR_ID.isNull())
//                .fetchInto(ItemDto.class);
//        itemDtos.addAll(dsl.selectFrom(ITEMS_24)
//                .where(ITEMS_24.ID.in(ids))
//                .and(ITEMS_24.CREATOR_ID.isNull())
//                .fetchInto(ItemDto.class));
        itemDtos.addAll(dsl.selectFrom(ITEMS_USER)
                .where(ITEMS_USER.ID.in(ids))
                .fetchInto(ItemDto.class));
        itemDtos.addAll(dsl.selectFrom(ITEM_BUNDLED)
                .where(ITEM_BUNDLED.ID.in(ids))
                .fetchInto(ItemDto.class));
        enrichSkills(itemDtos);
        enrichBundles(itemDtos);
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
        // Скрываем из поиска предметы-модели "до опознания" — их нельзя добавлять напрямую,
        // они видны только через маскировку реального предмета
        var unidentifiedModelCheck = ITEMS_USER.as("unidentified_model_check");
        userItemsCondition = userItemsCondition.and(
                DSL.notExists(DSL.select(DSL.val(1)).from(unidentifiedModelCheck)
                        .where(unidentifiedModelCheck.UNIDENTIFIED_ITEM_ID.eq(ITEMS_USER.ID)))
        );
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

        // Предметы из бандлов, включённых для этой комнаты (room_bundle -> item_bundled)
        var bundledItemsCondition = DSL.condition("1=1");
        if (searchQuery != null && !searchQuery.isBlank()) {
            var searchPattern = "%" + searchQuery + "%";
            bundledItemsCondition = bundledItemsCondition.and(
                    DSL.field("item_bundled.name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field("item_bundled.name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
        }
        if (roomId != null) {
            bundledItemsCondition = bundledItemsCondition.and(
                    DSL.exists(DSL.select(DSL.val(1))
                            .from(ROOM_BUNDLE)
                            .where(ROOM_BUNDLE.ROOM_ID.eq(roomId).and(ROOM_BUNDLE.ITEM_BUNDLE_ID.eq(ITEM_BUNDLED.ITEM_BUNDLE_ID))))
            );
        } else {
            bundledItemsCondition = bundledItemsCondition.and(DSL.condition("1=0"));
        }
        if (lastSeenCreatedAt != null && lastSeenId != null) {
            bundledItemsCondition = bundledItemsCondition.and(
                    DSL.row(ITEM_BUNDLED.CREATED_AT, ITEM_BUNDLED.ID).gt(DSL.row(lastSeenCreatedAt, lastSeenId))
            );
        }
        if (type != null && !type.isBlank()) {
            bundledItemsCondition = bundledItemsCondition.and(ITEM_BUNDLED.TYPE.eq(type));
        }
        if (subtype != null && !subtype.isBlank()) {
            bundledItemsCondition = bundledItemsCondition.and(ITEM_BUNDLED.SUBTYPE.eq(subtype));
        }
        if (rarity != null && !rarity.isBlank()) {
            bundledItemsCondition = bundledItemsCondition.and(ITEM_BUNDLED.RARITY.eq(rarity));
        }
        if (customization != null) {
            bundledItemsCondition = bundledItemsCondition.and(ITEM_BUNDLED.CUSTOMIZATION.eq(customization));
        }
        if (hasSkills != null) {
            var bundledSkillsExists = DSL.exists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(ITEM_BUNDLED.ID)));
            bundledItemsCondition = hasSkills
                    ? bundledItemsCondition.and(bundledSkillsExists)
                    : bundledItemsCondition.and(DSL.notExists(DSL.select(DSL.val(1)).from(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(ITEM_BUNDLED.ID))));
        }
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                bundledItemsCondition = bundledItemsCondition.and(
                        DSL.exists(DSL.select(DSL.val(1))
                                .from(DSL.table("itemstorage.item_tag_relation").as("itr_b"))
                                .join(DSL.table("itemstorage.item_tag").as("it_b"))
                                .on(DSL.field("it_b.id").eq(DSL.field("itr_b.tag_id")))
                                .where(DSL.field("itr_b.item_id").eq(ITEM_BUNDLED.ID)
                                        .and(DSL.field("it_b.name", String.class).eq(tag))))
                );
            }
        }

        List<ItemDto> baseItems = new ArrayList<>();
//        final List<ItemDto> baseItems = dsl.selectFrom(rulesItemsTable)
//                .where(baseItemsCondition)
//                .orderBy(DSL.field(rulesItemsTableName + ".created_at").asc(), DSL.field(rulesItemsTableName + ".id").asc())
//                .limit(limit)
//                .fetchInto(ItemDto.class);
        final List<ItemDto> userItems = dsl.selectFrom(ITEMS_USER)
                .where(userItemsCondition)
                .orderBy(ITEMS_USER.CREATED_AT.asc(), ITEMS_USER.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);
        final List<ItemDto> bundledItems = dsl.selectFrom(ITEM_BUNDLED)
                .where(bundledItemsCondition)
                .orderBy(ITEM_BUNDLED.CREATED_AT.asc(), ITEM_BUNDLED.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);

        // Дедуп по id: система бандл-2014/2024 может ссылаться на те же id, что уже есть в items/items_24
        final Map<UUID, ItemDto> merged = new LinkedHashMap<>();
        Stream.concat(Stream.concat(baseItems.stream(), userItems.stream()), bundledItems.stream())
                .sorted(Comparator.comparing(ItemDto::getCreatedAt).thenComparing(ItemDto::getId))
                .forEach(item -> merged.putIfAbsent(item.getId(), item));
        final List<ItemDto> itemDtos = merged.values().stream().limit(limit).toList();

        enrichSkills(itemDtos);
        enrichBundles(itemDtos);

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

        // Скрываем из поиска предметы-модели "до опознания" — их нельзя добавлять напрямую
        var unidentifiedModelCheckOwned = ITEMS_USER.as("unidentified_model_check_owned");
        condition = condition.and(
                DSL.notExists(DSL.select(DSL.val(1)).from(unidentifiedModelCheckOwned)
                        .where(unidentifiedModelCheckOwned.UNIDENTIFIED_ITEM_ID.eq(ITEMS_USER.ID)))
        );

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
        enrichBundles(itemDtos);

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
    public List<ItemBundleDto> findBundlesForItems(List<UUID> bundlesIds) {
        return dsl.selectFrom(ITEM_BUNDLE)
                .where(ITEM_BUNDLE.ID.in(bundlesIds))
                .fetchInto(ItemBundleDto.class);
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
                .set(ITEMS_USER.HIDDEN_STATS, itemDto.getHiddenStats() != null && itemDto.getHiddenStats())
                .set(ITEMS_USER.UNIDENTIFIED_ITEM_ID, itemDto.getUnidentifiedItemId())
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

    private void enrichBundles(List<ItemDto> itemDtos) {
        List<ItemBundleDto> bundles = findBundlesForItems(itemDtos.stream().map(ItemDto::getItemBundleId).collect(Collectors.toList()));
        itemDtos.forEach(itemDto -> itemDto.setItemBundleName(bundles
                .stream()
                .filter(bundleDto -> bundleDto.getId().equals(itemDto.getItemBundleId()))
                .findAny().orElseGet(ItemBundleDto::new).getName()));
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
                .set(ITEMS_USER.HIDDEN_STATS, itemDto.getHiddenStats() != null && itemDto.getHiddenStats())
                .set(ITEMS_USER.UNIDENTIFIED_ITEM_ID, itemDto.getUnidentifiedItemId())
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


    /**
     * Все предметы, созданные пользователем в любых комнатах (для импорта в бандл).
     * Модели "до опознания" исключаются — они переносятся вместе с родительским предметом.
     */
    public List<ItemDto> findAllByCreatorId(UUID creatorId, String search) {
        var condition = ITEMS_USER.CREATOR_ID.eq(creatorId);
        var unidentifiedModelCheck = ITEMS_USER.as("unidentified_model_check_creator");
        condition = condition.and(
                DSL.notExists(DSL.select(DSL.val(1)).from(unidentifiedModelCheck)
                        .where(unidentifiedModelCheck.UNIDENTIFIED_ITEM_ID.eq(ITEMS_USER.ID)))
        );
        if (search != null && !search.isBlank()) {
            var searchPattern = "%" + search.trim() + "%";
            condition = condition.and(
                    DSL.field("items_user.name ->> 'rus'", String.class).likeIgnoreCase(searchPattern)
                            .or(DSL.field("items_user.name ->> 'eng'", String.class).likeIgnoreCase(searchPattern))
            );
        }
        List<ItemDto> itemDtos = dsl.selectFrom(ITEMS_USER)
                .where(condition)
                .orderBy(ITEMS_USER.CREATED_AT.desc())
                .fetchInto(ItemDto.class);
        enrichSkills(itemDtos);
        enrichBundles(itemDtos);
        return itemDtos;
    }

    /**
     * Импортирует копии предметов (по их id) в бандл. Каждая копия получает новый id,
     * копируются навыки и связи с глобальными тегами. Связанные модели "до опознания"
     * также копируются и перелинковываются.
     */
    public void importItemsIntoBundle(UUID bundleId, List<UUID> itemIds, UUID userId) {
        if (itemIds == null || itemIds.isEmpty()) return;
        List<ItemDto> sources = findByIds(itemIds);
        for (ItemDto source : sources) {
            UUID newUnidentifiedId = null;
            if (source.getUnidentifiedItemId() != null) {
                Optional<ItemDto> unidentifiedSource = findById(source.getUnidentifiedItemId());
                if (unidentifiedSource.isPresent()) {
                    ItemDto unidentifiedCopy = unidentifiedSource.get();
                    newUnidentifiedId = UUID.randomUUID();
                    copyIntoBundle(bundleId, unidentifiedCopy, newUnidentifiedId, null, userId);
                }
            }
            copyIntoBundle(bundleId, source, UUID.randomUUID(), newUnidentifiedId, userId);
        }
    }

    private void copyIntoBundle(UUID bundleId, ItemDto source, UUID newId, UUID newUnidentifiedId, UUID userId) {
        List<ItemSkillDto> skills = source.getSkills();
        source.setId(newId);
        source.setUnidentifiedItemId(newUnidentifiedId);
        source.setCreatorId(userId);
        // createBundledItem сам переносит tagIds из stats в item_tag_relation
        createBundledItem(bundleId, source);
        if (skills != null && !skills.isEmpty()) {
            skills.forEach(skill -> skill.setItemId(newId));
            createSkills(skills);
        }
    }

    public List<ItemDto> findBundledItemsByBundleId(UUID bundleId) {
        List<ItemDto> itemDtos = dsl.selectFrom(ITEM_BUNDLED)
                .where(ITEM_BUNDLED.ITEM_BUNDLE_ID.eq(bundleId))
                .orderBy(ITEM_BUNDLED.CREATED_AT.asc())
                .fetchInto(ItemDto.class);
        enrichSkills(itemDtos);
        enrichBundles(itemDtos);
        return itemDtos;
    }

    public void createBundledItem(UUID bundleId, ItemDto itemDto) {
        List<UUID> tagIds = itemDto.getStats() != null ? itemDto.getStats().getTagIds() : null;
        if (itemDto.getStats() != null) {
            itemDto.getStats().setTags(null);
            itemDto.getStats().setTagIds(null);
        }
        dsl.insertInto(ITEM_BUNDLED)
                .set(ITEM_BUNDLED.ID, itemDto.getId())
                .set(ITEM_BUNDLED.ITEM_BUNDLE_ID, bundleId)
                .set(ITEM_BUNDLED.CREATOR_ID, itemDto.getCreatorId())
                .set(ITEM_BUNDLED.CREATED_AT, LocalDateTime.now())
                .set(ITEM_BUNDLED.VISIBLE_FOR_PLAYERS, itemDto.getVisibleForPlayers())
                .set(ITEM_BUNDLED.CREATOR, itemDto.getCreator())
                .set(ITEM_BUNDLED.CUSTOMIZATION, itemDto.getCustomization())
                .set(ITEM_BUNDLED.DESCRIPTION, itemDto.getDescription())
                .set(ITEM_BUNDLED.IMG_URL, itemDto.getImgUrl())
                .set(ITEM_BUNDLED.NAME, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getName())))
                .set(ITEM_BUNDLED.TYPE, itemDto.getType().name())
                .set(ITEM_BUNDLED.SUBTYPE, itemDto.getSubtype() != null ? itemDto.getSubtype().name() : null)
                .set(ITEM_BUNDLED.STATS, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getStats())))
                .set(ITEM_BUNDLED.RARITY, itemDto.getRarity().name())
                .set(ITEM_BUNDLED.HIDDEN_STATS, itemDto.getHiddenStats() != null && itemDto.getHiddenStats())
                .set(ITEM_BUNDLED.UNIDENTIFIED_ITEM_ID, itemDto.getUnidentifiedItemId())
                .execute();
        createTagRelations(itemDto.getId(), tagIds);
    }

    public void updateBundledItem(ItemDto itemDto) {
        List<UUID> tagIds = itemDto.getStats() != null ? itemDto.getStats().getTagIds() : null;
        if (itemDto.getStats() != null) {
            itemDto.getStats().setTags(null);
            itemDto.getStats().setTagIds(null);
        }
        dsl.update(ITEM_BUNDLED)
                .set(ITEM_BUNDLED.VISIBLE_FOR_PLAYERS, itemDto.getVisibleForPlayers())
                .set(ITEM_BUNDLED.CREATOR, itemDto.getCreator())
                .set(ITEM_BUNDLED.CUSTOMIZATION, itemDto.getCustomization())
                .set(ITEM_BUNDLED.DESCRIPTION, itemDto.getDescription())
                .set(ITEM_BUNDLED.IMG_URL, itemDto.getImgUrl())
                .set(ITEM_BUNDLED.NAME, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getName())))
                .set(ITEM_BUNDLED.TYPE, itemDto.getType().name())
                .set(ITEM_BUNDLED.SUBTYPE, itemDto.getSubtype() != null ? itemDto.getSubtype().name() : null)
                .set(ITEM_BUNDLED.STATS, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getStats())))
                .set(ITEM_BUNDLED.RARITY, itemDto.getRarity().name())
                .set(ITEM_BUNDLED.HIDDEN_STATS, itemDto.getHiddenStats() != null && itemDto.getHiddenStats())
                .set(ITEM_BUNDLED.UNIDENTIFIED_ITEM_ID, itemDto.getUnidentifiedItemId())
                .where(ITEM_BUNDLED.ID.eq(itemDto.getId()))
                .execute();
        itemTagRepository.deleteTagRelationsByItemId(itemDto.getId());
        createTagRelations(itemDto.getId(), tagIds);
    }

    /**
     * Удаляет предмет-заготовку из бандла вместе с его навыками и тегами.
     * Не трогает inventory_item в комнатах, где этот предмет уже выдан игрокам —
     * такие записи станут "осиротевшими" (ссылка на несуществующий item_id).
     */
    public void deleteBundledItemById(UUID itemId) {
        dsl.transaction(transaction -> {
            DSL.using(transaction).deleteFrom(ITEM_SKILL).where(ITEM_SKILL.ITEM_ID.eq(itemId)).execute();
            DSL.using(transaction).deleteFrom(DSL.table("itemstorage.item_tag_relation")).where(DSL.field("item_id", UUID.class).eq(itemId)).execute();
            DSL.using(transaction).deleteFrom(ITEM_BUNDLED).where(ITEM_BUNDLED.ID.eq(itemId)).execute();
        });
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
