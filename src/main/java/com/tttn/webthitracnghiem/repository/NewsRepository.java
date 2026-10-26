package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Integer> {

    @Query("SELECT n FROM News n WHERE (n.status IS NULL OR n.status = 1)")
    Page<News> findAllActive(Pageable pageable);

    @Query("SELECT n FROM News n WHERE (n.status IS NULL OR n.status = 1) AND n.title LIKE %:name%")
    Page<News> findAllByTitleContainingActive(@Param("name") String name, Pageable pageable);

    @Query("SELECT n FROM News n WHERE (n.status IS NULL OR n.status = 1)")
    List<News> findAllActive();

    Page<News> findAllByTitleContaining(String name, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM news WHERE create_date >= :date - interval 7 day AND (status IS NULL OR status = 1)")
    List<News> newsInWeek(@Param("date") Date date);
}
