package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(unique = true)
    private String roleName;

    @ManyToMany(mappedBy = "roles")
    @JsonManagedReference
    private Set<User> users;

    public Role() {
    }

    public Role(String roleName) {
        this.roleName = roleName;
    }

    public Role(Integer id, String roleName, Set<User> users) {
        this.id = id;
        this.roleName = roleName;
        this.users = users;
    }

    public Role(String roleName, Set<User> users) {
        this.roleName = roleName;
        this.users = users;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}

