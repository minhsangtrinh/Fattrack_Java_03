# Assignment — JVM Performance Engineering

## 1. Vấn đề tìm thấy

Đoạn /process gây **High GC Churn**: tạo nhiều object tạm thời, khiến GC chạy thường xuyên và xử lý chậm. Chưa có bằng chứng memory leak vì các chuỗi cũ không bị giữ tham chiếu lâu dài.

## 2. Bằng chứng

Trích log thật từ lần đo trước trong cuộc trò chuyện, dùng OpenJDK 21.0.8, G1 GC và heap 256 MiB:

~~~text
[2026-11-04T20:39:16.479+0700][0.144s][info][gc          ] GC(2) Pause Young (Normal) (G1 Evacuation Pause) 152M->0M(256M) 1.043ms
[2026-11-04T20:39:18.817+0700][2.481s][info][gc          ] GC(168) Pause Young (Concurrent Start) (G1 Humongous Allocation) 212M->1M(256M) 0.912ms
~~~

Heap giảm mạnh sau GC cho thấy phần lớn bộ nhớ vừa cấp phát có thể thu hồi.

| Chỉ số | Code gốc | StringBuilder |
|---|---:|---:|
| Thời gian trung vị, 3 lượt | 7411,172 ms | 1,073 ms |
| Cấp phát cộng dồn mỗi lượt | Khoảng 62,53 GiB | Khoảng 1,90 MiB |
| Số lần GC trong mỗi lượt đo | 713–723 | 0 |
| Độ dài kết quả | 938.890 | 938.890 |

Hai bản có cùng SHA-256 của kết quả. Đây là đo riêng vòng lặp 150.000 lần, không phải thời gian HTTP API; allocation cộng dồn không phải lượng RAM dùng đồng thời. File evidence gốc hiện không còn trong folder; số liệu và đoạn log trên được chép lại từ kết quả đã thu trước đó, không chạy đo mới.

## 3. Nguyên nhân gốc

String bất biến. Câu lệnh sau phải tạo kết quả mới và sao chép toàn bộ phần chuỗi đã có ở mỗi vòng:

~~~java
result += " " + i;
~~~

Chuỗi càng dài, lượng sao chép càng lớn. Các kết quả cũ trở thành rác, gây áp lực cấp phát và GC. Vì vậy chi phí không chỉ nằm ở GC mà còn ở việc sao chép dữ liệu lặp lại.

## 4. Cách sửa và JVM flags

Dùng StringBuilder cục bộ, cấp trước dung lượng để hạn chế mở rộng buffer:

~~~java
@GetMapping("/process")
public String processData() {
    long startTime = System.nanoTime();
    StringBuilder builder = new StringBuilder(150_000 * 7);

    for (int i = 0; i < 150_000; i++) {
        builder.append(' ').append(i);
    }

    String result = builder.toString();
    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
    return "Processing finished in " + elapsedMs
            + "ms. Result length: " + result.length();
}
~~~

Builder ghi nối tiếp, tránh sao chép toàn bộ prefix ở mỗi vòng. System.nanoTime dùng để đo khoảng thời gian; builder cục bộ tránh chia sẻ dữ liệu giữa các request.

Các JVM flags đề xuất:

~~~text
-Xms256m -Xmx256m
-XX:+UseG1GC
-Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags:filecount=3,filesize=10M
~~~

- Heap 256 MiB dùng để tái hiện và so sánh; khi vận hành cần chọn heap theo tải và RAM khả dụng.
- G1GC là điểm bắt đầu; GC log giúp theo dõi tần suất, pause và lượng heap thu hồi.
- Có thể thử -XX:MaxGCPauseMillis=100 nếu cần giảm pause, nhưng đây là mục tiêu mềm và chưa được đo trong bài.

Ưu tiên sửa thuật toán trước; tăng heap hoặc đổi GC không loại bỏ việc nối chuỗi kém hiệu quả.
