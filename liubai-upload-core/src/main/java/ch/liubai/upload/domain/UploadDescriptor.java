package ch.liubai.upload.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated value object identifying one logical upload.
 */
public final class UploadDescriptor {

    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    private final String sha256;
    private final long totalBytes;

    private UploadDescriptor(String sha256, long totalBytes) {
        this.sha256 = sha256.toLowerCase(Locale.ROOT);
        this.totalBytes = totalBytes;
    }

    public static UploadDescriptor of(String sha256, long totalBytes) {
        if (!isValidSha256(sha256)) {
            throw new IllegalArgumentException("sha256必须是64位十六进制字符串");
        }
        if (totalBytes < 0) {
            throw new IllegalArgumentException("totalBytes不能小于0");
        }
        return new UploadDescriptor(sha256, totalBytes);
    }

    public static boolean isValidSha256(String sha256) {
        return sha256 != null && SHA256_PATTERN.matcher(sha256).matches();
    }

    public String getSha256() {
        return sha256;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public String getFileName() {
        return sha256 + "_" + totalBytes;
    }

    public String getTempFileName() {
        return getFileName() + TEMP_FILE_SUFFIX;
    }

    public String getLockKey() {
        return getFileName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UploadDescriptor)) {
            return false;
        }
        UploadDescriptor that = (UploadDescriptor) other;
        return totalBytes == that.totalBytes && sha256.equals(that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha256, totalBytes);
    }
}
