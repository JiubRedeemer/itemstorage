package com.jiubredeemer.itemstorage.domain.model.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemParams {
    private String searchQuery;
    private Integer limit;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime lastSeenCreatedAt;
    private UUID lastSeenId;
    private String ruleType; //2024, 2014
    private String type;
    private String subtype;
    private String rarity;
    private List<String> tags;
    private Boolean customization;
    private Boolean hasSkills;
}
