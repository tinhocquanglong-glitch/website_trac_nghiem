package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @Query("SELECT d FROM Document d WHERE (d.status IS NULL OR d.status = 1)")
    Page<Document> findAllActive(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE (d.status IS NULL OR d.status = 1) AND d.title LIKE %:title%")
    Page<Document> findByTitleContainingActive(@Param("title") String title, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE (d.status IS NULL OR d.status = 1)")
    List<Document> findAllActive();

    Page<Document> findByTitleContaining(String title, Pageable pageable);
}
