package com.jiubredeemer.itemstorage.dal.repository.inventory;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.*;

@Repository
@RequiredArgsConstructor
public class RoomBundleRepository {

    private final DSLContext dsl;

    public List<UUID> findEnabledBundleIdsByRoom(UUID roomId) {
        return dsl.select(ROOM_BUNDLE.ITEM_BUNDLE_ID)
                .from(ROOM_BUNDLE)
                .where(ROOM_BUNDLE.ROOM_ID.eq(roomId))
                .fetch(ROOM_BUNDLE.ITEM_BUNDLE_ID);
    }

    public void enable(UUID roomId, UUID bundleId) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne().from(ROOM_BUNDLE)
                        .where(ROOM_BUNDLE.ROOM_ID.eq(roomId).and(ROOM_BUNDLE.ITEM_BUNDLE_ID.eq(bundleId)))
        );
        if (exists) return;
        dsl.insertInto(ROOM_BUNDLE)
                .set(ROOM_BUNDLE.ID, UUID.randomUUID())
                .set(ROOM_BUNDLE.ROOM_ID, roomId)
                .set(ROOM_BUNDLE.ITEM_BUNDLE_ID, bundleId)
                .execute();
    }

    public void disable(UUID roomId, UUID bundleId) {
        dsl.deleteFrom(ROOM_BUNDLE)
                .where(ROOM_BUNDLE.ROOM_ID.eq(roomId).and(ROOM_BUNDLE.ITEM_BUNDLE_ID.eq(bundleId)))
                .execute();
    }
}
