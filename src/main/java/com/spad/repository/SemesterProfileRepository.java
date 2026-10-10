package com.spad.repository;

import com.spad.model.SemesterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SemesterProfileRepository extends JpaRepository<SemesterProfile, Long> {

    @Query("select p from SemesterProfile p join fetch p.student")
    List<SemesterProfile> findAllFetched();

    List<SemesterProfile> findByStudentId(Long studentId);

    Optional<SemesterProfile> findByStudentIdAndSemester(Long studentId, int semester);

    void deleteByStudentId(Long studentId);
}
