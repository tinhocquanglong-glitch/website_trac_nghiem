package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Question;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.service.IQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionServiceImpl implements IQuestionService {
    @Autowired
    private QuestionRepository questionRepository;

    @Override
    public Page<Question> findAll(Pageable pageable) {
        return questionRepository.findAll(pageable);
    }

    @Override
    public Iterable<Question> findAll() {
        return questionRepository.findAll();
    }

    @Override
    public List<Question> getAll() {
        return questionRepository.findAll();
    }

    @Override
    public Question findById(Integer id) {
        return questionRepository.findById(id).orElse(null);
    }

    @Override
    public Page<Question> findBySubjectAndTitle(int subjectId, String title, Pageable pageable) {
        return questionRepository.findAllBySubjectAndTitleContaining(subjectId,title,pageable);
    }

    @Override
    public void save(Question question) {
        questionRepository.save(question);
    }
    @Override
    public void delete(Question question) {
        questionRepository.delete(question);
    }
    @Override
    public Page<Question> findAllByExams(int id, Pageable pageable) {
        return questionRepository.findAllByExams(id, pageable);
    }
    @Override
    public Page<Question> findAllByTitleContaining(String title, Pageable pageable) {
        return questionRepository.findAllByTitleContaining(title, pageable);
    }
    @Override
    public Page<Question> findAllBySubject(Optional<Integer> id, Pageable pageable) {
        return questionRepository.findAllBySubject(id, pageable);
    }
    @Override
    public List<Question> findByLessonId(int id) {
        return questionRepository.findByLesson(id);
    }
    @Override
    public List<Question> findByExam(int examId) {
        return questionRepository.findByExam(examId);
    }
    @Override
    public List<Question> findBySubAndClass(int subId, int classId) {
        return questionRepository.findBySubAndClass(subId,classId);
    }
    @Override
    public List<Question> findBySub(int subId) {
        return questionRepository.findBySubject(subId);
    }
}

