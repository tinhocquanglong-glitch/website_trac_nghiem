package com.tttn.webthitracnghiem.service;

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
        String input = BLOCKED.matcher(value).replaceAll("")
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

    private static boolean isAllowed(String tag) {
        return "p".equals(tag) || "br".equals(tag) || "strong".equals(tag)
                || "b".equals(tag) || "em".equals(tag) || "i".equals(tag)
                || "u".equals(tag) || "ul".equals(tag) || "ol".equals(tag)
                || "li".equals(tag) || "font".equals(tag);
    }
}
