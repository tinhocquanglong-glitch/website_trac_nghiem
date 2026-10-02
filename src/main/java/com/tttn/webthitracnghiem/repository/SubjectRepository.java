package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    @Query(value = "SELECT * FROM subject where subject.name like %:name%",
            countQuery = "SELECT COUNT(*) FROM subject where subject.name like %:name%",
            nativeQuery = true)
    Page<Subject> findAllByName(String name, Pageable pageable);

}

