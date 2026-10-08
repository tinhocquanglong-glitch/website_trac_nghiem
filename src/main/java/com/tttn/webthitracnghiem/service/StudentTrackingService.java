package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.StudentSemesterRecord;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.TrackedStudent;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.repository.StudentSemesterRecordRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.TrackedStudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StudentTrackingService {
    private static final long MAX_IMPORT_BYTES = 5L * 1024 * 1024;
    private static final Pattern SCHOOL_YEAR_PATTERN = Pattern.compile("\\d{4}-\\d{4}");

    private final StudentTrackingClassRepository classRepository;
    private final TrackedStudentRepository studentRepository;
    private final StudentSemesterRecordRepository recordRepository;
    private final StudentTrackingExcelParser excelParser;

    public StudentTrackingService(StudentTrackingClassRepository classRepository,
                                  TrackedStudentRepository studentRepository,
                                  StudentSemesterRecordRepository recordRepository,
                                  StudentTrackingExcelParser excelParser) {
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.recordRepository = recordRepository;
        this.excelParser = excelParser;
    }

    @Transactional(readOnly = true)
    public List<StudentTrackingClass> listClasses() {
        return classRepository.findAllByOrderBySchoolYearDescClassNameAsc();
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResult> searchStudents(String query, Integer classId) {
        String normalizedQuery = normalizeForSearch(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        List<TrackedStudent> students;
        if (classId == null) {
            students = studentRepository.findAllForSearch();
        } else {
            findClass(classId);
            students = studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(classId);
        }
        return students.stream()
                .filter(student -> normalizeForSearch(student.getFullName()).contains(normalizedQuery))
                .map(student -> new StudentSearchResult(
                        student.getId(), student.getTrackingClass().getId(), student.getFullName(),
                        student.getTrackingClass().getClassName(), student.getTrackingClass().getSchoolYear()))
                .sorted(Comparator.comparing(StudentSearchResult::getSchoolYear).reversed()
                        .thenComparing(StudentSearchResult::getClassName)
                        .thenComparing(StudentSearchResult::getFullName))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentTrackingClass findClass(Integer classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay lop theo doi"));
    }

    @Transactional
    public StudentTrackingClass createClass(String className, String schoolYear) {
        String cleanName = cleanRequired(className, 50, "Ten lop");
        String cleanYear = cleanRequired(schoolYear, 9, "Nam hoc");
        if (!SCHOOL_YEAR_PATTERN.matcher(cleanYear).matches()) {
            throw new IllegalArgumentException("Nam hoc phai co dang 2026-2027");
        }
        if (classRepository.findByClassNameIgnoreCaseAndSchoolYear(cleanName, cleanYear).isPresent()) {
            throw new IllegalArgumentException("Lop nay da ton tai trong nam hoc " + cleanYear);
        }
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setClassName(cleanName);
        trackingClass.setSchoolYear(cleanYear);
        return classRepository.save(trackingClass);
    }

    @Transactional
    public void deleteClass(Integer classId) {
        classRepository.delete(findClass(classId));
    }

    @Transactional
    public TrackedStudent addStudent(Integer classId, String fullName) {
        StudentTrackingClass trackingClass = findClass(classId);
        String cleanName = cleanRequired(fullName, 150, "Ho va ten");
        if (studentRepository.findFirstByTrackingClassIdAndFullNameIgnoreCase(classId, cleanName).isPresent()) {
            throw new IllegalArgumentException("Hoc sinh nay da co trong lop");
        }
        List<TrackedStudent> currentStudents = studentRepository
                .findByTrackingClassIdOrderByDisplayOrderAscIdAsc(classId);
        int nextOrder = currentStudents.stream()
                .map(TrackedStudent::getDisplayOrder)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        return studentRepository.save(newStudent(trackingClass, cleanName, nextOrder));
    }

    @Transactional
    public void deleteStudent(Integer classId, Integer studentId) {
        TrackedStudent student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay hoc sinh"));
        if (!student.getTrackingClass().getId().equals(classId)) {
            throw new IllegalArgumentException("Hoc sinh khong thuoc lop nay");
        }
        studentRepository.delete(student);
    }

    @Transactional(readOnly = true)
    public TrackingSheetForm getSheet(Integer classId, int semester) {
        validateSemester(semester);
        findClass(classId);
        List<TrackedStudent> students = studentRepository
                .findByTrackingClassIdOrderByDisplayOrderAscIdAsc(classId);
        List<Integer> studentIds = students.stream().map(TrackedStudent::getId).collect(Collectors.toList());
        Map<Integer, StudentSemesterRecord> records = new HashMap<>();
        if (!studentIds.isEmpty()) {
            records = recordRepository.findByStudentIdInAndSemester(studentIds, semester).stream()
                    .collect(Collectors.toMap(record -> record.getStudent().getId(), record -> record));
        }

        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(semester);
        List<TrackingRowForm> rows = new ArrayList<>();
        for (TrackedStudent student : students) {
            TrackingRowForm row = new TrackingRowForm();
            row.setStudentId(student.getId());
            row.setFullName(student.getFullName());
            StudentSemesterRecord record = records.get(student.getId());
            if (record != null) {
                copyRecordToRow(record, row);
            }
            rows.add(row);
        }
        form.setRows(rows);
        return form;
    }

    @Transactional
    public void saveSheet(Integer classId, TrackingSheetForm form) {
        if (form == null || form.getSemester() == null) {
            throw new IllegalArgumentException("Hoc ky khong hop le");
        }
        int semester = form.getSemester();
        validateSemester(semester);
        List<TrackingRowForm> rows = form.getRows() == null ? List.of() : form.getRows();
        Set<String> submittedNames = new HashSet<>();
        Set<Integer> submittedStudentIds = new HashSet<>();
        for (TrackingRowForm row : rows) {
            validateRow(row);
            if (!submittedStudentIds.add(row.getStudentId())) {
                throw new IllegalArgumentException("Danh sach co ma hoc sinh bi trung");
            }
            if (!submittedNames.add(nameKey(row.getFullName()))) {
                throw new IllegalArgumentException("Danh sach co hoc sinh trung ten");
            }
        }

        findClass(classId);
        Map<Integer, TrackedStudent> students = studentRepository
                .findByTrackingClassIdOrderByDisplayOrderAscIdAsc(classId).stream()
                .collect(Collectors.toMap(TrackedStudent::getId, student -> student));
        Map<Integer, StudentSemesterRecord> records = new HashMap<>();
        if (!students.isEmpty()) {
            records = recordRepository
                    .findByStudentIdInAndSemester(new ArrayList<>(students.keySet()), semester).stream()
                    .collect(Collectors.toMap(record -> record.getStudent().getId(), record -> record));
        }
        List<TrackedStudent> changedStudents = new ArrayList<>();
        List<StudentSemesterRecord> changedRecords = new ArrayList<>();
        for (TrackingRowForm row : rows) {
            TrackedStudent student = students.get(row.getStudentId());
            if (student == null) {
                throw new IllegalArgumentException("Danh sach co hoc sinh khong thuoc lop nay");
            }
            student.setFullName(cleanRequired(row.getFullName(), 150, "Ho va ten"));
            changedStudents.add(student);

            StudentSemesterRecord record = records.getOrDefault(student.getId(), new StudentSemesterRecord());
            record.setStudent(student);
            record.setSemester(semester);
            copyRowToRecord(row, record);
            changedRecords.add(record);
        }
        studentRepository.saveAll(changedStudents);
        recordRepository.saveAll(changedRecords);
    }

    @Transactional
    public ImportSummary importWorkbook(MultipartFile file, int semester) throws IOException {
        validateSemester(semester);
        validateUpload(file);
        List<StudentTrackingExcelParser.ImportedClass> importedClasses;
        try (var inputStream = file.getInputStream()) {
            importedClasses = excelParser.parse(inputStream);
        }

        int importedStudentCount = 0;
        for (StudentTrackingExcelParser.ImportedClass importedClass : importedClasses) {
            StudentTrackingClass trackingClass = classRepository
                    .findByClassNameIgnoreCaseAndSchoolYear(importedClass.getClassName(), importedClass.getSchoolYear())
                    .orElseGet(() -> {
                        StudentTrackingClass created = new StudentTrackingClass();
                        created.setClassName(importedClass.getClassName());
                        created.setSchoolYear(importedClass.getSchoolYear());
                        return classRepository.save(created);
                    });
            List<TrackedStudent> currentStudents = studentRepository
                    .findByTrackingClassIdOrderByDisplayOrderAscIdAsc(trackingClass.getId());
            int nextOrder = currentStudents.stream().map(TrackedStudent::getDisplayOrder)
                    .filter(value -> value != null).max(Integer::compareTo).orElse(0) + 1;

            Map<String, TrackedStudent> studentsByName = currentStudents.stream()
                    .collect(Collectors.toMap(student -> nameKey(student.getFullName()), student -> student,
                            (first, duplicate) -> first, LinkedHashMap::new));
            List<TrackedStudent> newStudents = new ArrayList<>();
            for (StudentTrackingExcelParser.ImportedStudent importedStudent : importedClass.getStudents()) {
                String key = nameKey(importedStudent.getFullName());
                if (!studentsByName.containsKey(key)) {
                    TrackedStudent student = newStudent(trackingClass, importedStudent.getFullName(), nextOrder++);
                    studentsByName.put(key, student);
                    newStudents.add(student);
                }
            }
            if (!newStudents.isEmpty()) {
                studentRepository.saveAll(newStudents);
            }

            List<Integer> importedStudentIds = importedClass.getStudents().stream()
                    .map(importedStudent -> studentsByName.get(nameKey(importedStudent.getFullName())).getId())
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, StudentSemesterRecord> records = new LinkedHashMap<>();
            if (!importedStudentIds.isEmpty()) {
                records = recordRepository
                        .findByStudentIdInAndSemester(importedStudentIds, semester).stream()
                        .collect(Collectors.toMap(record -> record.getStudent().getId(), record -> record,
                                (first, duplicate) -> first, LinkedHashMap::new));
            }
            for (StudentTrackingExcelParser.ImportedStudent importedStudent : importedClass.getStudents()) {
                TrackedStudent student = studentsByName.get(nameKey(importedStudent.getFullName()));
                StudentSemesterRecord record = records.computeIfAbsent(student.getId(), ignored -> {
                    StudentSemesterRecord created = new StudentSemesterRecord();
                    created.setStudent(student);
                    created.setSemester(semester);
                    return created;
                });
                record.setStudent(student);
                record.setSemester(semester);
                copyImportedToRecord(importedStudent, record);
                importedStudentCount++;
            }
            recordRepository.saveAll(records.values());
        }
        return new ImportSummary(importedClasses.size(), importedStudentCount);
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui long chon file Excel");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new IllegalArgumentException("File Excel toi da 5 MB");
        }
        String originalName = file.getOriginalFilename();
        String lowerName = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new IllegalArgumentException("Chi chap nhan file .xls hoac .xlsx");
        }
    }

    private TrackedStudent newStudent(StudentTrackingClass trackingClass, String fullName, int displayOrder) {
        TrackedStudent student = new TrackedStudent();
        student.setTrackingClass(trackingClass);
        student.setFullName(fullName);
        student.setDisplayOrder(displayOrder);
        return student;
    }

    private void validateRow(TrackingRowForm row) {
        if (row == null || row.getStudentId() == null) {
            throw new IllegalArgumentException("Dong hoc sinh khong hop le");
        }
        cleanRequired(row.getFullName(), 150, "Ho va ten");
        validateScore(row.getRegularScore1());
        validateScore(row.getRegularScore2());
        validateScore(row.getMidtermScore());
        validateScore(row.getFinalScore());
        validateScore(row.getAverageScore());
        if (row.getProgressComment() != null && row.getProgressComment().length() > 2000) {
            throw new IllegalArgumentException("Nhan xet toi da 2000 ky tu");
        }
    }

    private void validateScore(BigDecimal score) {
        if (score != null && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0)) {
            throw new IllegalArgumentException("Diem phai tu 0 den 10");
        }
    }

    private void validateSemester(int semester) {
        if (semester != 1 && semester != 2) {
            throw new IllegalArgumentException("Hoc ky chi co the la 1 hoac 2");
        }
    }

    private String cleanRequired(String value, int maxLength, String fieldName) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " khong duoc de trong");
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " qua dai");
        }
        return cleaned;
    }

    private String nameKey(String fullName) {
        return cleanRequired(fullName, 150, "Ho va ten").toLowerCase(Locale.ROOT);
    }

    private String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private void copyRecordToRow(StudentSemesterRecord record, TrackingRowForm row) {
        row.setRegularScore1(record.getRegularScore1());
        row.setRegularScore2(record.getRegularScore2());
        row.setMidtermScore(record.getMidtermScore());
        row.setFinalScore(record.getFinalScore());
        row.setAverageScore(record.getAverageScore());
        row.setProgressComment(record.getProgressComment());
        row.setAttendanceStatus(record.getAttendanceStatus());
    }

    private void copyRowToRecord(TrackingRowForm row, StudentSemesterRecord record) {
        record.setRegularScore1(row.getRegularScore1());
        record.setRegularScore2(row.getRegularScore2());
        record.setMidtermScore(row.getMidtermScore());
        record.setFinalScore(row.getFinalScore());
        record.setAverageScore(row.getAverageScore());
        record.setProgressComment(row.getProgressComment() == null ? null : row.getProgressComment().trim());
        record.setAttendanceStatus(row.getAttendanceStatus());
    }

    private void copyImportedToRecord(StudentTrackingExcelParser.ImportedStudent imported,
                                      StudentSemesterRecord record) {
        record.setRegularScore1(imported.getRegularScore1());
        record.setRegularScore2(imported.getRegularScore2());
        record.setMidtermScore(imported.getMidtermScore());
        record.setFinalScore(imported.getFinalScore());
        record.setAverageScore(imported.getAverageScore());
        record.setProgressComment(imported.getProgressComment());
        record.setAttendanceStatus(imported.getAttendanceStatus());
    }

    public static class ImportSummary {
        private final int classCount;
        private final int studentCount;

        public ImportSummary(int classCount, int studentCount) {
            this.classCount = classCount;
            this.studentCount = studentCount;
        }

        public int getClassCount() { return classCount; }
        public int getStudentCount() { return studentCount; }
    }
}
