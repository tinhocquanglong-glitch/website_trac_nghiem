package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Result;

import java.util.List;

public interface IResultService {
    List<Result> getTopTen();

    List<Result> findByHistory(String id);

    Object findSum(String id);

    Object findAvg(String id);
    List<Result> findAll();
    Result findById(int id);
}
