# Plan: Tim kiem va xuat Excel so theo doi

## Slice 1: Tim kiem hoc sinh

- Them ket qua tim kiem dang DTO, loc khong dau/khong phan biet hoa thuong tai service.
- Ho tro pham vi tat ca lop hoac mot lop qua query string tren controller.
- Them form tim kiem, bang ket qua va loi tat tim trong lop dang xem.
- Kiem thu service, controller va template.

## Slice 2: Xuat Excel

- Tao exporter Apache POI doc du lieu qua `StudentTrackingService`.
- Them endpoint tai `.xlsx` cho mot lop hoac tat ca lop.
- Them nut xuat tai danh sach va lop dang xem.
- Kiem thu noi dung workbook, response va template.

## Verification

- `./mvnw.cmd '-Dtest=StudentTracking*Test' test`
- `$env:DB_PASSWORD='root'; $env:DB_PORT='3307'; ./mvnw.cmd test`
- `docker compose up -d --build app`
- Kiem tra HTTP tren `http://localhost:8095/student-tracking`.
