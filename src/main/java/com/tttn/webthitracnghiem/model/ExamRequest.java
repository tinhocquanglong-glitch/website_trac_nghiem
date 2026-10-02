package com.tttn.webthitracnghiem.model;
import java.sql.Date;
import java.util.List;

public class ExamRequest {
    private int id;
    private String idName;
    private boolean free = true;
    private int time;
    private Date createDate;
    private Date lastModify;
    private String nameExam;
    private String usersId;
    private int lessonId;
    private List<QuestionRequest> questions;

    public ExamRequest() {
    }

    public ExamRequest(int id, String idName, boolean free, int time, Date createDate, Date lastModify,
                       String nameExam, String usersId, int lessonId, List<QuestionRequest> questions) {
        this.id = id;
        this.idName = idName;
        this.free = free;
        this.time = time;
        this.createDate = createDate;
        this.lastModify = lastModify;
        this.nameExam = nameExam;
        this.usersId = usersId;
        this.lessonId = lessonId;
        this.questions = questions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
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

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public String getNameExam() {
        return nameExam;
    }

    public void setNameExam(String nameExam) {
        this.nameExam = nameExam;
    }

    public String getUsersId() {
        return usersId;
    }

    public void setUsersId(String usersId) {
        this.usersId = usersId;
    }

    public int getLessonId() {
        return lessonId;
    }

    public void setLessonId(int lessonId) {
        this.lessonId = lessonId;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }
}
