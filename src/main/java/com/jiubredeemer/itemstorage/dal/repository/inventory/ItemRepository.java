package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.INVENTORY;
import static com.jiubredeemer.itemstorage.dal.entity.Tables.ITEMS;

@Repository
@RequiredArgsConstructor
public class ItemRepository {
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public List<ItemDto> findAll() {
        return dsl.selectFrom(ITEMS)
                .fetchInto(ItemDto.class);
    }

    public Optional<ItemDto> findById(UUID id) {
        return dsl.selectFrom(ITEMS)
                .where(ITEMS.ID.eq(id))
                .fetchOptionalInto(ItemDto.class);
    }

    public List<ItemDto> findByIds(List<UUID> ids) {
        return dsl.selectFrom(ITEMS)
                .where(ITEMS.ID.in(ids))
                .fetchInto(ItemDto.class);
    }

    public List<ItemDto> searchByNameRoomAndCommunityItems(
            String searchQuery,
            UUID roomId,
            UUID userId,
            LocalDateTime lastSeenCreatedAt,
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

        if (userId != null) {
            condition.and((ITEMS.CREATOR_ID.eq(userId).and(ITEMS.VISIBLE_FOR_PLAYERS.eq(false))).or(ITEMS.CREATOR_ID.isNull()));
        }

        // Добавляем seek-пагинацию по created_at + id
        if (lastSeenCreatedAt != null && lastSeenId != null) {
            condition = condition.and(
                    ITEMS.CREATED_AT.ge(lastSeenCreatedAt).and(ITEMS.ID.gt(lastSeenId))
            );
        }

        return dsl.selectFrom(ITEMS)
                .where(condition)
                .orderBy(ITEMS.CREATED_AT.asc(), ITEMS.ID.asc())
                .limit(limit)
                .fetchInto(ItemDto.class);
    }

    public void create(ItemDto itemDto) throws JsonProcessingException {
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
                .set(ITEMS.SUBTYPE, itemDto.getSubtype().name())
                .set(ITEMS.STATS, JSONB.valueOf(objectMapper.writeValueAsString(itemDto.getStats())))
                .set(ITEMS.RARITY, itemDto.getRarity().name())
                .execute();
    }
}
