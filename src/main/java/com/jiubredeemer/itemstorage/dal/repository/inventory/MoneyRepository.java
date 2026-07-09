package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.money.MoneyDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.jiubredeemer.itemstorage.dal.entity.Tables.MONEY;

@Repository
@RequiredArgsConstructor
public class MoneyRepository {
    private final DSLContext dsl;

    // Колонки electrum_count / platinum_count добавлены миграцией; jOOQ codegen ещё не знает о них,
    // поэтому обращаемся к ним через raw-поля.
    private static final Field<Long> ELECTRUM_COUNT = DSL.field(DSL.name("electrum_count"), Long.class);
    private static final Field<Long> PLATINUM_COUNT = DSL.field(DSL.name("platinum_count"), Long.class);

    public Optional<MoneyDto> findByInventoryId(UUID inventoryId) {
        return dsl.select(
                        MONEY.ID,
                        MONEY.INVENTORY_ID,
                        MONEY.GOLDEN_COUNT,
                        MONEY.SILVER_COUNT,
                        MONEY.COPPER_COUNT,
                        ELECTRUM_COUNT,
                        PLATINUM_COUNT
                )
                .from(MONEY)
                .where(MONEY.INVENTORY_ID.eq(inventoryId))
                .fetchOptionalInto(MoneyDto.class);
    }

    public Optional<MoneyDto> changeCount(UUID inventoryId, MoneyDto moneyDto) {
        dsl.update(MONEY)
                .set(MONEY.GOLDEN_COUNT, moneyDto.getGoldenCount())
                .set(MONEY.SILVER_COUNT, moneyDto.getSilverCount())
                .set(MONEY.COPPER_COUNT, moneyDto.getCopperCount())
                .set(ELECTRUM_COUNT, moneyDto.getElectrumCount() != null ? moneyDto.getElectrumCount() : 0L)
                .set(PLATINUM_COUNT, moneyDto.getPlatinumCount() != null ? moneyDto.getPlatinumCount() : 0L)
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
                .set(ELECTRUM_COUNT, moneyDto.getElectrumCount() != null ? moneyDto.getElectrumCount() : 0L)
                .set(PLATINUM_COUNT, moneyDto.getPlatinumCount() != null ? moneyDto.getPlatinumCount() : 0L)
                .execute();
        return findByInventoryId(inventoryId);
    }

}
