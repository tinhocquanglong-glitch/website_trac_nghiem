package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface  UserRepository  extends JpaRepository<User,String > {
    @Query(value = "select count(id) from user;",nativeQuery = true)
    Integer findByTotalUser();
    @Query(value = "SELECT full_name FROM user order by create_date DESC LIMIT 1",nativeQuery = true)
    String findByNewUser();
    @Query(value = "select pass_word\n" +
            "from user\n" +
            "where id =:id",nativeQuery = true)
    String findByPass(@Param("id") String id);
    Optional<User> findByEmail(String email);
    @Query(value = "SELECT * FROM user WHERE id LIKE %:name% or address LIKE %:name% or email LIKE %:name% or" +
            " full_name LIKE %:name% or phone_number LIKE %:name%",nativeQuery = true)
    Page<User> search(String name, Pageable pageable);

    @Query(nativeQuery = true,value = "SELECT * FROM user WHERE create_date >= :date - interval 7 day")
    List<User> newMember(Date date);
    @Query(nativeQuery = true,value = "SELECT * FROM user JOIN user_role ON user.id = user_role.username ")
    List<User> getMember();
    Optional<User> findById(int id);
}
