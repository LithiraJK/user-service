package com.tripvisito.userservice.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Storage bean configuration.
 *
 * <p>Creates the {@link Storage} client using <b>Application Default Credentials (ADC)</b>.
 */
@Configuration
public class GcpStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(GcpStorageConfig.class);

    @Bean
    public Storage storage() {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            log.info("[GcpStorageConfig] Google Cloud Storage client initialized successfully.");
            return storage;
        } catch (Exception e) {
            log.warn("[GcpStorageConfig] Could not initialize GCP Storage client: {}. " +
                     "Image uploads will fail at runtime. " +
                     "Set GOOGLE_APPLICATION_CREDENTIALS or run on GCP Compute Engine.", e.getMessage());
            return StorageOptions.getDefaultInstance().getService();
        }
    }
}
