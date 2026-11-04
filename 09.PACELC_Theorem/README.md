# Assignment 09 — PACELC Theorem: Thiết kế hệ thống Flash Sale

## 1. Bối cảnh và yêu cầu

Thiết kế Flash Sale phục vụ **50.000 người dùng đồng thời**, với:

- SLO P95 read < 80 ms, P95 write < 120 ms.
- Không được oversell.
- Trang listing được hiển thị tồn kho trễ tối đa 2 giây.
- Thanh toán phải chính xác.

Bài nộp gồm phân loại PACELC cho listing stock, add-to-cart, checkout; chọn DB/topology; đề xuất cấu hình; vẽ sơ đồ Mermaid; phân tích lag, thundering herd, retry và idempotency.

**Quyết định chính:** listing ưu tiên tốc độ trong giới hạn freshness; cart không giữ hàng; checkout giữ hàng bằng transaction trên PostgreSQL primary. Xác nhận thanh toán là một luồng bất đồng bộ có đối soát.

## 2. PACELC và phân loại từng luồng

PACELC mô tả hai đánh đổi:

- **If Partition (P):** chọn Availability (A) hay Consistency (C) khi mạng bị chia cắt.
- **Else (E):** khi mạng bình thường, chọn Latency (L) hay Consistency (C).

C trong CAP có nghĩa chặt là linearizability. Cấu hình quorum hoặc synchronous replication đơn lẻ không tự động tạo ra bảo đảm này; còn phụ thuộc đường đọc/ghi, bầu leader và fencing. Các nhãn dưới đây mô tả chính sách của từng luồng cùng các giới hạn cụ thể.

| Luồng | Phân loại | Khi partition | Khi mạng bình thường |
|---|---|---|---|
| Listing stock | **PA/EL có giới hạn freshness** | Trả snapshot còn trong cửa sổ 2 giây. Hết cửa sổ thì ẩn số tồn hoặc báo chưa khả dụng | Đọc Redis cache, chấp nhận dữ liệu cũ trong giới hạn |
| Add-to-cart | **PA/EL** cho cart best effort | Cho phép ghi nếu đến được replica Cassandra của partition; các bản sao cart có thể tạm khác nhau | Đọc/ghi ONE để giảm số replica cần chờ |
| Checkout, giữ hàng và trạng thái tiền | **PC/EC** | Chỉ xử lý trên primary hợp lệ có standby đồng bộ; thiếu điều kiện thì dừng ghi | Dùng transaction, khóa/conditional update và synchronous commit |

**Lưu ý listing:** yêu cầu freshness tối đa 2 giây giới hạn availability. Sau partition dài hơn cửa sổ đó, không thể vừa tiếp tục trả con số tồn cũ vừa bảo đảm yêu cầu đề bài. Vì vậy đây không phải cam kết AP vô điều kiện. Trang vẫn có thể trả tên, ảnh, giá và thông báo “Tồn kho đang cập nhật”, nhưng thao tác đọc số tồn đã suy giảm availability.

**Lưu ý cart:** việc thêm vào giỏ không cam kết có hàng, không trừ tồn và không chốt giá. Nếu nghiệp vụ yêu cầu cart giữ hàng hoặc tuyệt đối không mất thay đổi đã xác nhận, phải chuyển phần đó sang PC/EC và chấp nhận chi phí tương ứng.

## 3. DB và topology

| Thành phần | DB/topology đề xuất | Vai trò |
|---|---|---|
| Catalog và tồn kho nguồn | PostgreSQL primary A, synchronous standby B cùng region, khác AZ | Nguồn dữ liệu có thẩm quyền |
| Orders, reservations, payment ledger, idempotency, outbox | Cùng PostgreSQL cluster với tồn kho | Ghi trong cùng local transaction khi cần bảo đảm nguyên tử |
| Listing stock | Redis Cluster; mỗi shard có replica bất đồng bộ | Cache có thể tái tạo, không dùng quyết định bán hàng |
| Cart | Cassandra, 3 node trong một data center, RF=3 | Lưu ý định mua, chấp nhận eventual consistency |
| Bầu leader PostgreSQL | Bộ điều phối có 3 consensus voter và cơ chế fencing | Chặn split-brain, chọn primary hợp lệ |
| Thanh toán | Payment provider + worker + webhook inbox | Giao tiếp ngoài DB; retry và reconciliation |

PostgreSQL phù hợp với transaction nhiều dòng giữa đơn hàng và tồn kho. Redis phục vụ phần lớn lượt đọc listing. Cassandra tách lưu lượng ghi cart khỏi database giữ hàng. SQL/NoSQL không tự quyết định PACELC; chính cấu hình và nghiệp vụ tạo ra đánh đổi.

Thiết kế ban đầu đặt luồng ghi trong một region để giảm RTT. Chưa dùng active-active cho checkout. Nếu mở rộng nhiều region, vẫn định tuyến checkout về region có primary; độ trễ tăng và phải đánh giá lại SLO.

## 4. Cấu hình và knobs

### 4.1. Listing: cache nhanh nhưng có giới hạn 2 giây

- Chỉ lấy snapshot nguồn từ PostgreSQL primary; không đọc replica có lag chưa biết.
- Refresh theo batch cho các SKU đang flash sale, chu kỳ mục tiêu 500 ms. Gộp request cùng SKU để tránh mỗi client tự đọc DB.
- Snapshot chứa available, version, source_snapshot_at. Timestamp được lấy lúc bắt đầu câu đọc nguồn, không lấy lúc cache nhận dữ liệu.
- Chỉ trả số tồn nếu tuổi snapshot, cộng sai số đồng hồ đã dự trù, không quá 2 giây. Ví dụ dùng ngưỡng vận hành 1,8 giây khi đã kiểm soát tổng sai số trong 200 ms.
- Redis TTL 1 giây là lớp dọn cache bổ sung; kiểm tra source_snapshot_at vẫn là điều kiện bắt buộc. Retry refresh không được làm mới timestamp của snapshot cũ.
- Refresh chậm, clock uncertainty vượt giới hạn hoặc không đọc được nguồn: không trả con số tồn đã hết hạn. Có thể trả 503 cho API số tồn hoặc trạng thái stock_unknown để UI ẩn số.
- Update cache chỉ chấp nhận version mới hơn; dùng generation tăng sau failover và sequence trong generation để snapshot trễ không ghi đè snapshot mới.
- Redis failover có thể làm mất cache hoặc khôi phục bản cũ. Luôn kiểm tra freshness sau failover; cache trống không đồng nghĩa hết hàng.

Redis replication không được xem là đồng bộ. Listing cache có thể ghi “còn hàng” trong khi checkout đã hết; checkout luôn kiểm tra lại nguồn.

### 4.2. Cart: Cassandra PA/EL

- NetworkTopologyStrategy với RF=3 tại data center hiện hành.
- Read consistency ONE, write consistency ONE; không dùng LWT cho cart.
- Partition theo user_id và sale_id; mỗi item có SKU và quantity.
- API đặt quantity tuyệt đối thay vì phép tăng không idempotent. Retry nội bộ của cùng một mutation giữ nguyên payload và write timestamp; không tự phát lại mutation cũ sau một chỉnh sửa mới.
- Khi hai thiết bị sửa cùng item, chấp nhận quy tắc last-write-wins và có thể mất một thay đổi. Retry của client sau response không rõ phải đọc lại cart và xác nhận trạng thái mong muốn.
- TTL theo vòng đời sale chỉ phục vụ dọn dữ liệu, không phải thời hạn giữ hàng.

ONE không bảo đảm read-your-writes hay durability nếu replica duy nhất đã xác nhận bị mất trước khi đồng bộ. Mất cart chỉ ảnh hưởng trải nghiệm; không làm sai tồn kho hoặc tiền. Checkout nhận danh sách SKU/quantity đã được người dùng xác nhận trong request, không lấy một bản cart có thể cũ làm mệnh lệnh mua hàng.

### 4.3. Checkout: PostgreSQL PC/EC

| Knob/chính sách | Giá trị/ý nghĩa |
|---|---|
| Đường đọc/ghi | Primary đã được xác thực vai trò |
| synchronous_standby_names | 'FIRST 1 (pg_b)' khi A là primary, B là synchronous standby |
| synchronous_commit | remote_apply cho transaction giữ hàng, đơn hàng và tiền |
| Isolation | READ COMMITTED cho transaction ngắn có conditional update/row lock |
| Multi-SKU | Khóa SKU theo thứ tự cố định; mọi item cùng transaction |
| Constraints | available >= 0, quantity > 0; unique theo idempotency key và provider reference |
| Mất synchronous standby | Dừng ghi quan trọng; không tự hạ xuống asynchronous commit |
| Failover | Fence primary cũ; chỉ promote standby đồng bộ đủ điều kiện |

READ COMMITTED riêng nó không ngăn oversell. Tính đúng đắn dựa vào conditional update và khóa dòng trong cùng transaction. Với invariant phức tạp hơn như giới hạn mua theo nhiều đơn, cần thêm khóa/ràng buộc phù hợp hoặc SERIALIZABLE và retry có giới hạn.

remote_apply chờ WAL được apply trên B, tăng độ trễ. Cấu hình này cũng không thay thế leader election/fencing. Khi không fence được primary cũ, phải dừng ghi. Sau khi promote B, thiết lập lại standby đồng bộ đủ điều kiện trước khi mở lại luồng ghi quan trọng; failover có thể gây gián đoạn.

## 5. Cách ngăn oversell

available là số lượng có thể tiếp tục bán, đã loại phần đang giữ cho đơn chưa hoàn tất.

Transaction tạo checkout:

1. Nhận user từ phiên đã xác thực; kiểm tra SKU/quantity, tính giá và tổng tiền từ dữ liệu server.
2. Tạo bản ghi idempotency với khóa unique theo user + operation + key và hash payload. Cùng key khác payload trả conflict; cùng key cùng payload trả đơn đã tạo.
3. Sắp xếp SKU để thứ tự lấy khóa nhất quán.
4. Với từng SKU, thực hiện conditional update sau:

~~~sql
UPDATE inventory
SET available = available - :quantity
WHERE sale_id = :sale_id
  AND sku_id = :sku_id
  AND available >= :quantity
RETURNING available;
~~~

5. Nếu một item không trả dòng nào, rollback toàn bộ transaction, không tạo đơn nửa chừng.
6. Ghi order trạng thái PENDING_PAYMENT, reservation và sự kiện outbox trong cùng transaction.
7. Commit với synchronous replication rồi trả 202 Accepted kèm order_id.

PostgreSQL khóa dòng khi update. Hai checkout tranh SKU cuối cùng không thể cùng trừ thành công nếu điều kiện tồn kho không còn đủ. CHECK available >= 0 là lớp bảo vệ bổ sung.

Đây là SQL minh họa có parameter, không phải script thực thi độc lập. Không đọc tồn trước rồi thực hiện một phép trừ vô điều kiện ở transaction khác.

Khi thanh toán thành công, chuyển reservation từ HELD sang CONSUMED; không trừ available lần nữa. Khi đủ điều kiện hủy, chỉ một transaction chuyển HELD sang RELEASED và cộng lại quantity. Chuyển trạng thái có điều kiện ngăn worker retry cộng tồn hai lần.

## 6. Thanh toán chính xác và xử lý timeout

DB nội bộ và payment provider không có một transaction ACID chung. Dùng state machine, idempotency, outbox/inbox và đối soát:

1. Worker nhận outbox sau khi reservation đã commit.
2. Gọi provider với khóa idempotency ổn định cho payment attempt và merchant order reference. Số tiền lưu bằng số nguyên đơn vị tiền nhỏ nhất, gắn currency; không dùng float hoặc giá từ client.
3. Khi timeout, đặt trạng thái PAYMENT_UNKNOWN/PENDING và tra cứu provider trước khi quyết định gọi lại. Không tạo payment attempt mới chỉ vì chưa nhận response.
4. Webhook phải xác minh chữ ký và đối chiếu order, currency, amount. Unique provider event ID loại sự kiện trùng.
5. Trong một DB transaction, ghi payment ledger và chuyển trạng thái order/reservation có điều kiện. Webhook cũ không được kéo trạng thái PAID quay lại PENDING.
6. Reconciliation worker truy vấn các giao dịch chưa rõ kết quả, phát hiện lệch giữa provider và ledger. Nếu cửa sổ idempotency của provider đã hết, tra cứu theo reference trước khi gửi yêu cầu có thể thu tiền lần nữa.

Hết thời hạn giữ hàng chỉ kích hoạt quy trình hủy/đối soát. **Không tự trả hàng về kho khi payment còn UNKNOWN**, vì provider có thể đã thu tiền. Chỉ release sau khi xác nhận chưa thu tiền, hoặc đã void/refund thành công theo chính sách. Các bản ghi chưa thể xác minh được đưa sang xử lý thủ công và vẫn giữ invariant tồn kho.

Provider cần hỗ trợ idempotency hoặc truy vấn theo reference đủ tin cậy; nếu thiếu các khả năng đó, không thể khẳng định exactly-once charge qua một mạng không tin cậy. Bảo đảm thiết kế hướng tới là một hiệu ứng nghiệp vụ cho cùng payment attempt, với retry và reconciliation, không phải exactly-once delivery.

## 7. Architecture diagram

~~~mermaid
flowchart TD
    U["50.000 concurrent users"] --> EDGE["Gateway: auth, rate limit, admission control"]
    EDGE --> LIST["Listing API"]
    EDGE --> CART["Cart API"]
    EDGE --> CHECK["Checkout API"]

    LIST --> FRESH{"Snapshot age within 2s?"}
    REDIS[("Redis Cluster: stock snapshots")] --> FRESH
    FRESH -->|Yes| DATA["Return stock"]
    FRESH -->|No| UNKNOWN["Hide count / stock_unknown / 503"]

    REFRESH["Batch snapshot refresher: target 500ms"] -->|Read primary| PG[("PostgreSQL A: primary")]
    REFRESH -->|Timestamp + version + TTL| REDIS
    CART -->|RF=3, read/write ONE| CASS[("Cassandra: cart intent only")]
    CHECK -->|Atomic reservation + order + outbox| PG
    PG -->|Synchronous WAL apply| STANDBY[("PostgreSQL B: sync standby")]
    COORD["3 consensus voters + fencing"] -.-> PG
    COORD -.-> STANDBY

    PG -->|Committed outbox| WORKER["Payment worker"]
    WORKER -->|Stable idempotency key| PSP["Payment provider"]
    PSP --> HOOK["Verified webhook + inbox deduplication"]
    HOOK -->|Conditional state transitions| PG
    RECON["Reconciliation / cancellation worker"] --> PSP
    RECON --> PG
~~~

File sơ đồ riêng: **architecture.mmd**. Cart không nối trực tiếp sang luồng trừ tồn; request checkout mang danh sách mua đã xác nhận và được kiểm tra lại ở server.

## 8. Thiết kế cho tải 50k và SLO

50k concurrent user không tự xác định số request/giây. Một giả định sizing ban đầu:

| Hành vi | Giả định | Tải ước tính |
|---|---|---|
| Listing | 50k user đọc mỗi 5 giây | 10.000 read/s |
| Cart | 10% user ghi mỗi 5 giây | 1.000 write/s |
| Checkout | 2% user gửi mỗi 5 giây | 200 checkout/s |

Đây là đầu vào thiết kế, chưa phải benchmark. Burst lúc mở sale và một SKU cực hot có thể vượt xa giá trị trung bình. API stateless có thể mở rộng ngang; thêm API instance không loại bỏ tranh chấp trên cùng một dòng tồn kho.

### Phạm vi đo SLO

- Đo độ trễ từ lúc gateway nhận request đến lúc trả response, bao gồm thời gian admission/queue và DB.
- Read gồm listing, cart và trạng thái đơn; tách metric theo endpoint để một endpoint chậm không bị trung bình chung che đi.
- Write gồm cập nhật cart và tạo reservation/checkout được chấp nhận.
- P95 write < 120 ms cho checkout là **thời gian trả kết quả reservation**, không phải thời gian thanh toán hoàn tất tại provider. Nếu đề bài yêu cầu toàn bộ thanh toán hoàn tất trong 120 ms, cần thêm cam kết từ provider và đánh giá lại tính khả thi.
- Theo dõi riêng tỷ lệ thành công, 429/503, timeout, queue wait và stock freshness; không dùng trả lỗi nhanh để tuyên bố đạt SLO.

### Ngân sách độ trễ định hướng

| Luồng | Tổng ngân sách mục tiêu | Phân bổ thiết kế |
|---|---|---|
| Listing | 65 ms | Gateway/network 20 + app 10 + Redis 15 + response 20 |
| Cart write | 100 ms | Gateway/network 20 + validation 10 + Cassandra 50 + response 20 |
| Checkout accepted | 110 ms | Gateway/network 20 + validation/idempotency 15 + DB lock/commit/replication 60 + response 15 |

Các số trên là ngân sách đề xuất, không phải kết quả đo hay phép cộng các percentile thành một bảo đảm thống kê.

Biện pháp: cache listing; refresh theo batch; pool DB có giới hạn; giới hạn lượng checkout đang chạy; queue có giới hạn; deadline và lock timeout theo ngân sách còn lại. Không mở transaction DB trong lúc chờ payment provider. Request không được nhận vào xử lý trả 429/503 có hướng dẫn retry; timeout sau khi có thể đã commit phải truy vấn lại cùng idempotency key.

Với SKU hot, bắt đầu bằng admission control theo sale/SKU và transaction ngắn. Nếu sau khi đo vẫn không đạt, có thể nghiên cứu chia quota tồn kho được cấp phát trước; chưa đưa cơ chế đó vào thiết kế cơ sở vì cần invariant bổ sung để tránh oversell.

## 9. Rủi ro và cách giảm thiểu

| Rủi ro | Hậu quả | Cách giảm thiểu |
|---|---|---|
| Cache/replica lag | Listing hiển thị tồn quá cũ | Timestamp từ lần đọc nguồn, giới hạn tuổi dữ liệu; không đọc replica không biết lag |
| Refresh hoặc snapshot đến sai thứ tự | Cache quay về dữ liệu cũ | So sánh generation/version, không refresh tuổi snapshot khi retry |
| Thundering herd lúc cache hết hạn | Dồn tải xuống DB | Batch refresh, single-flight theo SKU, TTL jitter trong freshness bound, giới hạn fallback |
| SKU hot / lock contention | Checkout chậm, timeout | Admission theo SKU, pool/queue có giới hạn, khóa theo thứ tự, transaction ngắn |
| Client/worker retry hàng loạt | Tăng tải, đơn hoặc charge trùng | Exponential backoff có jitter, retry budget, stable idempotency key |
| Deadlock/serialization failure | Transaction rollback | Retry cả transaction có giới hạn trong deadline; giữ nguyên request identity |
| Commit/HTTP response không rõ | Client tưởng thất bại dù đã giữ hàng | Lookup bằng cùng idempotency key; không tạo order mới |
| Cart LWW / mất replica | Cart khác nhau hoặc mất một sửa đổi | Nêu rõ best effort, đọc lại trước xác nhận; không dùng cart làm nguồn tồn/giá |
| Split-brain PostgreSQL | Hai nơi cùng bán cùng lượng hàng | Consensus election + fencing; không ghi nếu không có primary hợp lệ |
| Sync standby lỗi | Checkout không commit được | Fail closed; khôi phục standby, không hạ durability để cứu latency |
| Payment timeout / webhook trùng | Thu tiền trùng hoặc giải phóng hàng sai | Outbox/inbox, dedup, conditional transitions, đối soát trước retry/release |
| DB phục hồi sau mất kết nối | Commit chờ sync có kết quả chưa rõ | Tra cứu trạng thái; không giả định mọi timeout đều đã rollback |