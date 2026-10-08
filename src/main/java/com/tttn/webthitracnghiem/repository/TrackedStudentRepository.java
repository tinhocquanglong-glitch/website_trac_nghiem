package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.TrackedStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrackedStudentRepository extends JpaRepository<TrackedStudent, Integer> {
    @Query("select student from TrackedStudent student "
            + "join fetch student.trackingClass trackingClass "
            + "order by trackingClass.schoolYear desc, trackingClass.className asc, "
            + "student.displayOrder asc, student.id asc")
    List<TrackedStudent> findAllForSearch();

    List<TrackedStudent> findByTrackingClassIdOrderByDisplayOrderAscIdAsc(Integer classId);
    Optional<TrackedStudent> findFirstByTrackingClassIdAndFullNameIgnoreCase(Integer classId, String fullName);
}
