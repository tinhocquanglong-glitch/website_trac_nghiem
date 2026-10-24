package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson,Integer> {
    @Query(nativeQuery = true,value = "SELECT * FROM lesson WHERE chapter_id = :id order by serial")
    Page<Lesson> findByChapter(int id, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM lesson WHERE chapter_id = :id")
    List<Lesson> findByChapterId(int id);
    @Query(nativeQuery = true,value = "SELECT COUNT(id) FROM lesson WHERE chapter_id = :id ")
    Optional<Integer> getSerial(int id);
    Optional<Lesson> findById(int id);

    List<Lesson> findByChapter_IdOrderBySerialAscIdAsc(int chapterId);

    /** Lessons of other schools copied from the same shared lesson (plus the shared lesson itself). */
    @Query("SELECT l FROM Lesson l WHERE l.templateLesson.id = :templateId OR l.id = :templateId")
    List<Lesson> findSiblingsOfTemplate(@org.springframework.data.repository.query.Param("templateId") int templateId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Lesson l SET l.templateLesson = NULL WHERE l.templateLesson.id = :templateId")
    int detachCopiesOf(@org.springframework.data.repository.query.Param("templateId") int templateId);
}
