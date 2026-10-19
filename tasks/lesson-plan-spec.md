# Feature: Teacher lesson plans and exam scope

## Objective

Teachers create reusable lesson plans independently from the shared curriculum. A plan is scoped by subject, grade and optional concrete class. Its content is organized as topics and lessons. Each lesson can contain rich text and an optional Word attachment. Exam creation uses the teacher's topic/lesson catalog after selecting a grade, without forcing a concrete class.

## Compatibility decisions

- Keep the existing `lesson_plan_custom_chapter` and `lesson_plan_custom_lesson` tables and Java mappings for backward compatibility; the UI and labels call them topics and lessons.
- Existing plans and questions remain readable.
- Store the Word attachment in the lesson-plan lesson record so it follows the plan's ownership and visibility rules.
- Do not mutate the shared subject/class curriculum when a teacher edits a plan.

## Acceptance criteria

- A teacher only sees grades allowed by their school level, and sees only their own lesson plans plus public plans.
- A plan can contain multiple topics and lessons; each lesson saves rich text and an optional `.doc`/`.docx` file.
- The exam form selects subject, allowed grade, topic and lesson from accessible lesson plans. It does not require a concrete class for question scope.
- Search on the lesson-plan list matches plan name, class, teacher and subject.
- The lesson-plan sidebar and Bootstrap interactions work on list, form and detail pages.
- Existing curriculum data remains unchanged and old plans continue to render.
- Server-side authorization and file validation reject invalid grade, hidden plan lesson, unsupported file type and oversized upload.

## Testing strategy

- Unit tests for grade/visibility filtering, lesson content/file validation and exam-scope lookup.
- Template tests for topic labels, content editor, file input, search controls, sidebar scripts and grade/topic/lesson selectors.
- Full Maven test suite before completion.
