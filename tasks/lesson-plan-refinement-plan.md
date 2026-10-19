# Kế hoạch chỉnh sửa Giáo án và phạm vi đề thi

## Mục tiêu

Mở rộng giáo án hiện có thành danh mục chủ đề/bài học có nội dung, file Word và có thể dùng làm phạm vi chọn câu hỏi khi tạo đề thi.

## Phạm vi và quyết định

- Môn học lấy từ danh sách môn hiện có.
- Khối lấy theo phạm vi phân công của giáo viên; school admin/admin được xem toàn bộ khối hiện có.
- Lớp là tùy chọn và chỉ lấy các lớp thuộc khối đã chọn.
- Chủ đề và bài là nội dung riêng của giáo án, không ghi ngược vào chương trình học dùng chung.
- Public/private và quyền sửa/xóa theo chủ sở hữu vẫn giữ nguyên.
- Bài học có nội dung soạn trực tiếp và file Word tùy chọn.
- Tạo đề thi chọn môn -> khối -> chủ đề -> bài; khối bị giới hạn theo cấp của giáo viên.
- Danh sách giáo án có tìm kiếm theo tên giáo án, lớp, môn và giáo viên.
- Các trang giáo án nạp đủ script layout để sidebar hoạt động.

## Các lát cắt

- [x] Lát cắt 1: Xác định dữ liệu phân công môn/khối/lớp và contract form request.
- [x] Lát cắt 2: Thêm model/repository/service lưu chủ đề và bài riêng của giáo án.
- [x] Lát cắt 3: Cập nhật controller, kiểm tra quyền và xử lý create/edit/delete.
- [x] Lát cắt 4: Làm lại UI dạng wizard môn -> khối -> lớp -> chủ đề/bài.
- [ ] Lát cắt 5: Thêm nội dung bài học và upload Word.
- [ ] Lát cắt 6: Nối giáo án vào bộ lọc đề thi môn -> khối -> chủ đề -> bài.
- [ ] Lát cắt 7: Tìm kiếm giáo án và sửa layout/sidebar.
- [ ] Lát cắt 8: Test backend/template, build và kiểm tra runtime.

## Tiêu chí nghiệm thu

- [ ] Giáo viên chỉ thấy các khối/lớp nằm trong phạm vi được phân công.
- [ ] Chọn môn, khối và lớp độc lập; lớp không bắt buộc.
- [ ] Giáo viên tạo được nhiều chủ đề, mỗi chủ đề có nhiều bài ngay trong form.
- [ ] Mỗi bài có nội dung và có thể tải lên file Word.
- [ ] Đề thi không yêu cầu chọn lớp để lọc câu hỏi; chỉ chọn khối, chủ đề và bài trong giáo án được phép xem.
- [ ] Tìm kiếm trả đúng giáo án theo tên, lớp hoặc giáo viên.
- [ ] Sidebar giáo án mở/đóng menu và điều hướng bình thường.
- [ ] Dữ liệu chương/bài của giáo án không làm thay đổi chương trình học chung.
- [ ] Giáo án public/private và quyền sở hữu vẫn hoạt động.
- [ ] Request giả mạo ID môn/khối/lớp hoặc chỉnh dữ liệu ngoài quyền bị từ chối ở server.

## Kiểm tra

- Test focused cho service và template.
- `$env:DB_PASSWORD='root'; $env:DB_PORT='3307'; .\\mvnw.cmd test`
- `docker compose up -d --build app`
- Kiểm tra route `/lesson-plans` và form create trên `http://localhost:8095`.
