package org.itmo.shop.infrastructure.vehicleservice;

import org.itmo.shop.infrastructure.vehicleservice.generated.api.VehiclesApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VehicleServiceProperties.class)
class VehicleServiceClientConfiguration {

    static final String SSL_BUNDLE = "vehicle-service";

    @Bean
    VehiclesApi vehiclesApi(RestClient.Builder builder, SslBundles sslBundles, VehicleServiceProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings
                .ofSslBundle(sslBundles.getBundle(SSL_BUNDLE))
                .withTimeouts(properties.connectTimeout(), properties.readTimeout())
                .withRedirects(ClientHttpRequestFactorySettings.Redirects.DONT_FOLLOW);
        RestClient client = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings))
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(VehiclesApi.class);
    }
}
