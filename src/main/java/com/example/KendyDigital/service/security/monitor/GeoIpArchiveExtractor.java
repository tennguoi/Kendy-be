package com.example.KendyDigital.service.security.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Minimal reader for the MaxMind GeoLite2 distribution format (a gzip-compressed USTAR archive
 * containing a single {@code .mmdb} file). Implemented without extra dependencies.
 */
public final class GeoIpArchiveExtractor {
    private static final int BLOCK = 512;
    private static final String MMDB_SUFFIX = ".mmdb";

    private GeoIpArchiveExtractor() {
    }

    /**
     * Extracts the first {@code .mmdb} file from a {@code .tar.gz} stream into {@code target},
     * creating parent directories as needed.
     *
     * @return the archive entry name that was extracted.
     */
    public static String extractMmdb(InputStream tarGz, Path target) throws IOException {
        if (tarGz == null) {
            throw new IOException("Archive stream is null");
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try (GZIPInputStream gzip = new GZIPInputStream(tarGz)) {
            byte[] header = new byte[BLOCK];
            while (readFully(gzip, header)) {
                if (isZeroBlock(header)) {
                    break;
                }
                String name = readString(header, 0, 100);
                String prefix = readString(header, 345, 155);
                String fullName = prefix.isEmpty() ? name : prefix + "/" + name;
                long size = parseOctal(header, 124, 12);
                char type = (char) header[156];

                boolean regularFile = type == '0' || type == 0 || type == '\0';
                if (regularFile && fullName.toLowerCase(java.util.Locale.ROOT).endsWith(MMDB_SUFFIX)) {
                    try (OutputStream out = Files.newOutputStream(target)) {
                        copyN(gzip, out, size);
                    }
                    return fullName;
                }
                skipN(gzip, size);
                skipN(gzip, (BLOCK - (size % BLOCK)) % BLOCK);
            }
        }
        throw new IOException("No " + MMDB_SUFFIX + " entry found in archive");
    }

    private static boolean readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read == -1) {
                return false;
            }
            offset += read;
        }
        return true;
    }

    private static boolean isZeroBlock(byte[] block) {
        for (byte value : block) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static String readString(byte[] block, int offset, int length) {
        int end = offset;
        int limit = Math.min(offset + length, block.length);
        while (end < limit && block[end] != 0) {
            end++;
        }
        return new String(block, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private static long parseOctal(byte[] block, int offset, int length) {
        long value = 0;
        int limit = Math.min(offset + length, block.length);
        for (int i = offset; i < limit; i++) {
            int digit = block[i] & 0xFF;
            if (digit == 0 || digit == ' ') {
                continue;
            }
            if (digit < '0' || digit > '7') {
                break;
            }
            value = (value << 3) + (digit - '0');
        }
        return value;
    }

    private static void copyN(InputStream in, OutputStream out, long count) throws IOException {
        byte[] buffer = new byte[8192];
        long remaining = count;
        while (remaining > 0) {
            int toRead = (int) Math.min(buffer.length, remaining);
            int read = in.read(buffer, 0, toRead);
            if (read == -1) {
                throw new IOException("Unexpected end of archive while extracting entry");
            }
            out.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private static void skipN(InputStream in, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                if (in.read() == -1) {
                    return;
                }
                remaining--;
            } else {
                remaining -= skipped;
            }
        }
    }
}
