# Implementation Plan: Student Learning and Assessment

## Overview

Make student learning content and assessment visibility follow the student's current class, while preserving public practice exams. Real exams are attached to curriculum lessons, private graded exams notify their target students, and saved results carry the configured grading month and correct class/subject context.

## Architecture Decisions

- Use `User.trackingClass` as the source of truth for a student's current grade and class.
- Keep practice exams separate from graded real exams; practice results are not written to the gradebook.
- Store the selected grade month on `Exam` as an optional additive field so every result can inherit the teacher's configuration.
- Resolve gradebook ownership from the exam creator's subject/class assignment, never from the submitting student's current page or the server clock.
- Add student-facing notifications as a persistent, target-class record rather than an in-memory message.

## Task List

### Phase 1: Student Scope and Lesson Exams

- [ ] Filter the student home, subject, document, and curriculum routes to the student's class/grade.
- [ ] Show assigned subject teachers and homeroom teacher in the student home context.
- [ ] Load eligible real exams alongside the matching legacy lesson or lesson-plan lesson.
- [ ] Add regression tests for class/grade isolation and lesson exam visibility.

### Phase 2: Grading Contract

- [ ] Add optional grading month to exam creation/update requests and the form.
- [ ] Replace singleton result state with a request-local result and persist real results reliably.
- [ ] Record the configured month and the correct assignment/teacher context in the gradebook.
- [ ] Add tests for month selection, practice exclusion, and teacher/class ownership.

### Phase 3: Notifications and Statistics

- [ ] Create notifications for newly published private graded exams and show unread notifications to target students.
- [ ] Add separate real/practice student score views.
- [ ] Add teacher participation statistics with taken/not-taken state and score/time sorting.

## Verification

- Focused Maven tests for each phase.
- `mvnw.cmd -DskipTests package` for compilation.
- Manual HTTP verification with a student in one target class and a student in another class.

## Risks

- Existing database schemas are managed outside this repository in SQL files; additive columns/tables must remain nullable or be created by Hibernate in the local profile.
- Existing legacy results may be constrained by the old one-to-one user mapping and need a compatibility migration before historical data is rewritten.
