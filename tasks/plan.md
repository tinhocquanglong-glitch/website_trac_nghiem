# Plan: So theo doi theo thang

## Slice 1: Du lieu theo thang

- Them bang `student_monthly_record` voi khoa duy nhat hoc sinh, hoc ky va thang.
- Giu `tracked_student` la danh sach hoc sinh cap lop de ten xuat hien trong moi thang cua ca hai hoc ky.
- Doc du lieu hoc ky cu lam du phong tai thang 9 (HK1) va thang 2 (HK2), khong sua/xoa du lieu cu.
- Kiem thu quy tac thang, doc/luu theo thang va tuong thich du lieu cu.

## Slice 2: Giao dien va nhap Excel

- Them tham so thang vao controller, form luu, chuyen lop va cac redirect.
- Them tab thang theo tung hoc ky va bo chon thang trong form import Excel.
- Import hoc sinh vao danh sach cap lop, luu diem/nhan xet/diem danh vao thang da chon.
- Kiem thu controller va template.

## Slice 3: Xuat Excel

- Giu hai sheet HK1/HK2 cho moi lop.
- Xuat lien tiep tung bang thang trong moi sheet, moi bang co tieu de DGTX hai tang va dropdown diem danh.
- Kiem thu vi tri du lieu, vung dropdown va day du 5/4 thang.

## Verification

- `.\\mvnw.cmd '-Dtest=StudentTracking*Test' test`
- `$env:DB_PASSWORD='root'; $env:DB_PORT='3307'; .\\mvnw.cmd test`
- `docker compose up -d --build app`
- Kiem tra HTTP va bang moi tren Docker.
