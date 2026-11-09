# Bài tập 30 — UML & Design Communication

## 1. Đề bài

Thiết kế chức năng **“Thêm sản phẩm vào giỏ hàng”** bằng hai sơ đồ viết bằng Mermaid:

1. **Class Diagram:** mô tả các đối tượng liên quan và quan hệ giữa chúng.
2. **Sequence Diagram:** mô tả người dùng đã đăng nhập thêm sản phẩm X với số lượng Y; kiểm tra sản phẩm tồn tại; tạo dòng giỏ hàng nếu chưa có hoặc tăng số lượng nếu đã có.

## 2. Tệp bài làm

| Tệp | Nội dung |
| --- | --- |
| [class-diagram.mmd](class-diagram.mmd) | Sơ đồ lớp, thuộc tính, phương thức và bội số quan hệ |
| [sequence-diagram.mmd](sequence-diagram.mmd) | Sơ đồ tuần tự có đầy đủ nhánh xử lý |
| `README.md` | Giải thích thiết kế, quy tắc và ví dụ đối chiếu |

Hai tệp `.mmd` là mã nguồn Mermaid hoàn chỉnh. Có thể mở bằng trình xem Mermaid trong IDE; nếu dùng Markdown, đặt toàn bộ nội dung từng tệp trong một khối mã có nhãn `mermaid`. Bài tập chỉ yêu cầu sơ đồ nên không bổ sung ứng dụng Java hoặc Spring Boot.

## 3. Giải thích Class Diagram

| Thành phần | Trách nhiệm |
| --- | --- |
| `CartController` | Nhận request; lấy người dùng từ ngữ cảnh xác thực; gọi service; ánh xạ kết quả/lỗi sang HTTP |
| `CartService` | Kiểm tra số lượng; tìm sản phẩm; tải hoặc tạo giỏ của người dùng; yêu cầu cập nhật và lưu giỏ |
| `ProductRepository` | Interface tìm sản phẩm theo ID; trả `Optional.empty()` nếu không tồn tại |
| `CartRepository` | Interface tìm giỏ theo người dùng và lưu toàn bộ giỏ cùng các dòng sản phẩm |
| `Cart` | Quản lý danh sách `CartItem`; quyết định thêm dòng mới hay tăng số lượng dòng cũ |
| `CartItem` | Giữ sản phẩm và số lượng; thực hiện tăng số lượng |
| `Product` | Thông tin sản phẩm: ID, tên, đơn giá |
| `User` | Danh tính người dùng đã được xác thực |

### Quan hệ và bội số

- `User "1" -- "0..1" Cart`: mỗi giỏ thuộc đúng một người dùng; một người dùng có thể chưa có giỏ hoặc có một giỏ đang hoạt động. Phạm vi bài không mô hình hóa lịch sử giỏ hàng.
- `Cart "1" *-- "0..*" CartItem`: quan hệ **composition**. Một giỏ có thể rỗng hoặc có nhiều dòng; mỗi dòng thuộc đúng một giỏ và có vòng đời gắn với giỏ đó.
- `CartItem "0..*" --> "1" Product`: mỗi dòng tham chiếu đúng một sản phẩm; một sản phẩm có thể được tham chiếu bởi nhiều dòng ở các giỏ khác nhau. Đây là association, không phải composition: xóa giỏ không làm xóa sản phẩm.
- `CartController --> CartService` và `CartService --> ...Repository`: đối tượng giữ dependency để gọi nghiệp vụ hoặc truy cập dữ liệu.
- Các mũi tên nét đứt `..>` biểu diễn dependency sử dụng kiểu đối tượng hoặc tạo/trả về đối tượng.

`Cart.userId` biểu diễn khóa liên kết tới `User.id`. `CartItem.product` biểu diễn tham chiếu tới `Product`. Trong cùng một giỏ, mỗi `productId` chỉ xuất hiện ở **một** dòng; thêm lại sản phẩm phải cộng số lượng vào dòng đó.

`Cart.addProduct()` giữ quy tắc thêm/cộng số lượng bên trong giỏ. `CartService` điều phối các bước và lưu dữ liệu, không trực tiếp chỉnh danh sách `items`.

## 4. Giải thích Sequence Diagram

### Tiền điều kiện và request

Người dùng đã đăng nhập, ngữ cảnh xác thực cung cấp `authenticatedUser`. Ví dụ request:

```http
POST /cart/items
Content-Type: application/json

{
  "productId": "UUID-cua-san-pham-X",
  "quantity": 2
}
```

Giá trị `productId` trong ví dụ là ký hiệu minh họa, khi triển khai phải là UUID hợp lệ. Request không chứa `userId`; controller dùng ID của người dùng đã xác thực để chọn đúng giỏ.

### Luồng chính

1. `CartController` nhận sản phẩm X và số lượng Y, gọi `CartService.addItem(userId, X, Y)`.
2. Service kiểm tra Y là số nguyên dương hợp lệ, rồi gọi `ProductRepository.findById(X)`.
3. Khi sản phẩm tồn tại, service gọi `CartRepository.findByUserId(userId)`; nếu chưa có giỏ thì tạo `Cart(userId)` với danh sách rỗng.
4. Service gọi `Cart.addProduct(product, Y)`. Giỏ tìm dòng theo `productId`.
5. Nếu chưa có dòng: tạo `CartItem(product, Y)` rồi thêm vào giỏ. Nếu đã có: gọi `increaseQuantity(Y)`, cộng Y vào số lượng hiện tại.
6. Service gọi `CartRepository.save(cart)` và nhận giỏ đã lưu.
7. Controller trả `HTTP 200 OK` cùng giỏ đã cập nhật. Cả tạo dòng mới và tăng dòng cũ đều cập nhật tài nguyên giỏ nên dùng chung phản hồi này.

### Nhánh thay thế

| Điều kiện | Xử lý | Kết quả |
| --- | --- | --- |
| Y không hợp lệ, ví dụ 0 hoặc âm | Dừng trước khi truy vấn repository | `400 Bad Request`; không sửa giỏ |
| Không tìm thấy sản phẩm X | Dừng trước khi tải/tạo giỏ | `404 Not Found`; không sửa giỏ |
| Người dùng chưa có giỏ | Tạo giỏ rồi thêm dòng sản phẩm mới | Lưu giỏ, trả `200 OK` |
| Có giỏ, chưa có X | Tạo một `CartItem` mới với số lượng Y | Lưu giỏ, trả `200 OK` |
| Có giỏ, đã có X | Cộng Y vào số lượng dòng hiện tại | Lưu giỏ, trả `200 OK` |

Ký hiệu `alt/else` mô tả các nhánh loại trừ nhau; `opt` mô tả bước chỉ chạy khi chưa có giỏ; `autonumber` đánh số thông điệp. Nhãn `«create»` thể hiện thao tác khởi tạo đối tượng. Lifeline `CartItem` đại diện cho dòng mới hoặc dòng hiện có theo nhánh tương ứng.

`InvalidQuantity` và `ProductNotFound` trong sơ đồ là tên kết quả lỗi nghiệp vụ, không phải các phương thức hoặc lớp bổ sung bắt buộc. Request sai cú pháp hoặc sai kiểu dữ liệu bị từ chối tại lớp nhận request; sơ đồ tập trung vào luồng nghiệp vụ sau khi đọc được X và Y.

## 5. Ví dụ đối chiếu

Giả sử sản phẩm X tồn tại:

| Trước thao tác | Thao tác | Sau thao tác |
| --- | --- | --- |
| Chưa có giỏ | Thêm X, Y = 2 | Tạo giỏ và một dòng X, số lượng 2 |
| Có giỏ rỗng | Thêm X, Y = 2 | Một dòng X, số lượng 2 |
| Giỏ có X, số lượng 3 | Thêm X, Y = 2 | Vẫn một dòng X, số lượng 5 |
| Giỏ chỉ có sản phẩm Z | Thêm X, Y = 2 | Giữ Z và thêm một dòng X, số lượng 2 |
| Giỏ bất kỳ | Thêm X, Y = 0 | Báo lỗi; giỏ không đổi |
| Giỏ bất kỳ | Thêm sản phẩm không tồn tại, Y = 2 | Báo không tìm thấy; giỏ không đổi |

Hậu điều kiện khi thành công: chỉ giỏ của người dùng hiện tại được cập nhật; X xuất hiện đúng một dòng với số lượng mới bằng số lượng cũ cộng Y (coi số lượng cũ là 0 nếu chưa có).

## 6. Giả định và phạm vi

- Sơ đồ mô tả xử lý tuần tự. Khi triển khai nhiều request đồng thời, cần transaction và cơ chế ngăn mất cập nhật hoặc tạo trùng dòng; chi tiết đó nằm ngoài đề bài.
- `save(cart)` có hợp đồng lưu giỏ cùng các dòng, tạo mới hoặc cập nhật tùy giỏ đã tồn tại. Chỉ phản hồi thành công sau khi lưu thành công; sơ đồ không mở rộng nhánh lỗi hạ tầng.
- Số lượng sau khi cộng phải vẫn là số nguyên dương trong giới hạn biểu diễn. Domain cần bảo vệ điều kiện này khi hiện thực.
- Đề không yêu cầu kiểm tra tồn kho, đặt giữ hàng, tính khuyến mãi hay thanh toán; thêm vào giỏ không đồng nghĩa trừ tồn kho.
- Đây là bài thiết kế UML bằng Mermaid, không có mã ứng dụng cần build/test. Chưa render hai sơ đồ bằng công cụ trong workspace.
