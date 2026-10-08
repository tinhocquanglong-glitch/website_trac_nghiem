# Spec: So theo doi hoc sinh

## Objective

Them khu vuc quan tri doc lap de giao vien dang nhap bang `ROLE_ADMIN` quan ly
so theo doi theo lop va hoc ky. Danh sach hoc sinh khong phu thuoc tai khoan
hoc sinh hay du lieu dang ky mon hoc.

## Scope

- Tao lop thu cong voi ten lop va nam hoc.
- Them, sua va xoa hoc sinh trong lop.
- Quan ly rieng hoc ky 1 va hoc ky 2 voi cac cot: hai diem DGTX, DGK, DCK,
  DTBM, nhan xet va trang thai diem danh.
- Trang thai diem danh gom: de trong, Vang, Bo hoc, Chuyen truong.
- Import `.xls`/`.xlsx`: moi sheet co cot `HO VA TEN` la mot lop; ten lop lay
  tu ten sheet, nam hoc lay tu tieu de. Sheet phu khong dung mau bi bo qua.
- Khi import trung ten lop va nam hoc, cap nhat hoc sinh trung ten va them hoc
  sinh moi, khong tao lop trung lap.
- Tim hoc sinh theo ten tren tat ca lop hoac gioi han trong mot lop. Tim kiem
  khong phan biet chu hoa, chu thuong va dau tieng Viet.
- Xuat workbook `.xlsx` cho mot lop hoac tat ca lop. Moi lop co hai sheet HK1
  va HK2, gom day du diem, nhan xet va trang thai diem danh.

## Boundaries

- Tat ca endpoint nam duoi `/student-tracking/**` va chi `ROLE_ADMIN` truy cap.
- File import toi da 5 MB, 50 sheet va 200 hoc sinh moi sheet.
- Diem neu co phai nam trong khoang 0-10; ten lop, nam hoc va ten hoc sinh
  duoc kiem tra o phia server.
- Khong ghi file upload xuong dia; workbook duoc doc truc tiep tu request.
- Khong them thu vien Excel moi; tiep tuc dung Apache POI da co trong du an.
- Tim kiem khong sua du lieu va chi tra ket qua khi co tu khoa.

## Success Criteria

- Du lieu ton tai trong MySQL qua JPA va tach rieng theo lop/hoc ky.
- Admin co the tao lop, them hoc sinh, chuyen hoc ky, sua ca bang va luu.
- Workbook mau `SO THEO DOI.xlsx` import duoc 12 lop va bo qua sheet phu.
- Giao dien bang bam sat cac cot trong workbook va hoat dong tren man hinh nho.
- Admin co the tim mot hoc sinh trong lop duoc chon hoac tren tat ca lop va mo
  nhanh so theo doi cua lop tu ket qua.
- Admin co the tai workbook cua lop dang xem hoac workbook tong hop tat ca lop;
  ten sheet phan biet lop, nam hoc va hoc ky.
- Test parser, validation va build Maven thanh cong.
