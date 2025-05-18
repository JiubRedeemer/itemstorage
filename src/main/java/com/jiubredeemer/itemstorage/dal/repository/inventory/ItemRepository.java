package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.ITEMS;

@Repository
@RequiredArgsConstructor
public class ItemRepository {
    private final DSLContext dsl;

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
}
