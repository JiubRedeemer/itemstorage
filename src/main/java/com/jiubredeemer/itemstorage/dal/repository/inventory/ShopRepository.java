package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.shop.ShopDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ShopRepository {

    private final DSLContext dsl;

    private static final org.jooq.Table<?> SHOP = DSL.table("itemstorage.shop");
    private static final org.jooq.Table<?> SHOP_ITEM = DSL.table("itemstorage.shop_item");

    private ShopDto map(Record r) {
        ShopDto dto = new ShopDto();
        dto.setId(r.get(DSL.field("id", UUID.class)));
        dto.setRoomId(r.get(DSL.field("room_id", UUID.class)));
        dto.setNpcId(r.get(DSL.field("npc_id", UUID.class)));
        dto.setName(r.get(DSL.field("name", String.class)));
        dto.setDescription(r.get(DSL.field("description", String.class)));
        dto.setImgUrl(r.get(DSL.field("img_url", String.class)));
        dto.setCreatedBy(r.get(DSL.field("created_by", UUID.class)));
        dto.setCreatedAt(r.get(DSL.field("created_at", LocalDateTime.class)));
        return dto;
    }

    private org.jooq.SelectJoinStep<? extends Record> selectShops() {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("room_id", UUID.class),
                        DSL.field("npc_id", UUID.class),
                        DSL.field("name", String.class),
                        DSL.field("description", String.class),
                        DSL.field("img_url", String.class),
                        DSL.field("created_by", UUID.class),
                        DSL.field("created_at", LocalDateTime.class)
                ).from(SHOP);
    }

    public List<ShopDto> findByRoom(UUID roomId) {
        return selectShops()
                .where(DSL.field("room_id", UUID.class).eq(roomId))
                .orderBy(DSL.field("created_at"))
                .fetch(this::map);
    }

    public Optional<ShopDto> findById(UUID id) {
        return selectShops()
                .where(DSL.field("id", UUID.class).eq(id))
                .fetchOptional(this::map);
    }

    public ShopDto create(UUID roomId, UUID createdBy, ShopDto dto) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(SHOP)
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("room_id", UUID.class), roomId)
                .set(DSL.field("npc_id", UUID.class), dto.getNpcId())
                .set(DSL.field("name", String.class), dto.getName())
                .set(DSL.field("description", String.class), dto.getDescription())
                .set(DSL.field("img_url", String.class), dto.getImgUrl())
                .set(DSL.field("created_by", UUID.class), createdBy)
                .set(DSL.field("created_at", LocalDateTime.class), LocalDateTime.now())
                .execute();
        return findById(id).orElseThrow();
    }

    public ShopDto update(UUID id, ShopDto dto) {
        dsl.update(SHOP)
                .set(DSL.field("npc_id", UUID.class), dto.getNpcId())
                .set(DSL.field("name", String.class), dto.getName())
                .set(DSL.field("description", String.class), dto.getDescription())
                .set(DSL.field("img_url", String.class), dto.getImgUrl())
                .where(DSL.field("id", UUID.class).eq(id))
                .execute();
        return findById(id).orElseThrow();
    }

    /**
     * Deletes the shop along with all its items (showcase entries).
     */
    public void deleteById(UUID id) {
        dsl.transaction(transaction -> {
            DSLContext tx = DSL.using(transaction);
            tx.deleteFrom(SHOP_ITEM).where(DSL.field("shop_id", UUID.class).eq(id)).execute();
            tx.deleteFrom(SHOP).where(DSL.field("id", UUID.class).eq(id)).execute();
        });
    }
}
