package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document,Integer> {

    Page<Document> findByTitleContaining(String title, Pageable pageable);
}
