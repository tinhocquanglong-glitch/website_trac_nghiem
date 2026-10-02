package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    @Query(nativeQuery = true,value = "SELECT * FROM chapter WHERE subject_class_id = :id order by serial")
    List<Chapter> findBySubjectAndClass(int id);

    @Query(nativeQuery = true,value = "SELECT * FROM chapter JOIN subject_classes ON chapter.subject_class_id = subject_classes.id " +
            "WHERE subject_classes.subject_id = :subId AND subject_classes.class_id = :classId order by chapter.serial")
    List<Chapter> findBySubjectAndClass(int subId, int classId);
    @Query(nativeQuery = true,value = "SELECT serial FROM chapter WHERE subject_class_id = :id order by serial desc limit 1")
    Optional<Integer> getSerial(int id);
}
