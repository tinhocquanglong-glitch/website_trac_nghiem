package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.repository.ResultRepository;
import com.tttn.webthitracnghiem.service.IResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResultServiceImpl implements IResultService {
    @Autowired
    ResultRepository resultRepository;

    @Override
    public List<Result> getTopTen() {
        return resultRepository.findTopTen();
    }

    @Override
    public List<Result> findByHistory(String id) {
        return resultRepository.findByHistory(id);
    }

    @Override
    public Object findSum(String id) {
        return resultRepository.findSum(id);
    }

    @Override
    public Object findAvg(String id) {
        return resultRepository.findAvg(id);
    }

    @Override
    public List<Result> findAll() {
        return resultRepository.findAll();
    }

    @Override
    public Result findById(int id) {
        return resultRepository.findById(id).orElse(null);
    }
}
