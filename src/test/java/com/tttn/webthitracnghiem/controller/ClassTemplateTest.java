package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassTemplateTest {
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
    void curriculumClassListDoesNotOfferDuplicateClassCreation() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", Map.of("img", "/img/test.png", "id", "admin"));
        WebContext context = new WebContext(
                request,
                new MockHttpServletResponse(),
                servletContext);
        context.setVariable("classes", new PageImpl<>(List.of()));
        context.setVariable("message", null);

        String html = templateEngine.process("class/list", context);

        assertThat(html)
                .contains("class/list")
                .doesNotContain("/class/create");
    }

    @Test
    void navigationOffersCurriculumMenu() {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("admin", Map.of("img", "/img/test.png", "id", "admin"));
        WebContext context = new WebContext(
                request,
                new MockHttpServletResponse(),
                servletContext);

        String html = templateEngine.process("index", context);

        assertThat(html).contains("/class/list");
    }
}
