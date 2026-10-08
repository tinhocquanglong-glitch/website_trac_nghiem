package com.tttn.webthitracnghiem.model;

import java.text.Normalizer;
import java.util.Locale;

public enum AttendanceStatus {
    FULL("Đầy đủ"),
    ABSENT("Vắng"),
    DROPPED_OUT("Bỏ học"),
    TRANSFERRED("Chuyển trường");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AttendanceStatus fromText(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return null;
        }
        switch (normalized) {
            case "day du":
                return FULL;
            case "vang":
                return ABSENT;
            case "bo hoc":
                return DROPPED_OUT;
            case "chuyen truong":
                return TRANSFERRED;
            default:
                throw new IllegalArgumentException("Trang thai diem danh khong hop le: " + value);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
