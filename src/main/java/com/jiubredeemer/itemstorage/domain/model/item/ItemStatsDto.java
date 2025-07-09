package com.jiubredeemer.itemstorage.domain.model.item;

import lombok.Data;

import java.util.UUID;

@Data
public class ItemStatsDto {
    private UUID id;
    private UUID itemId;
    private StatsDto armoryClass;
    private StatsDto speed;
    private StatsDto hp;
    private StatsDto savingThrow;
    private StatsDto adventage;
    private StatsDto interference;
    private StatsDto resistance;
    private StatsDto vulnerability;
    private StatsDto immunity;
    private StatsDto ability_score;
    private StatsDto proficiency_skill;
    private StatsDto attack_bonus;
    private StatsDto damage_bonus;
    private StatsDto limitation;
}
