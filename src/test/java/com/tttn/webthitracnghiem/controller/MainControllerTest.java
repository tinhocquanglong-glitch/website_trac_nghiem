package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.QuestionForm;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.IExamService;
import com.tttn.webthitracnghiem.service.IQuizService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainControllerTest {

    @Test
    void submitRendersThePersistedAttemptOnResultPage() {
        IQuizService quizService = mock(IQuizService.class);
        IExamService examService = mock(IExamService.class);
        Exam exam = new Exam();
        exam.setId(3);
        when(examService.findById(3)).thenReturn(exam);
        when(quizService.getResult(any(QuestionForm.class))).thenReturn(8.5);

        User student = new User();
        student.setFullName("Nguyen Van A");
        Result attempt = new Result();
        attempt.setUsers(student);
        attempt.setExam(exam);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("examResult", attempt);
        session.setAttribute("examSubmitted", false);

        MainController controller = new MainController();
        ReflectionTestUtils.setField(controller, "qService", quizService);
        ReflectionTestUtils.setField(controller, "examService", examService);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.submit1(3, new QuestionForm(), model, session);

        assertThat(view).isEqualTo("exam/resultTest");
        assertThat(model.get("result")).isSameAs(attempt);
        verify(quizService).saveScore(attempt);
    }
}
