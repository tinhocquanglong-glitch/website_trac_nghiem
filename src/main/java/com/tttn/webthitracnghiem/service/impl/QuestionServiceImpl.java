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
        return questionRepository.findAllActive(pageable);
    }

    @Override
    public Iterable<Question> findAll() {
        return questionRepository.findAll();
    }

    @Override
    public List<Question> findAllWithContext() {
        return questionRepository.findAllWithContext();
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
        question.setStatus(0);
        questionRepository.save(question);
    }
    @Override
    public Page<Question> findAllByExams(int id, Pageable pageable) {
        return questionRepository.findAllByExams(id, pageable);
    }
    @Override
    public Page<Question> findAllByTitleContaining(String title, Pageable pageable) {
        return questionRepository.findActiveByTitleContaining(title, pageable);
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
    public List<Question> findByLessonPlanLessonId(int id) {
        return questionRepository.findByLessonPlanLesson_Id(id);
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

    // School-scoped queries
    @Override
    public Page<Question> findAllBySubjectAndSchool(Optional<Integer> subjectId, int schoolId, Pageable pageable) {
        return questionRepository.findAllBySubjectAndSchool(subjectId, schoolId, pageable);
    }

    @Override
    public Page<Question> findAllBySchool(int schoolId, Pageable pageable) {
        return questionRepository.findAllBySchool(schoolId, pageable);
    }

    @Override
    public Page<Question> findAllByTitleContainingAndSchool(String title, int schoolId, Pageable pageable) {
        return questionRepository.findAllByTitleContainingAndSchool(title, schoolId, pageable);
    }

    @Override
    public Page<Question> findBySubjectAndTitleAndSchool(int subjectId, String title, int schoolId, Pageable pageable) {
        return questionRepository.findAllBySubjectAndTitleContainingAndSchool(subjectId, title, schoolId, pageable);
    }

    @Override
    public Page<Question> findByFilters(Integer subjectId, Integer classId, Integer chapterId, String title, Pageable pageable) {
        return questionRepository.findByFilters(subjectId, classId, chapterId, emptyToNull(title), pageable);
    }

    @Override
    public Page<Question> findByFiltersAndSchool(Integer subjectId, Integer classId, Integer chapterId, String title,
                                                 int schoolId, Pageable pageable) {
        return questionRepository.findByFiltersAndSchool(subjectId, classId, chapterId, emptyToNull(title), schoolId, pageable);
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
