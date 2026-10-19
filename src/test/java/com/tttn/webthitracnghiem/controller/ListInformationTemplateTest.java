package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListInformationTemplateTest {
    private TemplateEngine templateEngine;
    private MockServletContext servletContext;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        templateEngine = engine;
        servletContext = new MockServletContext();
    }

    @Test
    void rendersPracticeResultLinkedToLessonPlanWithoutLegacyLesson() {
        Subject subject = new Subject();
        subject.setName("Toán");
        Classes grade = new Classes();
        grade.setGrade(6);
        LessonPlan plan = new LessonPlan();
        plan.setSubject(subject);
        plan.setGrade(grade);
        LessonPlanChapter topic = new LessonPlanChapter();
        topic.setLessonPlan(plan);
        topic.setName("Phân số");
        LessonPlanLesson lesson = new LessonPlanLesson();
        lesson.setChapter(topic);
        lesson.setName("Khái niệm phân số");

        Exam exam = new Exam();
        exam.setIdName("MD-0001");
        exam.setLessonPlanLesson(lesson);

        User student = new User();
        student.setFullName("Nguyễn Văn A");
        Result result = new Result();
        result.setMark(8.5);
        result.setEndTime(new Timestamp(0));
        result.setUsers(student);
        result.setExam(exam);

        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        WebContext context = new WebContext(request, new MockHttpServletResponse(), servletContext);
        context.setVariables(Map.of("sList", List.of(result)));

        String html = templateEngine.process("statistical/ListInformation", context);

        assertThat(html).contains("MD-0001", "8,5");
    }
}
