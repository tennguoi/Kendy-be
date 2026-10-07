package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Downloads and refreshes the GeoLite2 Country / ASN databases from MaxMind. Requires a (free)
 * MaxMind license key and target file paths. Runs weekly by default and can be triggered manually
 * from the admin API. Fully opt-in: does nothing unless enabled and a license key is present.
 */
@Component
public class GeoIpDatabaseUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIpDatabaseUpdater.class);
    private static final String DOWNLOAD_URL = "https://download.maxmind.com/app/geoip_download";
    private static final String COUNTRY_EDITION = "GeoLite2-Country";
    private static final String ASN_EDITION = "GeoLite2-ASN";

    private final SecurityMonitorProperties properties;
    private final GeoIpService geoIpService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public GeoIpDatabaseUpdater(SecurityMonitorProperties properties, GeoIpService geoIpService) {
        this.properties = properties;
        this.geoIpService = geoIpService;
    }

    @Scheduled(cron = "${app.security.monitor.geo-ip-update-cron:0 0 4 * * SUN}")
    public void scheduledUpdate() {
        if (!properties.isGeoIpAutoUpdateEnabled()) {
            return;
        }
        try {
            UpdateResult result = updateAll();
            LOGGER.info("Scheduled GeoLite2 update: updated={} message={}", result.updated(), result.message());
        } catch (RuntimeException exception) {
            LOGGER.warn("Scheduled GeoLite2 update failed", exception);
        }
    }

    /** Downloads both editions (when their paths are configured) and reloads the readers. */
    public UpdateResult updateAll() {
        String licenseKey = properties.getMaxmindLicenseKey();
        if (licenseKey == null || licenseKey.isBlank()) {
            return new UpdateResult(false, "MaxMind license key is not configured (MAXMIND_LICENSE_KEY)");
        }
        if (!running.compareAndSet(false, true)) {
            return new UpdateResult(false, "A GeoLite2 update is already in progress");
        }
        try {
            List<String> updated = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            downloadEdition(COUNTRY_EDITION, licenseKey, properties.getGeoLiteDatabasePath(), updated, failed);
            downloadEdition(ASN_EDITION, licenseKey, properties.getGeoLiteAsnDatabasePath(), updated, failed);
            if (!updated.isEmpty()) {
                geoIpService.reload();
            }
            String message = "updated=" + updated + "; failed=" + failed;
            if (updated.isEmpty() && !failed.isEmpty()) {
                return new UpdateResult(false, message);
            }
            return new UpdateResult(!updated.isEmpty(), message);
        } finally {
            running.set(false);
        }
    }

    private void downloadEdition(String editionId, String licenseKey, String targetPath,
            List<String> updated, List<String> failed) {
        if (targetPath == null || targetPath.isBlank()) {
            return;
        }
        Path target = Path.of(targetPath);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            String url = DOWNLOAD_URL + "?edition_id=" + editionId
                    + "&license_key=" + URLEncoder.encode(licenseKey, StandardCharsets.UTF_8)
                    + "&suffix=tar.gz";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(3))
                    .header("Accept", "application/gzip")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                drain(response.body());
                failed.add(editionId + ":" + response.statusCode());
                LOGGER.warn("GeoLite2 {} download returned HTTP {}", editionId, response.statusCode());
                return;
            }
            try (InputStream body = response.body()) {
                GeoIpArchiveExtractor.extractMmdb(body, temp);
            }
            // Close any memory-mapped readers first so the file can be replaced on Windows.
            geoIpService.reload();
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            updated.add(editionId);
            LOGGER.info("GeoLite2 {} database updated at {}", editionId, target);
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            failed.add(editionId + ":" + exception.getMessage());
            LOGGER.warn("GeoLite2 {} update failed: {}", editionId, exception.getMessage());
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best effort cleanup
            }
        }
    }

    private void drain(InputStream body) {
        if (body == null) {
            return;
        }
        try (InputStream in = body) {
            in.readAllBytes();
        } catch (IOException ignored) {
            // nothing to do
        }
    }

    public boolean isAutoUpdateEnabled() {
        return properties.isGeoIpAutoUpdateEnabled();
    }

    public record UpdateResult(boolean updated, String message) {
    }
}
