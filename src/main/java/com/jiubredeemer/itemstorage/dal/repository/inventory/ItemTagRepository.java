package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.item.ItemTagDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ItemTagRepository {

    private final DSLContext dsl;

    /**
     * Returns global tags (room_id IS NULL) + room-specific tags for the given roomId.
     * If roomId is null, returns only global tags.
     */
    public List<ItemTagDto> findByRoomId(UUID roomId) {
        var condition = DSL.field("room_id").isNull();
        if (roomId != null) {
            condition = condition.or(DSL.field("room_id", UUID.class).eq(roomId));
        }
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("name", String.class),
                        DSL.field("description", String.class),
                        DSL.field("room_id", UUID.class)
                )
                .from(DSL.table("itemstorage.item_tag"))
                .where(condition)
                .orderBy(DSL.field("name"))
                .fetch(r -> new ItemTagDto(
                        r.get(DSL.field("id", UUID.class)),
                        r.get(DSL.field("name", String.class)),
                        r.get(DSL.field("description", String.class)),
                        r.get(DSL.field("room_id", UUID.class))
                ));
    }

    /**
     * Creates a new room-scoped tag.
     */
    public ItemTagDto create(String name, UUID roomId) {
        return dsl.insertInto(DSL.table("itemstorage.item_tag"))
                .set(DSL.field("name"), name)
                .set(DSL.field("room_id"), roomId)
                .returningResult(
                        DSL.field("id", UUID.class),
                        DSL.field("name", String.class),
                        DSL.field("description", String.class),
                        DSL.field("room_id", UUID.class)
                )
                .fetchOne(r -> new ItemTagDto(
                        r.get(DSL.field("id", UUID.class)),
                        r.get(DSL.field("name", String.class)),
                        r.get(DSL.field("description", String.class)),
                        r.get(DSL.field("room_id", UUID.class))
                ));
    }

    /**
     * Updates the description of a tag.
     */
    public ItemTagDto update(UUID id, String description) {
        return dsl.update(DSL.table("itemstorage.item_tag"))
                .set(DSL.field("description"), description)
                .where(DSL.field("id", UUID.class).eq(id))
                .returningResult(
                        DSL.field("id", UUID.class),
                        DSL.field("name", String.class),
                        DSL.field("description", String.class),
                        DSL.field("room_id", UUID.class)
                )
                .fetchOne(r -> new ItemTagDto(
                        r.get(DSL.field("id", UUID.class)),
                        r.get(DSL.field("name", String.class)),
                        r.get(DSL.field("description", String.class)),
                        r.get(DSL.field("room_id", UUID.class))
                ));
    }

    /**
     * Fetches all tag relations for a list of item IDs, returning a map of itemId -> list of tags.
     */
    public Map<UUID, List<ItemTagDto>> findDistinctByItemIds(List<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Map.of();
        return dsl.select(
                        DSL.field("itr.item_id", UUID.class),
                        DSL.field("it.id", UUID.class),
                        DSL.field("it.name", String.class),
                        DSL.field("it.description", String.class),
                        DSL.field("it.room_id", UUID.class)
                )
                .from(DSL.table("itemstorage.item_tag_relation").as("itr"))
                .join(DSL.table("itemstorage.item_tag").as("it"))
                .on(DSL.field("it.id").eq(DSL.field("itr.tag_id")))
                .where(DSL.field("itr.item_id").in(itemIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(DSL.field("itr.item_id", UUID.class)),
                        Collectors.mapping(r -> new ItemTagDto(
                                r.get(DSL.field("it.id", UUID.class)),
                                r.get(DSL.field("it.name", String.class)),
                                r.get(DSL.field("it.description", String.class)),
                                r.get(DSL.field("it.room_id", UUID.class))
                        ), Collectors.toList())
                ));
    }

    public void addTagRelation(UUID itemId, UUID tagId) {
        dsl.insertInto(DSL.table("itemstorage.item_tag_relation"))
                .set(DSL.field("item_id"), itemId)
                .set(DSL.field("tag_id"), tagId)
                .onConflictDoNothing()
                .execute();
    }

    public void deleteTagRelationsByItemId(UUID itemId) {
        dsl.deleteFrom(DSL.table("itemstorage.item_tag_relation"))
                .where(DSL.field("item_id", UUID.class).eq(itemId))
                .execute();
    }

    public void deleteTagRelation(UUID itemId, UUID tagId) {
        dsl.deleteFrom(DSL.table("itemstorage.item_tag_relation"))
                .where(DSL.field("item_id", UUID.class).eq(itemId)
                        .and(DSL.field("tag_id", UUID.class).eq(tagId)))
                .execute();
    }
}
