package com.tripvisito.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Feign client for communicating with the {@code trip-service}.
 *
 * <p>Used by {@link com.tripvisito.userservice.service.StatsService} to fetch
 * trip aggregate stats (total count, monthly count, trend data) for the admin
 * dashboard. The {@code lb://TRIP-SERVICE} URI resolves via Eureka service discovery.
 *
 * <p><b>Contract:</b> trip-service must expose a {@code GET /internal/stats}
 * endpoint that returns:
 * <pre>
 * {
 *   "status": 200,
 *   "data": {
 *     "total": 120,
 *     "currentMonth": 15,
 *     "lastMonth": 20,
 *     "trend": [2, 3, 1, 4, 2, 3, 0]
 *   }
 * }
 * </pre>
 *
 * <p>If trip-service is unavailable, {@link StatsService} catches the Feign
 * exception and returns zeroed-out stats rather than failing the dashboard request.
 */
@FeignClient(name = "TRIP-SERVICE")
public interface TripServiceClient {

    @GetMapping("/internal/stats")
    Map<String, Object> getInternalStats();
}
