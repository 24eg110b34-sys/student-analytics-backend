package com.spad.model;

import jakarta.persistence.*;

/** Scores (out of 100) of one student in one subject. */
@Entity
@Table(name = "marks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id"}))
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private double assignmentScore;

    @Column(nullable = false)
    private double internalScore;

    @Column(nullable = false)
    private double examScore;

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public double getAssignmentScore() { return assignmentScore; }
    public void setAssignmentScore(double assignmentScore) { this.assignmentScore = assignmentScore; }
    public double getInternalScore() { return internalScore; }
    public void setInternalScore(double internalScore) { this.internalScore = internalScore; }
    public double getExamScore() { return examScore; }
    public void setExamScore(double examScore) { this.examScore = examScore; }
}
