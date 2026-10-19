# Plan: Phan cong giang day va thong ke nha truong

## Overview

Add a school-admin workspace for bulk teaching assignments and a school-level
analytics page. School admins are restricted to their own school; full admins
can select an active school. Existing per-class assignment and teacher/student
statistics remain available.

## Architecture decisions

- Reuse `ClassSubjectAssignment` as the source of truth. A class and subject
  have at most one teacher; assigning again updates the existing assignment.
- Keep the existing per-class POST endpoint for backward compatibility and add
  bulk and matrix actions under `/school-admin/teaching-assignments`.
- Calculate first-version school analytics from existing repositories and
  filter by the selected school in the service/controller layer.
- Use a dedicated responsive Thymeleaf page and CSS instead of the legacy
  `student-tracking/sheet.html` form for the new workflow.

## Tasks

### Phase 1: Assignment foundation

- [x] Add bulk and matrix assignment service operations with school-scope validation.
- [x] Add the school-admin assignment controller and focused controller/service tests.

### Phase 2: Assignment workspace

- [x] Add filters, bulk selection, matrix editing, conflict feedback, and responsive styling.
- [x] Add role-aware navigation links for school admins and full admins.

### Checkpoint: Assignment workspace

- [x] Existing per-class behavior remains covered while bulk and matrix actions are added.
- [x] A teacher can be assigned to multiple selected classes in one action.
- [x] A class can be edited across multiple subject columns and existing assignments are updated.

### Phase 3: School analytics

- [x] Add school-scoped analytics aggregation for people, classes, assignments, exams, participation, and scores.
- [x] Add the responsive overview, learning outcomes, exam activity, assignment coverage, and alerts sections.
- [x] Add focused template/controller tests and role protection.

### Checkpoint: Complete

- [x] Focused test suite passes; the full suite has one environment-only MySQL context failure.
- [x] Application compiles successfully.
- [x] New pages render through Thymeleaf tests with empty and populated model paths.
- [x] No existing role or per-class assignment behavior regresses in the available tests.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Legacy pages use different assignment flows | Medium | Keep current endpoints and use shared assignment service semantics |
| Large schools make matrix pages heavy | Medium | Filter by school/year/grade and class search before rendering |
| Result entities can include public exams | Medium | Count school-owned exams and school students separately, with explicit labels |
