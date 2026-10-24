package com.tttn.webthitracnghiem.service.ai;

import java.util.List;

/**
 * Generates multiple-choice questions from the content of one lesson.
 *
 * <p>No implementation ships with the application yet. To plug in an AI model, add a Spring bean
 * implementing this interface (for example one that calls the Claude API with a key read from an
 * environment variable). {@link QuestionGenerationService} picks it up, stores its output in the
 * question bank as questions pending review, and teachers approve them before exams can use them.
 */
public interface QuestionGenerator {

    /** False while the generator is not configured (e.g. no API key); the UI then hides the action. */
    boolean isAvailable();

    /** Questions for the request; may return fewer than asked. Output is validated by the caller. */
    List<GeneratedQuestion> generate(QuestionGenerationRequest request);
}
