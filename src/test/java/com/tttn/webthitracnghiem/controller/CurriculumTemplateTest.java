package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculumTemplateTest {
    private TemplateEngine templateEngine;

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
    }

    @Test
    void editableCurriculumShowsTreeWithEditControlsAndUsage() {
        WebContext context = context(true);

        String html = templateEngine.process("curriculum/index", context);

        assertThat(html)
                .contains("Chủ đề 1. Số tự nhiên", "Bài 1. Tập hợp", "2 câu hỏi · 1 đề")
                .contains("Chương trình của trường", "Bạn được chỉnh sửa")
                .contains("action=\"/curriculum/chapters/5/rename\"", "action=\"/curriculum/chapters/5/lessons\"")
                .contains("action=\"/curriculum/lessons/50/delete\"", "action=\"/curriculum/chapters\"")
                .contains("name=\"subjectClassId\" value=\"10\"");
    }

    @Test
    void readOnlyCurriculumHidesEditControls() {
        WebContext context = context(false);

        String html = templateEngine.process("curriculum/index", context);

        assertThat(html)
                .contains("Chủ đề 1. Số tự nhiên", "Chỉ xem")
                .doesNotContain("/curriculum/chapters/5/rename", "action=\"/curriculum/chapters\"");
    }

    private WebContext context(boolean canEdit) {
        Subject subject = new Subject();
        subject.setId(3);
        subject.setName("Toán");
        Classes grade = new Classes();
        grade.setId(6);
        grade.setGrade(6);
        grade.setNameClass("Lớp 6");
        SubjectClasses subjectClass = new SubjectClasses();
        subjectClass.setId(10);
        subjectClass.setSubject(subject);
        subjectClass.setClasses(grade);
        Chapter chapter = new Chapter();
        chapter.setId(5);
        chapter.setChapterName("Chủ đề 1. Số tự nhiên");
        Lesson lesson = new Lesson();
        lesson.setId(50);
        lesson.setNameLesson("Bài 1. Tập hợp");

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", java.util.Map.of("img", "/img/test.png", "id", "gv"));
        WebContext context = new WebContext(request, new MockHttpServletResponse(), servletContext);
        context.setVariable("grades", List.of(grade));
        context.setVariable("gradeId", 6);
        context.setVariable("subjectClasses", List.of(subjectClass));
        context.setVariable("subjectId", 3);
        context.setVariable("isAdmin", false);
        context.setVariable("schools", List.of());
        context.setVariable("schoolId", 1);
        context.setVariable("schoolName", "Trường A");
        context.setVariable("selected", subjectClass);
        context.setVariable("tree", List.of(new CurriculumController.ChapterNode(chapter,
                List.of(new CurriculumController.LessonNode(lesson, 2, 1)))));
        context.setVariable("canEdit", canEdit);
        context.setVariable("usingShared", false);
        return context;
    }
}
