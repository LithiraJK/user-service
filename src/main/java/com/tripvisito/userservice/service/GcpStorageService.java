package com.tripvisito.userservice.service;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Google Cloud Storage service for profile picture uploads.
 */
@Service
public class GcpStorageService {

    private static final Logger log = LoggerFactory.getLogger(GcpStorageService.class);

    private final Storage storage;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    public GcpStorageService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Uploads a file to the GCP bucket and returns the publicly accessible URL.
     *
     * @param originalFileName the original file name from the upload (used for extension)
     * @param content          the raw file bytes
     * @param contentType      MIME type (e.g. {@code image/jpeg}, {@code image/png})
     * @return public URL in the format
     *         {@code https://storage.googleapis.com/{bucket}/profiles/{uuid}_{fileName}}
     * @throws RuntimeException if the upload fails
     */
    public String uploadFile(String originalFileName, byte[] content, String contentType) {
        // Sanitise filename — strip path separators and special chars
        String safeName = sanitizeFileName(originalFileName);
        String objectName = "profiles/" + UUID.randomUUID() + "_" + safeName;

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try {
            // Upload the file bytes
            storage.create(blobInfo, content);

            // Make the object publicly readable (commented out as uniform bucket-level access is enabled)
            // storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

            String publicUrl = String.format(
                    "https://storage.googleapis.com/%s/%s", bucketName, objectName);

            log.info("[GcpStorageService] Uploaded profile picture: {} → {}", safeName, publicUrl);
            return publicUrl;

        } catch (Exception e) {
            log.error("[GcpStorageService] Upload failed for '{}': {}", safeName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to GCP Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from the GCS bucket by its public URL.
     *
     * @param publicUrl the full public URL of the object to delete
     */
    public void deleteFile(String publicUrl) {
        try {
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";
            if (!publicUrl.startsWith(prefix)) {
                log.warn("[GcpStorageService] Skipping delete — URL not from this bucket: {}", publicUrl);
                return;
            }
            String objectName = publicUrl.substring(prefix.length());
            boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
            if (deleted) {
                log.info("[GcpStorageService] Deleted profile picture: {}", objectName);
            } else {
                log.warn("[GcpStorageService] Object not found for deletion: {}", objectName);
            }
        } catch (Exception e) {
            log.error("[GcpStorageService] Delete failed for '{}': {}", publicUrl, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "image.jpg";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
