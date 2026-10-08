FROM mysql:5.7

COPY db/webtracnghiem /database-dumps
COPY docker/mysql/init/00-import.sh /docker-entrypoint-initdb.d/00-import.sh

RUN chmod +x /docker-entrypoint-initdb.d/00-import.sh
