package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links topics and lessons parsed from a teacher's lesson-plan file to the school curriculum.
 *
 * <p>A lesson matches first by its number ("Bài 3" ↔ "Bài 3. ..."), since textbooks number
 * lessons across the whole grade, then by name similarity once numbering prefixes and Vietnamese
 * diacritics are removed. Topics match the same way against chapters. Nothing here touches the
 * database; unmatched items are left unlinked for the teacher to decide.
 */
public final class CurriculumMatcher {
    static final double NAME_THRESHOLD = 0.6;

    private static final Pattern NUMBERED = Pattern.compile(
            "^(bai|tiet|chu de|chuyen de|chuong|phan)\\s*([0-9]+|[ivxlc]+)\\b");
    private static final Pattern PREFIX = Pattern.compile(
            "^(bai|tiet|chu de|chuyen de|chuong|phan)\\s*([0-9]+|[ivxlc]+)\\b\\s*[:.\\-–)]*\\s*");

    private CurriculumMatcher() {
    }

    /** A curriculum chapter or lesson as the matcher sees it. */
    public record Node(int id, String name, List<Node> children) {
    }

    public static void link(List<LessonPlanChapterRequest> planChapters, List<Node> curriculum) {
        if (planChapters == null || curriculum == null || curriculum.isEmpty()) {
            return;
        }
        List<Node> allLessons = new ArrayList<>();
        curriculum.forEach(chapter -> allLessons.addAll(chapter.children()));
        for (LessonPlanChapterRequest planChapter : planChapters) {
            Node chapter = bestMatch(planChapter.getName(), curriculum);
            if (chapter != null) {
                planChapter.setCurriculumChapterId(chapter.id());
            }
            for (LessonPlanLessonRequest planLesson : planChapter.getLessons()) {
                Node lesson = chapter == null ? null : bestMatch(planLesson.getName(), chapter.children());
                if (lesson == null) {
                    lesson = bestMatch(planLesson.getName(), allLessons);
                }
                if (lesson != null) {
                    planLesson.setCurriculumLessonId(lesson.id());
                    if (planChapter.getCurriculumChapterId() == null) {
                        planChapter.setCurriculumChapterId(chapterOf(lesson, curriculum));
                    }
                }
            }
        }
    }

    static Node bestMatch(String name, List<Node> candidates) {
        if (name == null || candidates == null) {
            return null;
        }
        String normalized = normalize(name);
        String key = numberKey(normalized);
        if (key != null) {
            for (Node candidate : candidates) {
                if (key.equals(numberKey(normalize(candidate.name())))) {
                    return candidate;
                }
            }
        }
        Node best = null;
        double bestScore = NAME_THRESHOLD;
        String stripped = stripPrefix(normalized);
        for (Node candidate : candidates) {
            double score = similarity(stripped, stripPrefix(normalize(candidate.name())));
            if (score >= bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static Integer chapterOf(Node lesson, List<Node> curriculum) {
        for (Node chapter : curriculum) {
            if (chapter.children().contains(lesson)) {
                return chapter.id();
            }
        }
        return null;
    }

    /** Lower case, no diacritics (đ → d), punctuation turned into spaces, single spaces. */
    static String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^a-z0-9:.\\-–) ]", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Kind and number of a numbered name, e.g. "bai#3" or "topic#2" for "Chương II". Lessons
     * ("Bài") and periods ("Tiết") are different numberings, so they never match each other.
     */
    static String numberKey(String normalized) {
        Matcher matcher = NUMBERED.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(2);
        Integer number = Character.isDigit(raw.charAt(0)) ? Integer.valueOf(raw) : roman(raw);
        if (number == null) {
            return null;
        }
        String kind = matcher.group(1);
        String group = "bai".equals(kind) || "tiet".equals(kind) ? kind : "topic";
        return group + "#" + number;
    }

    static String stripPrefix(String normalized) {
        return PREFIX.matcher(normalized).replaceFirst("").replaceAll("[:.\\-–)]", " ").replaceAll("\\s+", " ").trim();
    }

    /** Jaccard similarity of the word sets. */
    static double similarity(String a, String b) {
        Set<String> left = new HashSet<>(Arrays.asList(a.split(" ")));
        Set<String> right = new HashSet<>(Arrays.asList(b.split(" ")));
        left.remove("");
        right.remove("");
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        left.retainAll(right);
        return (double) left.size() / union.size();
    }

    private static Integer roman(String value) {
        int total = 0;
        int previous = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            int digit = switch (value.charAt(i)) {
                case 'i' -> 1;
                case 'v' -> 5;
                case 'x' -> 10;
                case 'l' -> 50;
                case 'c' -> 100;
                default -> 0;
            };
            total += digit < previous ? -digit : digit;
            previous = Math.max(previous, digit);
        }
        return total == 0 ? null : total;
    }
}
