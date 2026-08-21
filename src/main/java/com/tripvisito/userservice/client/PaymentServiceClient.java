package com.tripvisito.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Feign client for communicating with the {@code payment-service}.
 *
 * <p>Used by {@link com.tripvisito.userservice.service.StatsService} to fetch
 * the latest payment records for the admin dashboard widget.
 * The {@code lb://PAYMENT-SERVICE} URI resolves via Eureka service discovery.
 *
 * <p><b>Contract:</b> payment-service must expose a {@code GET /internal/stats}
 * endpoint that returns:
 * <pre>
 * {
 *   "status": 200,
 *   "data": {
 *     "latest": [
 *       {
 *         "userId": "...", "userName": "...", "userEmail": "...",
 *         "tripId": "...", "amount": 299.00, "status": "CONFIRMED",
 *         "createdAt": "2026-08-16T10:30:00"
 *       }
 *     ]
 *   }
 * }
 * </pre>
 *
 * <p>If payment-service is unavailable, {@link StatsService} catches the Feign
 * exception and returns an empty payments list.
 */
@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

    @GetMapping("/internal/stats")
    Map<String, Object> getInternalStats();
}
