package com.tttn.webthitracnghiem.service.ai;

/**
 * One generated multiple-choice question.
 *
 * @param answer correct option, 1 = A ... 4 = D (same convention as {@code Question.ans})
 * @param level  1 easy, 2 medium, 3 hard (same convention as {@code Question.level})
 */
public record GeneratedQuestion(String title, String optionA, String optionB, String optionC, String optionD,
                                int answer, int level) {
}
