package org.itmo.shop.infrastructure.vehicleservice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("vehicle-service")
record VehicleServiceProperties(String baseUrl,
                                @DefaultValue("2s") Duration connectTimeout,
                                @DefaultValue("5s") Duration readTimeout) {
}
