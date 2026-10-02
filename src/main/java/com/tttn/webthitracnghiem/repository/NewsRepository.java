package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News,Integer> {
    Page<News> findAllByTitleContaining(String name, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM news WHERE create_date >= :date - interval 7 day")
    List<News> newsInWeek(Date date);
}
