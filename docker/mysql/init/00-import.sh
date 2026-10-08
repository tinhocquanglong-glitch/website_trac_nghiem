#!/bin/sh
set -eu

echo "Importing web_trac_nghiem database dump..."

for dump in \
  web_thuc_tap_v2_role.sql \
  web_thuc_tap_v2_user.sql \
  web_thuc_tap_v2_subject.sql \
  web_thuc_tap_v2_classes.sql \
  web_thuc_tap_v2_subject_classes.sql \
  web_thuc_tap_v2_chapter.sql \
  web_thuc_tap_v2_lesson.sql \
  web_thuc_tap_v2_question.sql \
  web_thuc_tap_v2_exam.sql \
  web_thuc_tap_v2_exam_question.sql \
  web_thuc_tap_v2_result.sql \
  web_thuc_tap_v2_document.sql \
  web_thuc_tap_v2_news.sql \
  web_thuc_tap_v2_iq.sql \
  web_thuc_tap_v2_hibernate_sequence.sql \
  web_thuc_tap_v2_user_role.sql
do
  echo "  -> $dump"
  mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" \
    < "/database-dumps/$dump"
done

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "ALTER DATABASE \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

echo "Database import completed."
