package com.tttn.webthitracnghiem.service.ai;

/**
 * Input for {@link QuestionGenerator}: where the lesson sits in the curriculum, what students
 * should learn, and the lesson-plan text to draw questions from.
 *
 * @param subjectName  e.g. "Toán"
 * @param grade        grade number 1-12
 * @param lessonName   curriculum lesson name, e.g. "Bài 3. Phép cộng và phép trừ"
 * @param objectives   lesson objectives from the plan, may be null
 * @param content      plain text of the lesson plan, already shortened to {@link #MAX_CONTENT_LENGTH}
 * @param count        number of questions wanted
 * @param easy         how many at level 1 (nhận biết)
 * @param medium       how many at level 2 (thông hiểu)
 * @param hard         how many at level 3 (vận dụng)
 */
public record QuestionGenerationRequest(String subjectName, Integer grade, String lessonName, String objectives,
                                        String content, int count, int easy, int medium, int hard) {
    public static final int MAX_CONTENT_LENGTH = 8000;
}
