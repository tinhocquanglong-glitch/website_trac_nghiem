package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.TrackedStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackedStudentRepository extends JpaRepository<TrackedStudent, Integer> {
    @Query("select student from TrackedStudent student "
            + "join fetch student.trackingClass trackingClass "
            + "where (student.status is null or student.status = 1) "
            + "and (trackingClass.status is null or trackingClass.status = 1) "
            + "order by trackingClass.schoolYear desc, trackingClass.className asc, "
            + "student.displayOrder asc, student.id asc")
    List<TrackedStudent> findAllForSearch();

    @Query("select s from TrackedStudent s where s.trackingClass.id = :classId and (s.status is null or s.status = 1) order by s.displayOrder asc, s.id asc")
    List<TrackedStudent> findByTrackingClassIdOrderByDisplayOrderAscIdAsc(@Param("classId") Integer classId);

    @Query("select s from TrackedStudent s where s.trackingClass.id = :classId and lower(s.fullName) = lower(:fullName) and (s.status is null or s.status = 1)")
    Optional<TrackedStudent> findFirstByTrackingClassIdAndFullNameIgnoreCase(@Param("classId") Integer classId, @Param("fullName") String fullName);
}
