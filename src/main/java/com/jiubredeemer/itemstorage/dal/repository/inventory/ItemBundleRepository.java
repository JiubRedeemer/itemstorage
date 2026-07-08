package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.bundle.ItemBundleDto;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.*;

@Repository
@RequiredArgsConstructor
public class ItemBundleRepository {

    private final DSLContext dsl;

//    private static final org.jooq.Table<?> BUNDLE = DSL.table("itemstorage.item_bundle");

    private ItemBundleDto map(Record r) {
        ItemBundleDto dto = new ItemBundleDto();
        dto.setId(r.get(DSL.field("id", UUID.class)));
        dto.setName(r.get(DSL.field("name", String.class)));
        dto.setDescription(r.get(DSL.field("description", String.class)));
        dto.setCreatedAt(r.get(DSL.field("created_at", LocalDateTime.class)));
        dto.setImgUrl(r.get(DSL.field("img_url", String.class)));
        dto.setOwnerUserId(r.get(DSL.field("owner_user_id", UUID.class)));
        dto.setIsPublic(r.get(DSL.field("is_public", Boolean.class)));
        dto.setPriceCrystals(r.get(DSL.field("price_crystals", Integer.class)));
        return dto;
    }

    private org.jooq.SelectJoinStep<? extends Record> selectBundles() {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("name", String.class),
                        DSL.field("description", String.class),
                        DSL.field("created_at", LocalDateTime.class),
                        DSL.field("img_url", String.class),
                        DSL.field("owner_user_id", UUID.class),
                        DSL.field("is_public", Boolean.class),
                        DSL.field("price_crystals", Integer.class)
                ).from(ITEM_BUNDLE);
    }

    /**
     * Бандлы, видимые пользователю: системные (owner_user_id IS NULL),
     * публичные и его собственные. Опциональный поиск по названию.
     */
    public List<ItemBundleDto> findVisibleForUser(UUID userId, String search) {
        Condition condition = DSL.field("owner_user_id", UUID.class).isNull()
                .or(DSL.field("is_public", Boolean.class).isTrue());
        if (userId != null) {
            condition = condition.or(DSL.field("owner_user_id", UUID.class).eq(userId));
        }
        if (search != null && !search.isBlank()) {
            condition = condition.and(DSL.field("name", String.class).likeIgnoreCase("%" + search.trim() + "%"));
        }
        return selectBundles()
                .where(condition)
                .orderBy(DSL.field("name"))
                .fetch(this::map);
    }

    public List<ItemBundleDto> findByOwner(UUID ownerUserId) {
        return selectBundles()
                .where(DSL.field("owner_user_id", UUID.class).eq(ownerUserId))
                .orderBy(DSL.field("name"))
                .fetch(this::map);
    }

    public Optional<ItemBundleDto> findById(UUID id) {
        return selectBundles()
                .where(DSL.field("id", UUID.class).eq(id))
                .fetchOptional(this::map);
    }

    public ItemBundleDto create(String name, String description, UUID ownerUserId, String imgUrl,
                                Boolean isPublic, Integer priceCrystals) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(ITEM_BUNDLE)
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("name", String.class), name)
                .set(DSL.field("description", String.class), description)
                .set(DSL.field("created_at", LocalDateTime.class), LocalDateTime.now())
                .set(DSL.field("img_url", String.class), imgUrl)
                .set(DSL.field("owner_user_id", UUID.class), ownerUserId)
                .set(DSL.field("is_public", Boolean.class), isPublic != null && isPublic)
                .set(DSL.field("price_crystals", Integer.class), priceCrystals != null ? priceCrystals : 0)
                .execute();
        return findById(id).orElseThrow();
    }

    public ItemBundleDto update(UUID id, String name, String description, String imgUrl,
                                Boolean isPublic, Integer priceCrystals) {
        dsl.update(ITEM_BUNDLE)
                .set(DSL.field("name", String.class), name)
                .set(DSL.field("description", String.class), description)
                .set(DSL.field("img_url", String.class), imgUrl)
                .set(DSL.field("is_public", Boolean.class), isPublic != null && isPublic)
                .set(DSL.field("price_crystals", Integer.class), priceCrystals != null ? priceCrystals : 0)
                .where(DSL.field("id", UUID.class).eq(id))
                .execute();
        return findById(id).orElseThrow();
    }

    /**
     * Deletes the bundle along with its membership (items in it, room toggles, purchases).
     * Note: does not touch inventory_item rows that may already reference items
     * from this bundle in players' inventories — those references become dangling.
     */
    public void deleteById(UUID id) {
        dsl.transaction(transaction -> {
            DSLContext tx = DSL.using(transaction);
            tx.deleteFrom(DSL.table("itemstorage.bundle_purchase"))
                    .where(DSL.field("item_bundle_id", UUID.class).eq(id)).execute();
            tx.deleteFrom(ROOM_BUNDLE).where(ROOM_BUNDLE.ITEM_BUNDLE_ID.eq(id)).execute();
            tx.deleteFrom(ITEM_BUNDLED).where(ITEM_BUNDLED.ITEM_BUNDLE_ID.eq(id)).execute();
            tx.deleteFrom(ITEM_BUNDLE).where(ITEM_BUNDLE.ID.eq(id)).execute();
        });
    }

    public Set<UUID> findPurchasedBundleIds(UUID userId) {
        return dsl.select(DSL.field("item_bundle_id", UUID.class))
                .from(DSL.table("itemstorage.bundle_purchase"))
                .where(DSL.field("user_id", UUID.class).eq(userId))
                .fetch(DSL.field("item_bundle_id", UUID.class))
                .stream().collect(Collectors.toSet());
    }

    public boolean isPurchased(UUID userId, UUID bundleId) {
        return dsl.fetchExists(
                dsl.selectOne().from(DSL.table("itemstorage.bundle_purchase"))
                        .where(DSL.field("user_id", UUID.class).eq(userId)
                                .and(DSL.field("item_bundle_id", UUID.class).eq(bundleId)))
        );
    }

    public void recordPurchase(UUID userId, UUID bundleId) {
        dsl.insertInto(DSL.table("itemstorage.bundle_purchase"))
                .set(DSL.field("user_id", UUID.class), userId)
                .set(DSL.field("item_bundle_id", UUID.class), bundleId)
                .set(DSL.field("created_at", LocalDateTime.class), LocalDateTime.now())
                .onConflictDoNothing()
                .execute();
    }
}
