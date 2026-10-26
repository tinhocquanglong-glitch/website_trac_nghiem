package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.LessonPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, Integer> {
    @org.springframework.data.jpa.repository.Query("SELECT lp FROM LessonPlan lp WHERE lp.isPublic = true AND (lp.status IS NULL OR lp.status = 1) ORDER BY lp.updatedAt DESC")
    List<LessonPlan> findByIsPublicTrueOrderByUpdatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT lp FROM LessonPlan lp WHERE lp.targetClass.id = :targetClassId AND (lp.status IS NULL OR lp.status = 1) ORDER BY lp.updatedAt DESC")
    List<LessonPlan> findByTargetClass_IdOrderByUpdatedAtDesc(@org.springframework.data.repository.query.Param("targetClassId") Integer targetClassId);

    @org.springframework.data.jpa.repository.Query("SELECT lp FROM LessonPlan lp WHERE lp.owner.id = :ownerId AND (lp.status IS NULL OR lp.status = 1) ORDER BY lp.updatedAt DESC")
    List<LessonPlan> findByOwner_IdOrderByUpdatedAtDesc(@org.springframework.data.repository.query.Param("ownerId") String ownerId);

    @org.springframework.data.jpa.repository.Query("SELECT lp FROM LessonPlan lp WHERE (lp.status IS NULL OR lp.status = 1) ORDER BY lp.updatedAt DESC")
    List<LessonPlan> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {
            "owner", "subject", "grade", "targetClass", "subjectClasses",
            "chapters", "lessons", "planChapters"
    })
    java.util.Optional<LessonPlan> findDetailedById(Integer id);

    /** Plans from the old form that still reference a curriculum lesson directly. */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM LessonPlan p JOIN p.lessons l WHERE l.id = :lessonId")
    long countLegacyLinksToLesson(@org.springframework.data.repository.query.Param("lessonId") int lessonId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM LessonPlan p JOIN p.chapters c WHERE c.id = :chapterId")
    long countLegacyLinksToChapter(@org.springframework.data.repository.query.Param("chapterId") int chapterId);
}
