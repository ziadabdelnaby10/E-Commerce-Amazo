package org.ecommerce.gatewayservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.cloud.gateway.server.webflux.routes[0].id=customer-service",
        "spring.cloud.gateway.server.webflux.routes[0].uri=lb://customer-service",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/v1/customers/**",
        "spring.cloud.gateway.server.webflux.routes[0].filters[0].name=CircuitBreaker",
        "spring.cloud.gateway.server.webflux.routes[0].filters[0].args.name=customerServiceCircuitBreaker",
        "spring.cloud.gateway.server.webflux.routes[0].filters[0].args.fallbackUri=forward:/fallback/customers",
        "spring.cloud.gateway.server.webflux.routes[0].filters[1].name=Retry",
        "spring.cloud.gateway.server.webflux.routes[0].filters[1].args.retries=2",
        "spring.cloud.gateway.server.webflux.routes[0].filters[1].args.methods[0]=GET",
        "spring.cloud.gateway.server.webflux.routes[0].filters[1].args.statuses[0]=BAD_GATEWAY",
        "spring.cloud.gateway.server.webflux.routes[0].filters[1].args.statuses[1]=GATEWAY_TIMEOUT",
        "spring.cloud.gateway.server.webflux.routes[0].filters[1].args.statuses[2]=INTERNAL_SERVER_ERROR"
})
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldExposeCustomerRouteWithLoadBalancerUri() {
        StepVerifier.create(routeLocator.getRoutes().filter(route -> route.getId().equals("customer-service")).next())
                .assertNext(this::assertCustomerRoute)
                .verifyComplete();
    }

    private void assertCustomerRoute(Route route) {
        assertThat(route.getUri()).isEqualTo(URI.create("lb://customer-service"));
        assertThat(route.getPredicate().toString()).contains("/api/v1/customers/**");
        assertThat(route.getFilters())
                .extracting(Object::toString)
                .anySatisfy(filter -> assertThat(filter).contains("CircuitBreaker"))
                .anySatisfy(filter -> assertThat(filter).contains("Retry"));
    }
}


