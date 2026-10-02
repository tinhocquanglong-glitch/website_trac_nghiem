package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.IQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IQRepository extends JpaRepository<IQ,Integer> {
    @Query(nativeQuery = true,value = "SELECT * FROM iq WHERE username = :name")
    List<IQ> findByUsername(@Param("name") String un);
}
