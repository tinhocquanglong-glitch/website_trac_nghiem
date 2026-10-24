package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.service.ai.GeneratedQuestion;
import com.tttn.webthitracnghiem.service.ai.QuestionGenerationRequest;
import com.tttn.webthitracnghiem.service.ai.QuestionGenerationService;
import com.tttn.webthitracnghiem.service.ai.QuestionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionGenerationServiceTest {
    private final LessonPlanLessonRepository planLessonRepository = mock(LessonPlanLessonRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);

    @Test
    void withoutAGeneratorTheFeatureIsUnavailable() {
        QuestionGenerationService service = service(null);

        assertThat(service.isAvailable()).isFalse();
        assertThatThrownBy(() -> service.generateDrafts(1, 5, owner()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void storesValidNewQuestionsAsPendingDraftsOfTheLinkedCurriculumLesson() {
        User owner = owner();
        Lesson curriculumLesson = curriculumLesson();
        LessonPlanLesson planLesson = planLesson(owner, curriculumLesson);
        when(planLessonRepository.findById(7)).thenReturn(Optional.of(planLesson));
        Question existing = new Question();
        existing.setTitle("Tập hợp là gì?");
        when(questionRepository.findByLesson_IdIn(List.of(50))).thenReturn(List.of(existing));
        List<QuestionGenerationRequest> requests = new ArrayList<>();
        QuestionGenerator generator = fakeGenerator(requests, List.of(
                new GeneratedQuestion("Kí hiệu nào chỉ phần tử thuộc tập hợp?", "∈", "∉", "⊂", "=", 1, 1),
                new GeneratedQuestion("  tập hợp LÀ gì? ", "a", "b", "c", "d", 2, 1),   // duplicate of an existing title
                new GeneratedQuestion("Đáp án sai số", "a", "b", "c", "d", 5, 1),       // invalid answer
                new GeneratedQuestion("Câu thiếu phương án", "a", "", "c", "d", 1, 1)));

        int stored = service(generator).generateDrafts(7, 10, owner);

        assertThat(stored).isEqualTo(1);
        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(1)).save(saved.capture());
        Question draft = saved.getValue();
        assertThat(draft.getLesson()).isSameAs(curriculumLesson);
        assertThat(draft.getSource()).isEqualTo(Question.SOURCE_AI);
        assertThat(draft.getReviewStatus()).isEqualTo(Question.REVIEW_PENDING);
        assertThat(draft.isApproved()).isFalse();
        assertThat(draft.getSourcePlanLessonId()).isEqualTo(7);
        assertThat(draft.getAuthor()).isSameAs(owner);

        QuestionGenerationRequest request = requests.get(0);
        assertThat(request.lessonName()).isEqualTo("Bài 1. Tập hợp");
        assertThat(request.grade()).isEqualTo(6);
        assertThat(request.content()).contains("Tập hợp là một khái niệm");
        assertThat(request.easy() + request.medium() + request.hard()).isEqualTo(10);
    }

    @Test
    void allowsUnlinkedLessonsAndRefusesOtherTeachers() {
        User owner = owner();
        LessonPlanLesson unlinked = planLesson(owner, null);
        when(planLessonRepository.findById(7)).thenReturn(Optional.of(unlinked));
        List<QuestionGenerationRequest> requests = new ArrayList<>();
        QuestionGenerator generator = fakeGenerator(requests, List.of(
                new GeneratedQuestion("Câu hỏi từ bài KHBD tự do?", "A", "B", "C", "D", 1, 1)));
        QuestionGenerationService service = service(generator);

        int stored = service.generateDrafts(7, 5, owner);
        assertThat(stored).isEqualTo(1);
        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(saved.capture());
        assertThat(saved.getValue().getLesson()).isNull();
        assertThat(saved.getValue().getLessonPlanLesson()).isSameAs(unlinked);

        User other = new User();
        other.setId("gv-khac");
        assertThatThrownBy(() -> service.generateDrafts(7, 5, other)).isInstanceOf(SecurityException.class);
    }

    private QuestionGenerationService service(QuestionGenerator generator) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (generator != null) beans.addBean("generator", generator);
        ObjectProvider<QuestionGenerator> provider = beans.getBeanProvider(QuestionGenerator.class);
        return new QuestionGenerationService(provider, planLessonRepository, questionRepository);
    }

    private static QuestionGenerator fakeGenerator(List<QuestionGenerationRequest> requests, List<GeneratedQuestion> output) {
        return new QuestionGenerator() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<GeneratedQuestion> generate(QuestionGenerationRequest request) {
                requests.add(request);
                return output;
            }
        };
    }

    private static User owner() {
        User owner = new User();
        owner.setId("gv");
        return owner;
    }

    private static Lesson curriculumLesson() {
        Subject subject = new Subject();
        subject.setName("Toán");
        Classes grade = new Classes();
        grade.setGrade(6);
        SubjectClasses subjectClass = new SubjectClasses();
        subjectClass.setSubject(subject);
        subjectClass.setClasses(grade);
        Chapter chapter = new Chapter();
        chapter.setSubjectClasses(subjectClass);
        chapter.setSchool(new School());
        Lesson lesson = new Lesson();
        lesson.setId(50);
        lesson.setNameLesson("Bài 1. Tập hợp");
        lesson.setChapter(chapter);
        return lesson;
    }

    private static LessonPlanLesson planLesson(User owner, Lesson curriculumLesson) {
        LessonPlan plan = new LessonPlan();
        plan.setOwner(owner);
        LessonPlanChapter chapter = new LessonPlanChapter();
        chapter.setLessonPlan(plan);
        LessonPlanLesson lesson = new LessonPlanLesson();
        lesson.setId(7);
        lesson.setName("Tập hợp");
        lesson.setChapter(chapter);
        lesson.setCurriculumLesson(curriculumLesson);
        lesson.setContent("<p>Tập hợp là một khái niệm cơ bản của toán học.</p>");
        return lesson;
    }
}
