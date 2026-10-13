package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;

import java.text.Normalizer;
import java.util.Locale;

/** Shared partial-address matching for admin school selectors. */
final class SchoolAddressMatcher {
    private SchoolAddressMatcher() {
    }

    static boolean matches(School school, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isEmpty()) {
            return false;
        }
        return normalize(school.getWardName()).contains(normalizedKeyword)
                || normalize(school.getDistrictName()).contains(normalizedKeyword)
                || normalize(school.getProvinceName()).contains(normalizedKeyword);
    }

    static String format(School school) {
        StringBuilder address = new StringBuilder();
        append(address, school.getWardName());
        append(address, school.getDistrictName());
        append(address, school.getProvinceName());
        return address.length() == 0 ? "Chưa cập nhật địa chỉ" : address.toString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }

    private static void append(StringBuilder address, String value) {
        if (value != null && !value.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(value.trim());
        }
    }
}
