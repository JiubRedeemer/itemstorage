package com.jiubredeemer.itemstorage.domain.service;

import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemBundleRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.ItemRepository;
import com.jiubredeemer.itemstorage.dal.repository.inventory.RoomBundleRepository;
import com.jiubredeemer.itemstorage.domain.model.bundle.ItemBundleDto;
import com.jiubredeemer.itemstorage.domain.model.item.ItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BundleService {

    private final ItemBundleRepository itemBundleRepository;
    private final RoomBundleRepository roomBundleRepository;
    private final ItemRepository itemRepository;

    public List<ItemBundleDto> getVisibleBundles(UUID userId, String search) {
        List<ItemBundleDto> bundles = itemBundleRepository.findVisibleForUser(userId, search);
        markPurchased(userId, bundles);
        return bundles;
    }

    public List<ItemBundleDto> getOwnBundles(UUID userId) {
        return itemBundleRepository.findByOwner(userId);
    }

    public ItemBundleDto getBundle(UUID id) {
        return itemBundleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bundle not found"));
    }

    public ItemBundleDto createBundle(UUID ownerUserId, ItemBundleDto dto) {
        return itemBundleRepository.create(dto.getName(), dto.getDescription(), ownerUserId,
                dto.getImgUrl(), dto.getIsPublic(), dto.getPriceCrystals());
    }

    public ItemBundleDto updateBundle(UUID id, UUID userId, ItemBundleDto dto) {
        ItemBundleDto existing = getBundle(id);
        requireOwner(existing, userId);
        return itemBundleRepository.update(id, dto.getName(), dto.getDescription(),
                dto.getImgUrl(), dto.getIsPublic(), dto.getPriceCrystals());
    }

    public void deleteBundle(UUID id, UUID userId) {
        ItemBundleDto existing = getBundle(id);
        requireOwner(existing, userId);
        itemBundleRepository.deleteById(id);
    }

    public List<ItemDto> getBundleItems(UUID bundleId) {
        getBundle(bundleId);
        return itemRepository.findBundledItemsByBundleId(bundleId);
    }

    public ItemDto saveBundleItem(UUID bundleId, UUID userId, ItemDto itemDto) {
        ItemBundleDto bundle = getBundle(bundleId);
        requireOwner(bundle, userId);

        Optional<ItemDto> existing = itemDto.getId() != null ? itemRepository.findById(itemDto.getId()) : Optional.empty();
        if (existing.isPresent()) {
            itemDto.setCreatorId(existing.get().getCreatorId());
            itemRepository.updateBundledItem(itemDto);
            itemRepository.deleteSkillsByItemId(itemDto.getId());
        } else {
            if (itemDto.getId() == null) {
                itemDto.setId(UUID.randomUUID());
            }
            itemDto.setCreatorId(userId);
            itemRepository.createBundledItem(bundleId, itemDto);
        }
        if (itemDto.getSkills() != null) {
            itemDto.getSkills().forEach(skill -> skill.setItemId(itemDto.getId()));
            itemRepository.createSkills(itemDto.getSkills());
        }
        return itemRepository.findById(itemDto.getId()).orElseThrow();
    }

    public void deleteBundleItem(UUID itemId) {
        itemRepository.deleteBundledItemById(itemId);
    }

    public void importItems(UUID bundleId, UUID userId, List<UUID> itemIds) {
        ItemBundleDto bundle = getBundle(bundleId);
        requireOwner(bundle, userId);
        itemRepository.importItemsIntoBundle(bundleId, itemIds, userId);
    }

    public List<ItemDto> getItemsCreatedByUser(UUID userId, String search) {
        return itemRepository.findAllByCreatorId(userId, search);
    }

    public List<ItemBundleDto> getBundlesForRoom(UUID roomId, UUID userId, String search) {
        List<UUID> enabledIds = roomBundleRepository.findEnabledBundleIdsByRoom(roomId);
        List<ItemBundleDto> bundles = itemBundleRepository.findVisibleForUser(userId, search);
        markPurchased(userId, bundles);
        bundles.forEach(b -> b.setEnabled(enabledIds.contains(b.getId())));
        return bundles;
    }

    public void enableBundleForRoom(UUID roomId, UUID userId, UUID bundleId) {
        ItemBundleDto bundle = getBundle(bundleId);
        boolean isSystem = bundle.getOwnerUserId() == null;
        boolean isOwn = userId != null && userId.equals(bundle.getOwnerUserId());
        boolean isFreePublic = Boolean.TRUE.equals(bundle.getIsPublic())
                && (bundle.getPriceCrystals() == null || bundle.getPriceCrystals() <= 0);
        boolean isPurchased = userId != null && itemBundleRepository.isPurchased(userId, bundleId);
        if (!isSystem && !isOwn && !isFreePublic && !isPurchased) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Bundle must be purchased first");
        }
        roomBundleRepository.enable(roomId, bundleId);
    }

    public void disableBundleForRoom(UUID roomId, UUID bundleId) {
        roomBundleRepository.disable(roomId, bundleId);
    }

    /**
     * Фиксирует факт покупки. Списание кристаллов делает Core до вызова этого метода.
     */
    public ItemBundleDto recordPurchase(UUID userId, UUID bundleId) {
        ItemBundleDto bundle = getBundle(bundleId);
        if (!Boolean.TRUE.equals(bundle.getIsPublic())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bundle is not public");
        }
        itemBundleRepository.recordPurchase(userId, bundleId);
        bundle.setPurchased(true);
        return bundle;
    }

    private void markPurchased(UUID userId, List<ItemBundleDto> bundles) {
        if (userId == null) return;
        Set<UUID> purchased = itemBundleRepository.findPurchasedBundleIds(userId);
        bundles.forEach(b -> b.setPurchased(purchased.contains(b.getId())));
    }

    private void requireOwner(ItemBundleDto bundle, UUID userId) {
        if (bundle.getOwnerUserId() == null || !bundle.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the bundle owner");
        }
    }
}
