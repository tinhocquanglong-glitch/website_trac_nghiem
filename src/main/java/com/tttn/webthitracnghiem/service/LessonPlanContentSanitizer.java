package com.tttn.webthitracnghiem.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the small HTML subset produced by the lesson editor. */
public final class LessonPlanContentSanitizer {
    private static final Pattern BLOCKED = Pattern.compile(
            "(?is)<(script|style|iframe|object|embed|form|svg|math)[^>]*>.*?</\\1>");
    private static final Pattern TAG = Pattern.compile("(?is)<(/?)([a-z0-9]+)([^>]*)>");
    private static final Pattern FONT_SIZE = Pattern.compile(
            "(?i)\\bsize\\s*=\\s*['\"]?(7[0-2]|[1-6][0-9]|[1-9])['\"]?");

    private LessonPlanContentSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String input = BLOCKED.matcher(normalizeUnicode(value)).replaceAll("")
                .replaceAll("(?i)javascript\\s*:", "");
        Matcher matcher = TAG.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String replacement = "";
            if (isAllowed(tag)) {
                if ("br".equals(tag)) {
                    replacement = "<br>";
                } else if ("font".equals(tag)) {
                    Matcher size = FONT_SIZE.matcher(matcher.group(3));
                    replacement = size.find()
                            ? "<font size=\"" + size.group(1) + "\">"
                            : "<span>";
                    if (!matcher.group(1).isEmpty()) replacement = "</font>";
                } else {
                    replacement = "<" + matcher.group(1) + tag + ">";
                }
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString().trim();
    }

    /** Removes malformed UTF-16 and database-hostile control characters from imported HTML. */
    private static String normalizeUnicode(String value) {
        StringBuilder clean = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isHighSurrogate(current)) {
                if (i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                    clean.append(current).append(value.charAt(++i));
                }
                continue;
            }
            if (Character.isLowSurrogate(current)) continue;
            if (current == '\t' || current == '\n' || current == '\r'
                    || (!Character.isISOControl(current) && !isNoncharacter(current))) {
                clean.append(current);
            }
        }
        return Normalizer.normalize(clean, Normalizer.Form.NFC);
    }

    private static boolean isNoncharacter(int codePoint) {
        int low = codePoint & 0xFFFF;
        return (low >= 0xFDD0 && low <= 0xFDEF) || low == 0xFFFE || low == 0xFFFF;
    }

    /**
     * Readable plain text of sanitized lesson HTML: one line per paragraph or list item.
     * Stored next to the HTML for search and as input for question generation.
     */
    public static String toPlainText(String html) {
        if (html == null || html.trim().isEmpty()) return null;
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|li|ul|ol)>", "\n")
                .replaceAll("(?i)<li>", "- ")
                .replaceAll("(?s)<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
        StringBuilder result = new StringBuilder();
        for (String line : text.split("\n")) {
            String cleaned = line.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
            if (!cleaned.isEmpty()) result.append(cleaned).append('\n');
        }
        return result.length() == 0 ? null : result.toString().trim();
    }

    private static boolean isAllowed(String tag) {
        return "p".equals(tag) || "br".equals(tag) || "strong".equals(tag)
                || "b".equals(tag) || "em".equals(tag) || "i".equals(tag)
                || "u".equals(tag) || "ul".equals(tag) || "ol".equals(tag)
                || "li".equals(tag) || "font".equals(tag);
    }
}
