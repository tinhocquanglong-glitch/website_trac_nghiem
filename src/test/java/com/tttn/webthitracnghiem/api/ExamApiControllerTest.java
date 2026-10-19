package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.service.IExamService;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.WebDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.List;
import java.sql.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class ExamApiControllerTest {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void multipartCreateReturnsSmallResponseInsteadOfSerializingJpaGraph() {
        ExamApiController controller = new ExamApiController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "examService", mock(IExamService.class));

        Exam exam = new Exam();
        exam.setId(42);

        ResponseEntity<?> response = controller.save(
                exam, null, null, null, null, null,
                null, null, null, null, false);

        assertThat(response.getBody()).isInstanceOf(Map.class);
        assertThat(response.getBody()).isEqualTo(Map.of("id", 42));
        assertThat(response.getBody()).isNotInstanceOf(Exam.class);
    }

    @Test
    void getReturnsSummariesInsteadOfJpaEntities() {
        ExamApiController controller = new ExamApiController();
        IExamService service = mock(IExamService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "examService", service);

        Exam exam = new Exam();
        exam.setId(7);
        exam.setIdName("MD-0007");
        when(service.getAll()).thenReturn(List.of(exam));

        ResponseEntity<?> response = controller.get();

        assertThat(response.getBody()).isInstanceOf(List.class);
        List<?> summaries = (List<?>) response.getBody();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0)).isInstanceOf(ExamApiController.ExamSummary.class);
        ExamApiController.ExamSummary summary = (ExamApiController.ExamSummary) summaries.get(0);
        assertThat(summary.getId()).isEqualTo(7);
        assertThat(summary.getIdName()).isEqualTo("MD-0007");
    }

    @Test
    void updatePreservesExistingExamDatesWhenEditFormOmitsThem() {
        ExamApiController controller = new ExamApiController();
        IExamService service = mock(IExamService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "examService", service);

        Exam existing = new Exam();
        existing.setId(12);
        existing.setCreateDate(Date.valueOf("2026-10-01"));
        existing.setLastModify(Date.valueOf("2026-10-10"));
        when(service.findById(12)).thenReturn(existing);

        Exam edit = new Exam();
        edit.setId(12);
        edit.setIdName("MD-0012");
        edit.setNameExam("De thi da chinh sua");
        controller.save(edit, null, null, null, null, null,
                null, null, null, null, false);

        assertThat(edit.getCreateDate()).isEqualTo(existing.getCreateDate());
        assertThat(edit.getLastModify()).isEqualTo(existing.getLastModify());
        verify(service).save(edit);
    }

    @Test
    void emptyDateFieldsAreConvertedToNullBeforeExamBinding() {
        ExamApiController controller = new ExamApiController();
        Exam target = new Exam();
        WebDataBinder binder = new WebDataBinder(target, "exam");

        controller.trimEmptyExamFields(binder);
        binder.bind(new MutablePropertyValues(Map.of(
                "createDate", "",
                "lastModify", ""
        )));

        assertThat(binder.getBindingResult().hasErrors()).isFalse();
        assertThat(target.getCreateDate()).isNull();
        assertThat(target.getLastModify()).isNull();
    }
}
