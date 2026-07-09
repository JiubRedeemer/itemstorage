package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.shop.ShopItemDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ShopItemRepository {

    private final DSLContext dsl;

    private static final org.jooq.Table<?> SHOP_ITEM = DSL.table("itemstorage.shop_item");

    private ShopItemDto map(Record r) {
        ShopItemDto dto = new ShopItemDto();
        dto.setId(r.get(DSL.field("id", UUID.class)));
        dto.setShopId(r.get(DSL.field("shop_id", UUID.class)));
        dto.setItemId(r.get(DSL.field("item_id", UUID.class)));
        dto.setPriceGold(r.get(DSL.field("price_gold", Long.class)));
        dto.setPriceSilver(r.get(DSL.field("price_silver", Long.class)));
        dto.setPriceCopper(r.get(DSL.field("price_copper", Long.class)));
        dto.setSortOrder(r.get(DSL.field("sort_order", Integer.class)));
        return dto;
    }

    public List<ShopItemDto> findByShopId(UUID shopId) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("shop_id", UUID.class),
                        DSL.field("item_id", UUID.class),
                        DSL.field("price_gold", Long.class),
                        DSL.field("price_silver", Long.class),
                        DSL.field("price_copper", Long.class),
                        DSL.field("sort_order", Integer.class)
                ).from(SHOP_ITEM)
                .where(DSL.field("shop_id", UUID.class).eq(shopId))
                .orderBy(DSL.field("sort_order"), DSL.field("created_at"))
                .fetch(this::map);
    }

    public ShopItemDto upsert(UUID shopId, ShopItemDto dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        boolean exists = dto.getId() != null && dsl.fetchExists(
                dsl.selectOne().from(SHOP_ITEM).where(DSL.field("id", UUID.class).eq(id)));
        long gold = dto.getPriceGold() != null ? dto.getPriceGold() : 0L;
        long silver = dto.getPriceSilver() != null ? dto.getPriceSilver() : 0L;
        long copper = dto.getPriceCopper() != null ? dto.getPriceCopper() : 0L;
        int sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : 0;
        if (exists) {
            dsl.update(SHOP_ITEM)
                    .set(DSL.field("item_id", UUID.class), dto.getItemId())
                    .set(DSL.field("price_gold", Long.class), gold)
                    .set(DSL.field("price_silver", Long.class), silver)
                    .set(DSL.field("price_copper", Long.class), copper)
                    .set(DSL.field("sort_order", Integer.class), sortOrder)
                    .where(DSL.field("id", UUID.class).eq(id))
                    .execute();
        } else {
            dsl.insertInto(SHOP_ITEM)
                    .set(DSL.field("id", UUID.class), id)
                    .set(DSL.field("shop_id", UUID.class), shopId)
                    .set(DSL.field("item_id", UUID.class), dto.getItemId())
                    .set(DSL.field("price_gold", Long.class), gold)
                    .set(DSL.field("price_silver", Long.class), silver)
                    .set(DSL.field("price_copper", Long.class), copper)
                    .set(DSL.field("sort_order", Integer.class), sortOrder)
                    .set(DSL.field("created_at", LocalDateTime.class), LocalDateTime.now())
                    .execute();
        }
        dto.setId(id);
        dto.setShopId(shopId);
        return dto;
    }

    public void deleteById(UUID id) {
        dsl.deleteFrom(SHOP_ITEM).where(DSL.field("id", UUID.class).eq(id)).execute();
    }
}
