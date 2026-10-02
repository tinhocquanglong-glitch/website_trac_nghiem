package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Integer> {
    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",
            countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",nativeQuery = true)
    Page<Exam> findAllBySubject(@Param("id") int id, Pageable pageable);
    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",nativeQuery = true)
    List<Exam> findAllBySubject(@Param("id") int id);
    @Query(value = "select * from exam_question join question on exam_question.ques_id = question.ques_id where exam_question.exam_id = :id",
            nativeQuery = true)
    Page<Exam> findAllByQuestions(@Param("id") int id, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM exam WHERE lesson_id = :id",countQuery = "SELECT COUNT(*) FROM exam WHERE lesson_id = :id")
    Page<Exam> findAllByLesson(int id,Pageable pageable);
    Page<Exam> findAllByNameExamContaining(String name, Pageable pageable);
    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id and exam.name_exam like %:name%",
            countQuery = "SELECT COUNT(exam.*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id and exam.name_exam like %:name%",
            nativeQuery = true)
    Page<Exam> findAllBySubjectAndNameExamContaining(@Param("id") int id, @Param("name") String name,Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM exam WHERE lesson_id = :lessonId AND name_exam like %:name%",
    countQuery = "SELECT COUNT(*) FROM exam WHERE lesson_id = :lessonId AND name_exam like %:name%")
    Page<Exam> findByNameExamContainingAndLesson(@Param("lessonId") int lessonId,@Param("name") String name, Pageable pageable);
}
