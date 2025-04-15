package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.money.MoneyDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.MONEY;

@Repository
@RequiredArgsConstructor
public class MoneyRepository {
    private final DSLContext dsl;

    public Optional<MoneyDto> findByInventoryId(UUID inventoryId) {
        return dsl.selectFrom(MONEY)
                .where(MONEY.INVENTORY_ID.eq(inventoryId))
                .fetchOptionalInto(MoneyDto.class);
    }

    public Optional<MoneyDto> changeCount(UUID inventoryId, MoneyDto moneyDto) {
        dsl.update(MONEY)
                .set(MONEY.GOLDEN_COUNT, moneyDto.getGoldenCount())
                .set(MONEY.SILVER_COUNT, moneyDto.getSilverCount())
                .set(MONEY.COPPER_COUNT, moneyDto.getCopperCount())
                .where(MONEY.INVENTORY_ID.eq(inventoryId))
                .execute();
        return findByInventoryId(inventoryId);
    }

    public Optional<MoneyDto> create(UUID inventoryId, MoneyDto moneyDto) {
        dsl.insertInto(MONEY)
                .set(MONEY.ID, UUID.randomUUID())
                .set(MONEY.INVENTORY_ID, inventoryId)
                .set(MONEY.GOLDEN_COUNT, moneyDto.getGoldenCount() != null ? moneyDto.getGoldenCount() : 0)
                .set(MONEY.SILVER_COUNT, moneyDto.getSilverCount() != null ? moneyDto.getSilverCount() : 0)
                .set(MONEY.COPPER_COUNT, moneyDto.getCopperCount() != null ? moneyDto.getCopperCount() : 0)
                .execute();
        return findByInventoryId(inventoryId);
    }

}
