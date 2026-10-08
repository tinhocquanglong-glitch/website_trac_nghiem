package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.AttendanceStatus;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.StudentSemesterRecord;
import com.tttn.webthitracnghiem.model.StudentMonthlyRecord;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.TrackedStudent;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.repository.StudentSemesterRecordRepository;
import com.tttn.webthitracnghiem.repository.StudentMonthlyRecordRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.TrackedStudentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentTrackingServiceTest {

    private final StudentTrackingService service = new StudentTrackingService(
            mock(StudentTrackingClassRepository.class),
            mock(TrackedStudentRepository.class),
            mock(StudentSemesterRecordRepository.class),
            mock(StudentMonthlyRecordRepository.class),
            mock(StudentTrackingExcelParser.class));

    @Test
    void rejectsScoresOutsideZeroToTenBeforeSaving() {
        TrackingRowForm row = new TrackingRowForm();
        row.setStudentId(1);
        row.setFullName("Nguyen Van A");
        row.setMidtermScore(new BigDecimal("10.1"));
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setRows(List.of(row));

        assertThatThrownBy(() -> service.saveSheet(1, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 den 10");
    }

    @Test
    void rejectsFilesThatAreNotExcel() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.txt", "text/plain", "not an excel file".getBytes());

        assertThatThrownBy(() -> service.importWorkbook(file, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".xls");
    }

    @Test
    void rejectsDuplicateStudentNamesWhenSavingSheet() {
        TrackingRowForm first = row(1, "Nguyen Van A");
        TrackingRowForm duplicate = row(2, " nguyen  van a ");
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setRows(List.of(first, duplicate));

        assertThatThrownBy(() -> service.saveSheet(1, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trung ten");
    }

    @Test
    void rejectsDuplicateStudentIdsWhenSavingSheet() {
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setRows(List.of(row(1, "Nguyen Van A"), row(1, "Tran Van B")));

        assertThatThrownBy(() -> service.saveSheet(1, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ma hoc sinh bi trung");
    }

    @Test
    void loadsSemesterRecordsInOneBatchWhenSavingSheet() {
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(1);
        TrackedStudent student = new TrackedStudent();
        student.setId(10);
        student.setTrackingClass(trackingClass);
        student.setFullName("Nguyen Van A");

        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentSemesterRecordRepository recordRepository = mock(StudentSemesterRecordRepository.class);
        StudentMonthlyRecordRepository monthlyRecordRepository = mock(StudentMonthlyRecordRepository.class);
        StudentTrackingService batchService = new StudentTrackingService(
                classRepository, studentRepository, recordRepository, monthlyRecordRepository,
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1)).thenReturn(List.of(student));
        when(monthlyRecordRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 9))
                .thenReturn(List.of());

        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setRows(List.of(row(10, "Nguyen Van A")));

        batchService.saveSheet(1, form);

        verify(monthlyRecordRepository).findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 9);
        verify(recordRepository, never()).findByStudentIdAndSemester(10, 1);
    }

    @Test
    void showsTheSameClassStudentsInBothSemesters() {
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(1);
        TrackedStudent student = new TrackedStudent();
        student.setId(10);
        student.setTrackingClass(trackingClass);
        student.setFullName("Nguyen Van A");

        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentSemesterRecordRepository recordRepository = mock(StudentSemesterRecordRepository.class);
        StudentMonthlyRecordRepository monthlyRecordRepository = mock(StudentMonthlyRecordRepository.class);
        StudentTrackingService semesterService = new StudentTrackingService(
                classRepository, studentRepository, recordRepository, monthlyRecordRepository,
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1))
                .thenReturn(List.of(student));
        when(recordRepository.findByStudentIdInAndSemester(List.of(10), 1)).thenReturn(List.of());
        when(recordRepository.findByStudentIdInAndSemester(List.of(10), 2)).thenReturn(List.of());
        when(monthlyRecordRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 9)).thenReturn(List.of());
        when(monthlyRecordRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 2, 2)).thenReturn(List.of());

        TrackingSheetForm semesterOne = semesterService.getSheet(1, 1);
        TrackingSheetForm semesterTwo = semesterService.getSheet(1, 2);

        assertThat(semesterOne.getRows()).extracting(TrackingRowForm::getFullName)
                .containsExactly("Nguyen Van A");
        assertThat(semesterTwo.getRows()).extracting(TrackingRowForm::getFullName)
                .containsExactly("Nguyen Van A");
    }

    @Test
    void searchesStudentsAcrossAllClassesWithoutVietnameseDiacritics() {
        StudentTrackingClass class6A = trackingClass(1, "6A", "2026-2027");
        StudentTrackingClass class7B = trackingClass(2, "7B", "2026-2027");
        TrackedStudent matching = student(10, "Nguyễn Văn An", class6A);
        TrackedStudent other = student(11, "Trần Thị Bình", class7B);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentTrackingService searchService = new StudentTrackingService(
                mock(StudentTrackingClassRepository.class), studentRepository,
                mock(StudentSemesterRecordRepository.class), mock(StudentMonthlyRecordRepository.class),
                mock(StudentTrackingExcelParser.class));
        when(studentRepository.findAllForSearch()).thenReturn(List.of(other, matching));

        List<StudentSearchResult> results = searchService.searchStudents("nguyen van", null);

        assertThat(results).extracting(StudentSearchResult::getFullName)
                .containsExactly("Nguyễn Văn An");
        assertThat(results.get(0).getClassName()).isEqualTo("6A");
    }

    @Test
    void searchesStudentsOnlyInsideSelectedClass() {
        StudentTrackingClass class6A = trackingClass(1, "6A", "2026-2027");
        TrackedStudent student = student(10, "Nguyễn Văn An", class6A);
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentTrackingService searchService = new StudentTrackingService(
                classRepository, studentRepository, mock(StudentSemesterRecordRepository.class),
                mock(StudentMonthlyRecordRepository.class),
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(class6A));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1))
                .thenReturn(List.of(student));

        List<StudentSearchResult> results = searchService.searchStudents("an", 1);

        assertThat(results).extracting(StudentSearchResult::getStudentId)
                .containsExactly(10);
    }

    @Test
    void keepsAttendanceNoteAsFreeTextForEveryStatus() {
        assertThat(saveAttendance(AttendanceStatus.ABSENT, "  Nghỉ có phép, phụ huynh đã báo  ")
                .getAttendanceDates()).isEqualTo("Nghỉ có phép, phụ huynh đã báo");
        assertThat(saveAttendance(AttendanceStatus.FULL, "Đã đi học bù vào thứ bảy")
                .getAttendanceDates()).isEqualTo("Đã đi học bù vào thứ bảy");
        assertThat(saveAttendance(AttendanceStatus.NONE, "Ghi chú riêng của giáo viên")
                .getAttendanceDates()).isEqualTo("Ghi chú riêng của giáo viên");
    }

    @Test
    void allowsBlankAttendanceNoteForEveryStatus() {
        assertThat(saveAttendance(AttendanceStatus.TRANSFERRED, "  ").getAttendanceDates()).isNull();
        assertThat(saveAttendance(AttendanceStatus.DROPPED_OUT, null).getAttendanceDates()).isNull();
    }

    @Test
    void rejectsAttendanceNoteLongerThanDatabaseLimit() {
        assertThatThrownBy(() -> saveAttendance(AttendanceStatus.ABSENT, "a".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void showsClassStudentInEveryMonthWithIndependentMonthlyData() {
        StudentTrackingClass trackingClass = trackingClass(1, "6A", "2026-2027");
        TrackedStudent student = student(10, "Nguyen Van A", trackingClass);
        StudentMonthlyRecord october = new StudentMonthlyRecord();
        october.setStudent(student);
        october.setSemester(1);
        october.setMonth(10);
        october.setRegularScore1(new BigDecimal("8.5"));
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentSemesterRecordRepository legacyRepository = mock(StudentSemesterRecordRepository.class);
        StudentMonthlyRecordRepository monthlyRepository = mock(StudentMonthlyRecordRepository.class);
        StudentTrackingService monthlyService = new StudentTrackingService(
                classRepository, studentRepository, legacyRepository, monthlyRepository,
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1)).thenReturn(List.of(student));
        when(monthlyRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 10))
                .thenReturn(List.of(october));
        when(monthlyRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 2, 3))
                .thenReturn(List.of());

        TrackingSheetForm octoberSheet = monthlyService.getSheet(1, 1, 10);
        TrackingSheetForm marchSheet = monthlyService.getSheet(1, 2, 3);

        assertThat(octoberSheet.getRows()).extracting(TrackingRowForm::getFullName)
                .containsExactly("Nguyen Van A");
        assertThat(octoberSheet.getRows().get(0).getRegularScore1()).isEqualByComparingTo("8.5");
        assertThat(marchSheet.getRows()).extracting(TrackingRowForm::getFullName)
                .containsExactly("Nguyen Van A");
        assertThat(marchSheet.getRows().get(0).getRegularScore1()).isNull();
    }

    @Test
    void usesLegacySemesterDataOnlyForFirstMonthWhenMonthlyDataIsMissing() {
        StudentTrackingClass trackingClass = trackingClass(1, "6A", "2026-2027");
        TrackedStudent student = student(10, "Nguyen Van A", trackingClass);
        StudentSemesterRecord legacy = new StudentSemesterRecord();
        legacy.setStudent(student);
        legacy.setSemester(1);
        legacy.setRegularScore1(new BigDecimal("7"));
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentSemesterRecordRepository legacyRepository = mock(StudentSemesterRecordRepository.class);
        StudentMonthlyRecordRepository monthlyRepository = mock(StudentMonthlyRecordRepository.class);
        StudentTrackingService monthlyService = new StudentTrackingService(
                classRepository, studentRepository, legacyRepository, monthlyRepository,
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1)).thenReturn(List.of(student));
        when(monthlyRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 9)).thenReturn(List.of());
        when(legacyRepository.findByStudentIdInAndSemester(List.of(10), 1)).thenReturn(List.of(legacy));

        TrackingSheetForm sheet = monthlyService.getSheet(1, 1, 9);

        assertThat(sheet.getRows().get(0).getRegularScore1()).isEqualByComparingTo("7");
    }

    @Test
    void rejectsMonthOutsideSelectedSemester() {
        assertThatThrownBy(() -> service.getSheet(1, 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thang");
    }

    @Test
    void importsStudentOnceAtClassLevelAndStoresTrackingDataForSelectedMonth() throws Exception {
        StudentTrackingClass trackingClass = trackingClass(1, "6A", "2026-2027");
        StudentTrackingExcelParser.ImportedStudent importedStudent = mock(StudentTrackingExcelParser.ImportedStudent.class);
        when(importedStudent.getFullName()).thenReturn("Nguyen Van A");
        when(importedStudent.getRegularScore1()).thenReturn(new BigDecimal("8"));
        StudentTrackingExcelParser parser = mock(StudentTrackingExcelParser.class);
        when(parser.parse(any())).thenReturn(List.of(new StudentTrackingExcelParser.ImportedClass(
                "6A", "2026-2027", List.of(importedStudent))));
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentMonthlyRecordRepository monthlyRepository = mock(StudentMonthlyRecordRepository.class);
        when(classRepository.findByClassNameIgnoreCaseAndSchoolYear("6A", "2026-2027"))
                .thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1)).thenReturn(List.of());
        when(studentRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TrackedStudent> students = invocation.getArgument(0);
            students.forEach(student -> student.setId(10));
            return students;
        });
        when(monthlyRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 10))
                .thenReturn(List.of());
        StudentTrackingService importService = new StudentTrackingService(
                classRepository, studentRepository, mock(StudentSemesterRecordRepository.class),
                monthlyRepository, parser);
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1});

        importService.importWorkbook(file, 1, 10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<TrackedStudent>> studentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(studentRepository).saveAll(studentCaptor.capture());
        TrackedStudent savedStudent = studentCaptor.getValue().iterator().next();
        assertThat(savedStudent.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(savedStudent.getTrackingClass()).isSameAs(trackingClass);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<StudentMonthlyRecord>> recordCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(monthlyRepository).saveAll(recordCaptor.capture());
        StudentMonthlyRecord record = recordCaptor.getValue().iterator().next();
        assertThat(record.getStudent()).isSameAs(savedStudent);
        assertThat(record.getSemester()).isEqualTo(1);
        assertThat(record.getMonth()).isEqualTo(10);
        assertThat(record.getRegularScore1()).isEqualByComparingTo("8");

        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1))
                .thenReturn(List.of(savedStudent));
        when(monthlyRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 11))
                .thenReturn(List.of());
        when(monthlyRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 2, 3))
                .thenReturn(List.of());

        assertThat(importService.getSheet(1, 1, 11).getRows())
                .extracting(TrackingRowForm::getFullName)
                .containsExactly("Nguyen Van A");
        assertThat(importService.getSheet(1, 2, 3).getRows())
                .extracting(TrackingRowForm::getFullName)
                .containsExactly("Nguyen Van A");
    }

    private StudentMonthlyRecord saveAttendance(AttendanceStatus status, String dates) {
        StudentTrackingClass trackingClass = trackingClass(1, "6A", "2026-2027");
        TrackedStudent student = student(10, "Nguyen Van A", trackingClass);
        StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
        TrackedStudentRepository studentRepository = mock(TrackedStudentRepository.class);
        StudentSemesterRecordRepository recordRepository = mock(StudentSemesterRecordRepository.class);
        StudentMonthlyRecordRepository monthlyRecordRepository = mock(StudentMonthlyRecordRepository.class);
        StudentTrackingService attendanceService = new StudentTrackingService(
                classRepository, studentRepository, recordRepository, monthlyRecordRepository,
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1)).thenReturn(List.of(student));
        when(monthlyRecordRepository.findByStudentIdInAndSemesterAndMonth(List.of(10), 1, 9)).thenReturn(List.of());

        TrackingRowForm row = row(10, "Nguyen Van A");
        row.setAttendanceStatus(status);
        row.setAttendanceDates(dates);
        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setRows(List.of(row));

        attendanceService.saveSheet(1, form);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<StudentMonthlyRecord>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(monthlyRecordRepository).saveAll(captor.capture());
        return captor.getValue().iterator().next();
    }

    private StudentTrackingClass trackingClass(int id, String className, String schoolYear) {
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(id);
        trackingClass.setClassName(className);
        trackingClass.setSchoolYear(schoolYear);
        return trackingClass;
    }

    private TrackedStudent student(int id, String fullName, StudentTrackingClass trackingClass) {
        TrackedStudent student = new TrackedStudent();
        student.setId(id);
        student.setFullName(fullName);
        student.setTrackingClass(trackingClass);
        student.setDisplayOrder(id);
        return student;
    }

    private TrackingRowForm row(int studentId, String fullName) {
        TrackingRowForm row = new TrackingRowForm();
        row.setStudentId(studentId);
        row.setFullName(fullName);
        return row;
    }
}
