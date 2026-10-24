package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonPlanLessonRepository extends JpaRepository<LessonPlanLesson, Integer> {
    long countByCurriculumLesson_Id(int lessonId);
}
