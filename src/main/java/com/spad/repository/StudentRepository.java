package com.spad.repository;

import com.spad.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    boolean existsByRollNo(String rollNo);

    @Query("select s from Student s join fetch s.user order by s.rollNo")
    List<Student> findAllWithUser();

    @Query("select s from Student s join fetch s.user where s.id = :id")
    Optional<Student> findByIdWithUser(@Param("id") Long id);
}
