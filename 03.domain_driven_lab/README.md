# Wallet Rich Domain Model

Bài tập refactor `WalletEntity` từ mô hình dữ liệu anemic sang Rich Domain Aggregate Root.

## Quy tắc nghiệp vụ

- `balance` và `status` là `private`; không có setter.
- Chỉ các hành vi domain mới được thay đổi trạng thái ví.
- Không thể rút tiền khi ví bị khóa.
- Không thể rút số tiền lớn hơn số dư.
- Số tiền rút phải lớn hơn 0; vi phạm được báo bằng `DomainException`.
- Ví mới bắt đầu ở trạng thái `ACTIVE` và số dư khởi tạo không âm.

## Ví dụ sử dụng

```java
var wallet = new WalletEntity(UUID.randomUUID(), new BigDecimal("100.00"));
wallet.withdrawMoney(new BigDecimal("25.00"));
wallet.lockWallet();
```

Sau khi khóa, gọi `withdrawMoney` sẽ ném `DomainException`. Aggregate không cung cấp setter để mã bên ngoài thay đổi số dư hoặc trạng thái trực tiếp.