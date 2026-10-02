package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.sql.Timestamp;

@Component
@Entity
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private double mark;
    private Timestamp startTime;
    private Timestamp endTime;

    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "username",referencedColumnName = "id" )
    @JsonBackReference
    private User users;


    @ManyToOne(targetEntity = Exam.class)
    @JoinColumn(name = "id_exam",referencedColumnName = "id")
    @JsonBackReference
    private Exam exam;

    public Result() {
    }

    public Result(int id, double mark, Timestamp startTime, Timestamp endTime, User users, Exam exam) {
        this.id = id;
        this.mark = mark;
        this.startTime = startTime;
        this.endTime = endTime;
        this.users = users;
        this.exam = exam;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public double getMark() {
        return mark;
    }

    public void setMark(double mark) {
        this.mark = mark;
    }
}
