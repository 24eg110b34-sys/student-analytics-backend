package com.spad.model;

import jakarta.persistence.*;

/** Self-reported weekly study hours for one semester (an input to the prediction model). */
@Entity
@Table(name = "semester_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "semester"}))
public class SemesterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    private double studyHoursPerWeek;

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    public double getStudyHoursPerWeek() { return studyHoursPerWeek; }
    public void setStudyHoursPerWeek(double studyHoursPerWeek) { this.studyHoursPerWeek = studyHoursPerWeek; }
}
