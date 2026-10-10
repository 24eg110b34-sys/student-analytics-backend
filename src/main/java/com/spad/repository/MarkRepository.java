package com.spad.repository;

import com.spad.model.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {

    @Query("select m from Mark m join fetch m.subject join fetch m.student")
    List<Mark> findAllFetched();

    @Query("select m from Mark m join fetch m.subject where m.student.id = :studentId")
    List<Mark> findByStudentIdFetched(@Param("studentId") Long studentId);

    Optional<Mark> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    void deleteByStudentId(Long studentId);
}
