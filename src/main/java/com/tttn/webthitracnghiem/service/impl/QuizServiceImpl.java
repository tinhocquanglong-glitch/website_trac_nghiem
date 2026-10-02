package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.model.QuestionForm;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.repository.ResultRepository;
import com.tttn.webthitracnghiem.service.IQuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class QuizServiceImpl implements IQuizService {
    @Autowired
    Question question;
    @Autowired
    QuestionForm qForm;
    @Autowired
    QuestionRepository qRepo;
    @Autowired
    Result result;
    @Autowired
    ResultRepository rRepo;

    @Override
    public QuestionForm getQuestions() {
        List<Question> allQues = qRepo.findAll();
        List<Question> qList = new ArrayList<Question>();
        Random random = new Random();
        // Random đề thi
        for (int i = 0; i < 15; i++) {
            int rand = random.nextInt(allQues.size());
            qList.add(allQues.get(rand));
            allQues.remove(rand);
        }

        qForm.setQuestions(qList);

        return qForm;
    }

    @Override
    public QuestionForm getQuestionsBySubAndClass(int sub, int cla, int num) {
        List<Question> allQues = qRepo.findBySubAndClass(sub,cla);
        List<Question> qList = new ArrayList<>();
        Random random = new Random();
        // Random đề thi
        for (int i = 0; i < num; i++) {
            int rand = random.nextInt(allQues.size());
            qList.add(allQues.get(rand));
            allQues.remove(rand);
        }

        qForm.setQuestions(qList);

        return qForm;
    }

    @Override
    public QuestionForm getQuestions(int id) {
        List<Question> allQues = qRepo.findAllId(id);
//        List<Question> qList = new ArrayList<Question>();
//        int fag = 10;
//        Random random = new Random();
//        if (allQues.size() < 10) {
//            fag = allQues.size();
//        }
//        for (int i = 0; i < fag; i++) {
//            int rand = random.nextInt(allQues.size());
//            qList.add(allQues.get(rand));
//            allQues.remove(rand);
//        }
        qForm.setQuestions(allQues);
        return qForm;
    }

    @Override
    public double getResult(QuestionForm qForm) {
        int correct = 0;
        int totalQuestion = qForm.getQuestions().size();
        for (Question q : qForm.getQuestions())
            if (q.getAns() == q.getChose())
                correct++;
        return 1.0*(correct * 10) / totalQuestion;
    }

    @Override
    public void saveScore(Result result) {

//        Result saveResult = new Result();
//        saveResult.setUsername(result.getUsername());
//        saveResult.setTotalCorrect(result.getTotalCorrect());
//        rRepo.save(saveResult);
        result.getUsers().setResult(null);
        result.getExam().setUsers(null);
        rRepo.save(result);

    }

    @Override
    public void saveScoreV2(Result result) {
//        Result saveResult = new Result();
//        saveResult.setUsername(result.getUsername());
//        saveResult.setTotalCorrect(result.getTotalCorrect());
//        rRepo.save(saveResult);
        //result.getQuestions().setUsers(null);
        result.getUsers().setResult(null);
        rRepo.save(result);
    }

    @Override
    public List<Result> getTopScore() {
        List<Result> sList = rRepo.findAll(Sort.by(Sort.Direction.DESC, "mark"));
        return sList;
    }

    @Override
    public List<Result> getTopScoreByExam(int idExam) {
        List<Result> sList = rRepo.findTopByExam(idExam);
        return sList;
    }
}
