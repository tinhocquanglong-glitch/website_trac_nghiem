package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IQuestionService {
    Page<Question> findAll(Pageable pageable);

    Iterable<Question> findAll();
    List<Question> getAll();

    Question findById(Integer id);

    Page<Question> findBySubjectAndTitle(int subjectId, String title, Pageable pageable);

    void save(Question question);

    void delete(Question question);

    Page<Question> findAllByExams(int id, Pageable pageable);

    Page<Question> findAllByTitleContaining(String title, Pageable pageable);

    Page<Question> findAllBySubject(Optional<Integer> id, Pageable pageable);
    List<Question> findByLessonId(int id);
    List<Question> findByExam(int examId);
    List<Question> findBySubAndClass(int subId,int classId);
    List<Question> findBySub(int subId);
}
