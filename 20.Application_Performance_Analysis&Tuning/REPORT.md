# Application Performance Analysis & Tuning

## 1. N+1 khi đọc tác giả và sách

**Vấn đề:** Author.books là LAZY. Nếu gọi findAll() rồi truy cập books của từng Author trong transaction, có thể phát sinh 1 query tác giả + N query sách. LAZY riêng nó không phải lỗi; cách truy cập quan hệ mới gây N+1.

**Cách xác định:** đọc mapping trong ảnh và suy luận đường truy cập trên. Đã bật org.hibernate.SQL=DEBUG để khi chạy có thể kiểm tra các SELECT books lặp theo author_id.

**Sửa:** AuthorRepository.findAllWithBooks() dùng:

~~~jpql
select distinct a from Author a left join fetch a.books order by a.id
~~~

LEFT JOIN giữ cả tác giả không có sách. Service chuyển sang DTO trong transaction; không serialize entity hai chiều hoặc kích hoạt lazy load khi trả JSON.

Với 20 tác giả, số SELECT dự kiến đổi từ 21 thành 1 nếu persistence context ban đầu trống, không có batching/cache. Đây là dự kiến từ query, không phải số đo. Không dùng fetch join collection này để phân trang trực tiếp.

## 2. Query lọc năm xuất bản thiếu index

**Vấn đề giả định:** query WHERE publication_year = ? trên bảng Book lớn nhưng không có index phù hợp có thể phải quét bảng.

**Cách xác định:** entity trong ảnh chưa khai báo index. Điều này chưa chứng minh DB thật thiếu index vì index có thể được tạo bằng migration; bài demo tạo schema mới để thể hiện bản sửa.

**Sửa:** thêm @Index(name = "idx_books_publication_year", columnList = "publication_year") và map tên cột rõ ràng. BookRepository.findByYear() dùng parameter, chỉ lấy các trường DTO cần trả.

SQL tương đương khi áp dụng vào schema có sẵn:

~~~sql
CREATE INDEX idx_books_publication_year ON books (publication_year);
EXPLAIN SELECT id, title, publication_year
FROM books WHERE publication_year = 2020 ORDER BY id;
~~~

Demo đã để Hibernate tạo index qua @Index, không chạy lại CREATE INDEX trên cùng schema. Kiểm tra execution plan trên dữ liệu thực; bảng nhỏ hoặc điều kiện ít chọn lọc vẫn có thể dùng table scan. Index tăng chi phí ghi và lưu trữ.
