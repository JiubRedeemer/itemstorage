package com.jiubredeemer.itemstorage.dal.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "itemstorage.license-mode")
public class LicenseMode {
    private Boolean ccBy4;
}
