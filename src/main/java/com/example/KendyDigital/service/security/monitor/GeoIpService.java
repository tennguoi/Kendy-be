package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CountryResponse;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Resolves coarse geographic/network metadata for an IP using local MaxMind GeoLite2 databases.
 * Both the country and the optional ASN database are loaded lazily; if the files are missing the
 * service degrades to a safe no-op (no external calls are ever made).
 */
@Service
public class GeoIpService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIpService.class);
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final SecurityMonitorProperties properties;

    private volatile DatabaseReader countryReader;
    private volatile DatabaseReader asnReader;
    private volatile boolean initialized;

    public GeoIpService(SecurityMonitorProperties properties) {
        this.properties = properties;
    }

    public GeoInfo lookup(String ip) {
        if (ip == null || ip.isBlank()) {
            return GeoInfo.unknown();
        }
        if (!IP_LITERAL.matcher(ip).matches()) {
            // Not an IP literal (e.g. "unknown"): avoid a blocking DNS lookup on the request path.
            return GeoInfo.unknown();
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException exception) {
            return GeoInfo.unknown();
        }
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
            return new GeoInfo("LO", "PRIVATE", true);
        }
        ensureInitialized();
        String country = countryCode(address);
        String asn = asnCode(address);
        boolean known = country != null || asn != null;
        return new GeoInfo(country, asn, known);
    }

    public boolean isDatacenter(String ip) {
        String asn = lookup(ip).asn();
        if (asn == null) {
            return false;
        }
        String organization = asnOrganization(ip);
        if (organization == null) {
            return false;
        }
        String normalized = organization.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("amazon") || normalized.contains("google") || normalized.contains("microsoft")
                || normalized.contains("digitalocean") || normalized.contains("ovh") || normalized.contains("hetzner")
                || normalized.contains("linode") || normalized.contains("vultr") || normalized.contains("cloudflare");
    }

    /** Reloads the databases from disk (used after an update) and resets cached readers. */
    public synchronized void reload() {
        closeQuietly(countryReader);
        closeQuietly(asnReader);
        countryReader = null;
        asnReader = null;
        initialized = false;
    }

    public boolean isCountryDatabaseLoaded() {
        ensureInitialized();
        return countryReader != null;
    }

    public boolean isAsnDatabaseLoaded() {
        ensureInitialized();
        return asnReader != null;
    }

    private void closeQuietly(DatabaseReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // nothing we can do while replacing the file
            }
        }
    }

    public boolean isMobileNetwork(String ip) {
        String asn = lookup(ip).asn();
        if (asn == null || properties.getMobileAsns().isEmpty()) {
            return false;
        }
        return properties.getMobileAsns().contains(asn);
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            countryReader = openReader(properties.getGeoLiteDatabasePath(), "country");
            asnReader = openReader(properties.getGeoLiteAsnDatabasePath(), "ASN");
            initialized = true;
        }
    }

    private DatabaseReader openReader(String path, String label) {
        if (!properties.isGeoIpEnabled() || path == null || path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            LOGGER.warn("GeoLite2 {} database not found or unreadable at {}; geo enrichment disabled", label, path);
            return null;
        }
        try {
            return new DatabaseReader.Builder(file).build();
        } catch (IOException exception) {
            LOGGER.warn("Could not open GeoLite2 {} database at {}: {}", label, path, exception.getMessage());
            return null;
        }
    }

    private String countryCode(InetAddress address) {
        DatabaseReader reader = countryReader;
        if (reader == null) {
            return null;
        }
        try {
            CountryResponse response = reader.country(address);
            return response.getCountry() == null ? null : response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception exception) {
            return null;
        }
    }

    private String asnCode(InetAddress address) {
        AsnResponse response = asnResponse(address);
        if (response == null || response.getAutonomousSystemNumber() == null) {
            return null;
        }
        return "AS" + response.getAutonomousSystemNumber();
    }

    private String asnOrganization(String ip) {
        try {
            AsnResponse response = asnResponse(InetAddress.getByName(ip));
            return response == null ? null : response.getAutonomousSystemOrganization();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private AsnResponse asnResponse(InetAddress address) {
        DatabaseReader reader = asnReader;
        if (reader == null) {
            return null;
        }
        try {
            return reader.asn(address);
        } catch (IOException | GeoIp2Exception exception) {
            return null;
        }
    }

    public record GeoInfo(String country, String asn, boolean known) {
        public static GeoInfo unknown() {
            return new GeoInfo(null, null, false);
        }
    }
}
