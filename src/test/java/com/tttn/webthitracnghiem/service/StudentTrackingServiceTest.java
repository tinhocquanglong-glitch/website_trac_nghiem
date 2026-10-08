package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.StudentSearchResult;
import com.tttn.webthitracnghiem.model.TrackedStudent;
import com.tttn.webthitracnghiem.model.TrackingRowForm;
import com.tttn.webthitracnghiem.model.TrackingSheetForm;
import com.tttn.webthitracnghiem.repository.StudentSemesterRecordRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.TrackedStudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentTrackingServiceTest {

    private final StudentTrackingService service = new StudentTrackingService(
            mock(StudentTrackingClassRepository.class),
            mock(TrackedStudentRepository.class),
            mock(StudentSemesterRecordRepository.class),
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
        StudentTrackingService batchService = new StudentTrackingService(
                classRepository, studentRepository, recordRepository, mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1)).thenReturn(List.of(student));
        when(recordRepository.findByStudentIdInAndSemester(List.of(10), 1)).thenReturn(List.of());

        TrackingSheetForm form = new TrackingSheetForm();
        form.setSemester(1);
        form.setRows(List.of(row(10, "Nguyen Van A")));

        batchService.saveSheet(1, form);

        verify(recordRepository).findByStudentIdInAndSemester(List.of(10), 1);
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
        StudentTrackingService semesterService = new StudentTrackingService(
                classRepository, studentRepository, recordRepository, mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(trackingClass));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1))
                .thenReturn(List.of(student));
        when(recordRepository.findByStudentIdInAndSemester(List.of(10), 1)).thenReturn(List.of());
        when(recordRepository.findByStudentIdInAndSemester(List.of(10), 2)).thenReturn(List.of());

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
                mock(StudentSemesterRecordRepository.class), mock(StudentTrackingExcelParser.class));
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
                mock(StudentTrackingExcelParser.class));
        when(classRepository.findById(1)).thenReturn(Optional.of(class6A));
        when(studentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(1))
                .thenReturn(List.of(student));

        List<StudentSearchResult> results = searchService.searchStudents("an", 1);

        assertThat(results).extracting(StudentSearchResult::getStudentId)
                .containsExactly(10);
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
