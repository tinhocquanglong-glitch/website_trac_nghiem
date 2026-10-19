package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.LessonPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, Integer> {
    List<LessonPlan> findByIsPublicTrueOrderByUpdatedAtDesc();
    List<LessonPlan> findByTargetClass_IdOrderByUpdatedAtDesc(Integer targetClassId);
    List<LessonPlan> findByOwner_IdOrderByUpdatedAtDesc(String ownerId);
    List<LessonPlan> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {
            "owner", "subject", "grade", "targetClass", "subjectClasses",
            "chapters", "lessons", "planChapters"
    })
    java.util.Optional<LessonPlan> findDetailedById(Integer id);
}
