package com.jiubredeemer.itemstorage.dal.repository.inventory;

import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

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
}
