package com.example.KendyDigital.service.security.monitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeoIpArchiveExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsMmdbFromTarGz() throws Exception {
        byte[] payload = "FAKE-MMDB-CONTENT".getBytes(StandardCharsets.UTF_8);
        byte[] archive = tarGz(
                entry("GeoLite2-Country_20240101/", new byte[0], '5'),
                entry("GeoLite2-Country_20240101/GeoLite2-Country.mmdb", payload, '0'));

        Path target = tempDir.resolve("out/GeoLite2-Country.mmdb");
        String extracted = GeoIpArchiveExtractor.extractMmdb(new ByteArrayInputStream(archive), target);

        assertTrue(extracted.endsWith("GeoLite2-Country.mmdb"), "should report the extracted entry name");
        assertArrayEquals(payload, Files.readAllBytes(target));
    }

    @Test
    void failsWhenNoMmdbEntryExists() throws Exception {
        byte[] archive = tarGz(entry("GeoLite2-Country_20240101/README.txt", "hello".getBytes(), '0'));
        Path target = tempDir.resolve("missing.mmdb");

        assertThrows(IOException.class,
                () -> GeoIpArchiveExtractor.extractMmdb(new ByteArrayInputStream(archive), target));
    }

    private record TarEntry(String name, byte[] content, char type) {
    }

    private static TarEntry entry(String name, byte[] content, char type) {
        return new TarEntry(name, content, type);
    }

    private static byte[] tarGz(TarEntry... entries) throws IOException {
        ByteArrayOutputStream tar = new ByteArrayOutputStream();
        for (TarEntry item : entries) {
            writeEntry(tar, item);
        }
        tar.write(new byte[1024]);
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(gz)) {
            gzip.write(tar.toByteArray());
        }
        return gz.toByteArray();
    }

    private static void writeEntry(ByteArrayOutputStream out, TarEntry item) throws IOException {
        byte[] header = new byte[512];
        writeString(header, 0, item.name());
        writeString(header, 100, "0000644");
        writeString(header, 108, "0000000");
        writeString(header, 116, "0000000");
        writeString(header, 124, String.format("%011o", item.content().length));
        writeString(header, 136, "00000000000");
        header[156] = (byte) item.type();
        writeString(header, 257, "ustar");
        writeString(header, 263, "00");
        for (int i = 148; i < 156; i++) {
            header[i] = (byte) ' ';
        }
        int sum = 0;
        for (byte value : header) {
            sum += value & 0xFF;
        }
        writeString(header, 148, String.format("%06o", sum));
        header[154] = 0;
        header[155] = (byte) ' ';

        out.write(header);
        out.write(item.content());
        int pad = (512 - (item.content().length % 512)) % 512;
        out.write(new byte[pad]);
    }

    private static void writeString(byte[] target, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, target, offset, Math.min(bytes.length, target.length - offset));
    }
}
