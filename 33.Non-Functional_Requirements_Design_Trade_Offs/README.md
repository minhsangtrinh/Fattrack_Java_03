# Bài tập 33 — Non-Functional Requirements & Design Trade-offs

## Yêu cầu theo đề

Viết một ADR bằng Markdown cho hệ thống thương mại điện tử bị sập giờ cao điểm do tính năng trừ kho gọi trực tiếp MySQL gây khóa; đề xuất kiến trúc cải thiện Performance và Scalability, nêu bối cảnh, quyết định và các đánh đổi chấp nhận.

## Bài làm

Đọc (ADR-001-async-inventory-decrement.md).

Tài liệu gồm:

- Bối cảnh, giả định và mục tiêu phi chức năng có thể đo.
- So sánh MySQL đồng bộ, Kafka, Redis atomic script, distributed lock và in-memory.
- Quyết định Kafka + MySQL, sơ đồ kiến trúc Mermaid, transactional outbox và chống trừ kho lặp.
- Trade-offs về độ trễ kết quả, tính nhất quán, khả năng mở rộng, chi phí và vận hành.
- Xử lý lỗi, backlog, kế hoạch chuyển đổi và điều kiện xem xét lại quyết định.