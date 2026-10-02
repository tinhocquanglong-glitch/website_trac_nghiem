package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.QuestionForm;
import com.tttn.webthitracnghiem.model.Result;

import java.util.List;

public interface IQuizService {
    QuestionForm getQuestions();
    QuestionForm getQuestionsBySubAndClass(int sub, int cla, int num);
    QuestionForm getQuestions(int id);
    double getResult(QuestionForm qForm);
    void saveScore(Result result);
    void saveScoreV2(Result result);
    List<Result> getTopScore();
    List<Result> getTopScoreByExam(int idExam);
}
