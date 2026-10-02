package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.Date;
import java.util.List;

@Entity
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank
    @Pattern(regexp = "^MD-[\\d]{4}$",message = "Mã đề thì phải là định dạng MD-XXXX(XXXX là số)")
    @Column(unique = true)
    private String idName;
    @Column
    private boolean free = true;
    private int time;
    private Date createDate;
    private Date lastModify;
    @NotBlank(message = "Tên đề thi không được bỏ trống")
    private String nameExam;

    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "users_id", referencedColumnName = "id")
    @JsonBackReference
    private User users;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private List<Result> results;
    @ManyToOne
    @JoinColumn(name = "lesson_id",referencedColumnName = "id")
    @JsonBackReference
    private Lesson lesson;
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "exam_question",
            joinColumns = @JoinColumn(name = "examId"),
            inverseJoinColumns = @JoinColumn(name = "quesId"))
    @JsonBackReference
    private List<Question> questions;

    private long view;
    private long turn;

    public Exam() {
    }

    public Exam(int id, String idName, boolean free, int time, Date createDate, Date lastModify, String nameExam,
                User users, List<Result> results, Lesson lesson, List<Question> questions,
                long view, long turn) {
        this.id = id;
        this.idName = idName;
        this.free = free;
        this.time = time;
        this.createDate = createDate;
        this.lastModify = lastModify;
        this.nameExam = nameExam;
        this.users = users;
        this.results = results;
        this.lesson = lesson;
        this.questions = questions;
        this.view = view;
        this.turn = turn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public String getNameExam() {
        return nameExam;
    }

    public void setNameExam(String nameExam) {
        this.nameExam = nameExam;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }


    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public long getView() {
        return view;
    }

    public void setView(long view) {
        this.view = view;
    }

    public long getTurn() {
        return turn;
    }

    public void setTurn(long turn) {
        this.turn = turn;
    }

}
