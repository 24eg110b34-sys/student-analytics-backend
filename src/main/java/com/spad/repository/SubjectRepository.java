package com.spad.repository;

import com.spad.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByOrderBySemesterAscCodeAsc();
    boolean existsByCode(String code);
}
