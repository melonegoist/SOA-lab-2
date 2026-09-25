package org.itmo.shop.infrastructure.config;

import org.itmo.shop.application.ShopService;
import org.itmo.shop.domain.VehicleCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ApplicationWiring {

    @Bean
    ShopService shopService(VehicleCatalog vehicles) {
        return new ShopService(vehicles);
    }
}
