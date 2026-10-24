package com.tttn.webthitracnghiem.service.ai;

import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.service.LessonPlanContentSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns the content of a lesson-plan lesson into draft questions in the question bank.
 *
 * <p>Drafts are filed under the curriculum lesson the plan lesson is linked to, marked
 * {@link Question#SOURCE_AI} and {@link Question#REVIEW_PENDING}; exams only use them after a
 * teacher approves them on the review page.
 */
@Service
public class QuestionGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionGenerationService.class);
    public static final int MAX_QUESTIONS_PER_RUN = 20;

    // Zero beans is the normal state until an AI generator is added.
    private final ObjectProvider<QuestionGenerator> generators;
    private final LessonPlanLessonRepository planLessonRepository;
    private final QuestionRepository questionRepository;

    public QuestionGenerationService(ObjectProvider<QuestionGenerator> generators,
                                     LessonPlanLessonRepository planLessonRepository,
                                     QuestionRepository questionRepository) {
        this.generators = generators;
        this.planLessonRepository = planLessonRepository;
        this.questionRepository = questionRepository;
    }

    public boolean isAvailable() {
        return generator() != null;
    }

    private QuestionGenerator generator() {
        return generators.orderedStream().filter(QuestionGenerator::isAvailable).findFirst().orElse(null);
    }

    /** Plan owner, or the system admin, may generate into the curriculum lesson of the plan's school. */
    public boolean canGenerate(LessonPlan plan, User user) {
        if (plan == null || user == null) return false;
        boolean admin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getRoleName()));
        return admin || (plan.getOwner() != null && plan.getOwner().getId().equals(user.getId()));
    }

    /** Generates and stores draft questions; returns how many were stored. */
    @Transactional
    public int generateDrafts(int planLessonId, int count, User user) {
        QuestionGenerator generator = generator();
        if (generator == null) {
            throw new IllegalStateException("Chức năng sinh câu hỏi bằng AI chưa được cấu hình.");
        }
        LessonPlanLesson planLesson = planLessonRepository.findById(planLessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài trong giáo án."));
        LessonPlan plan = planLesson.getChapter().getLessonPlan();
        if (!canGenerate(plan, user)) {
            throw new SecurityException("Chỉ người soạn giáo án mới được sinh câu hỏi từ giáo án này.");
        }
        Lesson curriculumLesson = planLesson.getCurriculumLesson();
        String text = planLesson.getContentText() != null ? planLesson.getContentText()
                : LessonPlanContentSanitizer.toPlainText(planLesson.getContent());
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Bài \"" + planLesson.getName() + "\" chưa có nội dung để sinh câu hỏi.");
        }
        int wanted = Math.max(1, Math.min(count, MAX_QUESTIONS_PER_RUN));
        int easy = Math.round(wanted * 0.4f);
        int hard = Math.round(wanted * 0.2f);

        String subjectName = "";
        Integer grade = null;
        if (curriculumLesson != null && curriculumLesson.getChapter() != null
                && curriculumLesson.getChapter().getSubjectClasses() != null) {
            SubjectClasses sc = curriculumLesson.getChapter().getSubjectClasses();
            if (sc.getSubject() != null) {
                subjectName = sc.getSubject().getName();
            }
            if (sc.getClasses() != null) {
                grade = sc.getClasses().getGrade();
            }
        }
        if ((subjectName == null || subjectName.isBlank()) && plan != null) {
            if (plan.getSubject() != null) {
                subjectName = plan.getSubject().getName();
            } else if (plan.getSubjectClasses() != null && plan.getSubjectClasses().getSubject() != null) {
                subjectName = plan.getSubjectClasses().getSubject().getName();
            }
        }
        if (grade == null && plan != null) {
            if (plan.getGrade() != null) {
                grade = plan.getGrade().getGrade();
            } else if (plan.getSubjectClasses() != null && plan.getSubjectClasses().getClasses() != null) {
                grade = plan.getSubjectClasses().getClasses().getGrade();
            }
        }

        String lessonTitle = (curriculumLesson != null && curriculumLesson.getNameLesson() != null && !curriculumLesson.getNameLesson().isBlank())
                ? curriculumLesson.getNameLesson() : planLesson.getName();

        QuestionGenerationRequest request = new QuestionGenerationRequest(
                subjectName != null ? subjectName : "",
                grade != null ? grade : 0,
                lessonTitle != null ? lessonTitle : "",
                planLesson.getObjectives(),
                text.length() > QuestionGenerationRequest.MAX_CONTENT_LENGTH
                        ? text.substring(0, QuestionGenerationRequest.MAX_CONTENT_LENGTH) : text,
                wanted, easy, wanted - easy - hard, hard);

        Set<String> seenTitles = new HashSet<>();
        if (curriculumLesson != null) {
            for (Question existing : questionRepository.findByLesson_IdIn(List.of(curriculumLesson.getId()))) {
                seenTitles.add(titleKey(existing.getTitle()));
            }
        }
        for (Question existing : questionRepository.findByLessonPlanLesson_Id(planLesson.getId())) {
            seenTitles.add(titleKey(existing.getTitle()));
        }
        int stored = 0;
        List<GeneratedQuestion> generatedList = generator.generate(request);
        logger.info("AI returned {} questions for plan lesson '{}'", generatedList.size(), planLesson.getName());
        for (GeneratedQuestion generated : generatedList) {
            if (stored >= wanted) {
                break;
            }
            if (!isValid(generated)) {
                logger.warn("Question rejected by isValid: title='{}', answer={}", generated != null ? generated.title() : null, generated != null ? generated.answer() : null);
                continue;
            }
            if (!seenTitles.add(titleKey(generated.title()))) {
                logger.info("Question skipped as duplicate title: '{}'", generated.title());
                continue;
            }
            Question question = new Question();
            question.setTitle(generated.title().trim());
            question.setOptionA(generated.optionA().trim());
            question.setOptionB(generated.optionB().trim());
            question.setOptionC(generated.optionC().trim());
            question.setOptionD(generated.optionD().trim());
            question.setAns(generated.answer());
            question.setLevel(generated.level() >= 1 && generated.level() <= 3 ? generated.level() : 1);
            question.setLesson(curriculumLesson);
            question.setLessonPlanLesson(planLesson);
            question.setSource(Question.SOURCE_AI);
            question.setReviewStatus(Question.REVIEW_PENDING);
            question.setSourcePlanLessonId(planLesson.getId());
            question.setAuthor(user);
            School school = null;
            if (curriculumLesson != null && curriculumLesson.getChapter() != null) {
                school = curriculumLesson.getChapter().getSchool();
            }
            if (school == null && user != null) {
                school = user.getSchool();
            }
            question.setSchool(school);

            if (curriculumLesson != null && curriculumLesson.getChapter() != null && curriculumLesson.getChapter().getSubjectClasses() != null) {
                question.setSubject(curriculumLesson.getChapter().getSubjectClasses().getSubject());
                question.setClasses(curriculumLesson.getChapter().getSubjectClasses().getClasses());
            } else if (plan != null) {
                question.setSubject(plan.getSubject());
                question.setClasses(plan.getGrade());
            }
            question.setIsPublic(false);
            questionRepository.save(question);
            stored++;
        }
        logger.info("Stored {} AI draft questions for plan lesson '{}'", stored, planLesson.getName());
        return stored;
    }

    private static boolean isValid(GeneratedQuestion question) {
        return question != null && notBlank(question.title()) && notBlank(question.optionA())
                && notBlank(question.optionB()) && notBlank(question.optionC()) && notBlank(question.optionD())
                && question.answer() >= 1 && question.answer() <= 4;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String titleKey(String title) {
        return title == null ? "" : title.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
