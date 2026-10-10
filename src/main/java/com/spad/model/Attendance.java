package com.spad.model;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id"}))
public class Attendance {

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
    private int classesHeld;

    @Column(nullable = false)
    private int classesAttended;

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public int getClassesHeld() { return classesHeld; }
    public void setClassesHeld(int classesHeld) { this.classesHeld = classesHeld; }
    public int getClassesAttended() { return classesAttended; }
    public void setClassesAttended(int classesAttended) { this.classesAttended = classesAttended; }
}
