package com.spad.repository;

import com.spad.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("select a from Attendance a join fetch a.subject join fetch a.student")
    List<Attendance> findAllFetched();

    @Query("select a from Attendance a join fetch a.subject where a.student.id = :studentId")
    List<Attendance> findByStudentIdFetched(@Param("studentId") Long studentId);

    Optional<Attendance> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    void deleteByStudentId(Long studentId);
}
