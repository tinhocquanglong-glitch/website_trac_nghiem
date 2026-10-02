package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "document")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private Date createDate;
    private long view;
    private String avatar;
    @Column(columnDefinition = "text")
    private String content;
    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "user_id",referencedColumnName = "id")
    @JsonBackReference
    private User users;
    @ManyToOne(targetEntity = Subject.class)
    @JoinColumn(name = "subject_id",referencedColumnName = "id")
    @JsonBackReference
    private Subject subject;

    public Document() {
    }

    public Document(int id, String title, Date createDate, long view, String avatar, String content, User users, Subject subject) {
        this.id = id;
        this.title = title;
        this.createDate = createDate;
        this.view = view;
        this.avatar = avatar;
        this.content = content;
        this.users = users;
        this.subject = subject;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public long getView() {
        return view;
    }

    public void setView(long view) {
        this.view = view;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }
}
