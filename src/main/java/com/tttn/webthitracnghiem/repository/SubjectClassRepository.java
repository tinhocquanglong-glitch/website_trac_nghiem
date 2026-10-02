package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.SubjectClasses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectClassRepository extends JpaRepository<SubjectClasses, Integer> {
    @Query(nativeQuery = true,value = "SELECT * FROM subject_classes WHERE class_id = :id")
    Page<SubjectClasses> findByClass(int id, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM subject_classes WHERE class_id = :id")
    List<SubjectClasses> findByClassId(int id);
    @Query(nativeQuery = true,value = "SELECT * FROM subject_classes WHERE class_id = :classId AND subject_id = :subjectId")
    Optional<SubjectClasses> findByClassesAndSubject(@Param("classId") int classId, @Param("subjectId") int subjectId);
    @Query(nativeQuery = true,value = "SELECT * FROM subject_classes WHERE subject_id = :id ORDER BY class_id")
    List<SubjectClasses> findBySubject(@Param("id") int id);
}
