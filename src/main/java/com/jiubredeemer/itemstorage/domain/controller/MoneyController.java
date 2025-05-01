package com.jiubredeemer.itemstorage.domain.controller;

import com.jiubredeemer.itemstorage.domain.model.money.MoneyDto;
import com.jiubredeemer.itemstorage.domain.service.MoneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/{roomId}/inventory/{characterId}/money")
@RequiredArgsConstructor
public class MoneyController {
    private final MoneyService moneyService;

    @GetMapping()
    public MoneyDto findMoneyByCharacterId(@PathVariable UUID roomId, @PathVariable UUID characterId) {
        return moneyService.findByCharacterId(roomId, characterId);
    }

    @PatchMapping()
    public MoneyDto changeMoneyCount(@PathVariable UUID roomId, @PathVariable UUID characterId, @RequestBody MoneyDto moneyDto) {
        return moneyService.changeMoneyCount(roomId, characterId, moneyDto);
    }
}
