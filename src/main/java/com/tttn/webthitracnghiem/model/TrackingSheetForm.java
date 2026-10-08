package com.tttn.webthitracnghiem.model;

import java.util.ArrayList;
import java.util.List;

public class TrackingSheetForm {
    private Integer semester;
    private Integer month;
    private List<TrackingRowForm> rows = new ArrayList<>();

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public List<TrackingRowForm> getRows() { return rows; }
    public void setRows(List<TrackingRowForm> rows) { this.rows = rows; }
}
