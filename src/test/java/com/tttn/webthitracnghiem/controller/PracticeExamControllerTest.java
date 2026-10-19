package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.service.IExamService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PracticeExamControllerTest {

    @Test
    void filtersSubjectsAndChaptersBySelectedGrade() {
        Subject math = subject(1, "Toán");
        Subject english = subject(2, "Tiếng Anh");
        Classes gradeSix = new Classes(6, "legacy", 6, null);
        SubjectClasses mathForGradeSix = new SubjectClasses(10, gradeSix, math, null);
        Chapter chapter = new Chapter(100, "Số nguyên", 1, null, mathForGradeSix);
        Lesson lesson = new Lesson();
        lesson.setChapter(chapter);
        Exam practiceExam = new Exam();
        practiceExam.setId(50);
        practiceExam.setPractice(true);
        practiceExam.setTargetGrade(6);
        practiceExam.setLesson(lesson);
        SubjectClasses englishForGradeSeven = new SubjectClasses(11, new Classes(7, "legacy", 7, null), english, null);
        Chapter englishChapter = new Chapter(101, "Grammar", 1, null, englishForGradeSeven);
        Lesson englishLesson = new Lesson();
        englishLesson.setChapter(englishChapter);
        Exam englishExam = new Exam();
        englishExam.setId(51);
        englishExam.setPractice(true);
        englishExam.setTargetGrade(7);
        englishExam.setLesson(englishLesson);

        IExamService examService = mock(IExamService.class);
        when(examService.getAll()).thenReturn(List.of(practiceExam, englishExam));

        PracticeExamController controller = new PracticeExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);

        ExtendedModelMap model = new ExtendedModelMap();
        controller.practice(6, null, null, null, model);

        assertThat(model.get("subjects")).isEqualTo(List.of(math));
        assertThat(model.get("chapters")).isEqualTo(List.of(chapter));
        assertThat(model.get("grades")).isEqualTo(List.of(6, 7));
    }

    @Test
    void includesLessonPlanSubjectsAndTopicsInPracticeFilters() {
        Subject physics = subject(3, "Vật lý");
        Classes gradeTen = new Classes(10, "grade-10", 10, null);
        LessonPlan plan = new LessonPlan();
        plan.setSubject(physics);
        plan.setGrade(gradeTen);

        LessonPlanChapter topic = new LessonPlanChapter();
        topic.setId(300);
        topic.setName("Động học");
        topic.setLessonPlan(plan);
        LessonPlanLesson lesson = new LessonPlanLesson();
        lesson.setId(301);
        lesson.setName("Chuyển động thẳng");
        lesson.setChapter(topic);
        topic.setLessons(List.of(lesson));
        plan.setPlanChapters(List.of(topic));

        Exam practiceExam = new Exam();
        practiceExam.setId(52);
        practiceExam.setPractice(true);
        practiceExam.setTargetGrade(10);
        practiceExam.setLessonPlanLesson(lesson);

        IExamService examService = mock(IExamService.class);
        when(examService.getAll()).thenReturn(List.of(practiceExam));

        PracticeExamController controller = new PracticeExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);

        ExtendedModelMap model = new ExtendedModelMap();
        controller.practice(10, null, null, null, model);

        assertThat(model.get("subjects")).isEqualTo(List.of(physics));
        @SuppressWarnings("unchecked")
        List<Chapter> topics = (List<Chapter>) model.get("chapters");
        assertThat(topics).hasSize(1);
        assertThat(topics.get(0).getChapterName()).isEqualTo("Động học");
        assertThat(model.get("grades")).isEqualTo(List.of(10));
    }

    @Test
    void practiceTemplateRendersLessonPlanTopicMetadata() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/practice/list.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("exam.lessonPlanLesson.chapter.lessonPlan.subject.name")
                .contains("exam.lessonPlanLesson.chapter.name")
                .contains("Chủ đề");
    }

    @Test
    void excludesPracticeExamThatIsNotPublic() {
        Exam publicExam = new Exam();
        publicExam.setId(60);
        publicExam.setPractice(true);
        publicExam.setIsPublic(true);
        Exam privateExam = new Exam();
        privateExam.setId(61);
        privateExam.setPractice(true);
        privateExam.setIsPublic(false);

        IExamService examService = mock(IExamService.class);
        when(examService.getAll()).thenReturn(List.of(publicExam, privateExam));
        PracticeExamController controller = new PracticeExamController();
        ReflectionTestUtils.setField(controller, "examService", examService);

        ExtendedModelMap model = new ExtendedModelMap();
        controller.practice(null, null, null, null, model);

        assertThat(model.get("practiceExams")).isEqualTo(List.of(publicExam));
    }

    @Test
    void practiceTemplateIncludesBothRegularAssessmentTypes() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/practice/list.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("value=\"REGULAR_1\"")
                .contains("value=\"REGULAR_2\"")
                .contains("scoreType");
    }

    private Subject subject(Integer id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }
}
