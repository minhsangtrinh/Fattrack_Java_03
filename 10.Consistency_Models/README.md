# Assignment 10 — Consistency Models

## 1. Đề bài

Thiết kế ứng dụng mạng xã hội đơn giản gồm:

1. Đăng ký/đăng nhập bằng username và password.
2. Cập nhật profile: tên, ảnh đại diện.
3. Đăng bài viết mới.
4. Bình luận vào bài viết.
5. Đếm số lượt thích của bài viết.

Với mỗi tính năng, chọn mô hình nhất quán, giải thích lý do và đánh đổi, đề xuất công nghệ lưu trữ phù hợp.

## 2. Các mô hình dùng trong bài

- **Strong consistency:** trong bài này, đường truy cập cần nhìn thấy thay đổi đã hoàn tất trước khi thao tác mới bắt đầu, theo nghĩa linearizability cho dữ liệu được xét.
- **Read-Your-Writes (RYW):** sau khi ghi thành công, người dùng đọc được thay đổi của chính mình hoặc phiên bản mới hơn trong phiên làm việc. Người dùng khác vẫn có thể thấy dữ liệu cũ.
- **Eventual consistency:** khi không còn thay đổi mới và việc truyền/cập nhật dữ liệu tiếp tục hoạt động, các bản sao cuối cùng hội tụ. Không mặc định có một thời hạn hội tụ cố định.
- **Ràng buộc nhân quả cụ thể:** một comment chỉ được hiển thị cùng bài viết cha hợp lệ. Bài này bảo đảm quan hệ post-before-comment, không tuyên bố toàn hệ thống đạt causal consistency cho mọi loại thao tác.

Consistency model mô tả điều người dùng quan sát được. Transaction isolation, replication và cache là các cơ chế triển khai; tên một database hoặc việc có transaction không tự bảo đảm mọi đường đọc đều strong.

## 3. Bảng lựa chọn

| Tính năng | Mô hình lựa chọn | Công nghệ đề xuất | Lý do và đánh đổi |
|---|---|---|---|
| Đăng ký/đăng nhập | **Strong consistency** cho danh tính và credential | PostgreSQL primary; unique constraint trên username chuẩn hóa | Tránh username trùng; đăng nhập sau đăng ký/đổi mật khẩu phải dùng dữ liệu mới. Đổi lại phải đến primary và có thể dừng phục vụ khi không bảo đảm tính đúng đắn |
| Cập nhật profile | **RYW cho chủ tài khoản**, eventual cho người khác | PostgreSQL lưu nguồn; read replica/Redis phục vụ bản công khai | Người sửa phải thấy tên/ảnh vừa đổi; người xem khác có thể thấy bản cũ. Cần version và đọc primary khi replica chưa theo kịp |
| Đăng bài viết | **RYW cho tác giả**, eventual cho feed người khác | PostgreSQL posts + transactional outbox; worker cập nhật feed/cache | Tác giả thấy bài sau khi đăng; fan-out không chặn request tạo bài. Feed follower có thể nhận bài trễ |
| Bình luận | **RYW cho người bình luận**, ràng buộc **post-before-comment**; eventual cho người đọc khác | PostgreSQL comments có foreign key tới posts; cache/read replica cho danh sách | Người gửi thấy comment của mình; không tạo comment cho post chưa tồn tại. Cần xử lý dependency khi projection đến sai thứ tự |
| Like count | **Eventual consistency** cho tổng số; trạng thái like cá nhân đọc từ nguồn khi cần | PostgreSQL likes có unique(post_id, user_id); Redis cache bộ đếm | Tổng số có thể cập nhật trễ để giảm tải đọc. Phải chống like trùng và không dùng phép tăng cache làm nguồn dữ liệu duy nhất |

Thiết kế dùng một PostgreSQL cluster làm nguồn dữ liệu chính để giữ ứng dụng đơn giản. Redis và read replica phục vụ các đường đọc chấp nhận dữ liệu cũ. Chưa cần thêm một NoSQL database riêng chỉ để lưu likes; khi tải thực tế tăng, có thể đánh giá lại cách phân vùng và lưu projection.

## 4. Thiết kế từng tính năng

### 4.1. Đăng ký/đăng nhập — Strong consistency

**Tình huống cần đúng:** hai người cùng đăng ký một username; người dùng đăng nhập ngay sau đăng ký hoặc sau khi đổi mật khẩu.

**Cách thực hiện:**

- Chuẩn hóa username theo một quy tắc thống nhất và lưu username_normalized có UNIQUE constraint.
- Thực hiện INSERT trực tiếp; nếu vi phạm unique thì trả lỗi username đã tồn tại. Không dựa vào một lần SELECT kiểm tra trước vì hai request có thể cùng thấy username còn trống.
- Lưu password hash bằng thuật toán phù hợp như Argon2id, không lưu password rõ.
- Đăng nhập đọc credential từ primary, không đọc cache hoặc async replica có thể chưa nhận mật khẩu mới.
- Chỉ trả thành công sau khi transaction ghi đã hoàn tất theo chính sách durability đã chọn.

**Đánh đổi:** tăng số request tới primary và phụ thuộc đường kết nối tới nguồn có thẩm quyền. Khi không thể xác định primary hợp lệ, không cho phép fallback sang credential cũ để tiếp tục đăng nhập.

**Điều kiện topology:** primary duy nhất phải được bảo vệ bằng leader election/fencing. Nếu yêu cầu giữ các thao tác đã xác nhận qua failover, cần synchronous replication, chỉ promote standby đủ điều kiện và tạm dừng khi không thể bảo đảm điều đó. PostgreSQL mặc định với async replica không tự cung cấp bảo đảm này.

### 4.2. Profile — Read-Your-Writes

**Tình huống:** người dùng đổi tên hoặc avatar và mở lại trang cá nhân ngay. Việc nhìn thấy tên/ảnh cũ làm người dùng tưởng lưu thất bại.

**Cách thực hiện:**

1. Ghi profile ở primary, tăng profile_version và trả bản profile đã commit.
2. Client lưu version mới nhất trong phiên. Sau lần ghi, các lần đọc profile của chính mình được route tới primary cho tới khi có cách xác minh replica đã theo kịp.
3. Nếu tối ưu bằng replica/cache, chỉ trả khi version không nhỏ hơn version phiên yêu cầu; nếu chưa đạt thì đọc primary hoặc chờ có giới hạn.
4. Profile công khai cho người khác có thể đọc từ replica/cache và cập nhật eventual.
5. Avatar dùng URL có version/content hash để trình duyệt hoặc CDN không tiếp tục lấy ảnh cũ dưới cùng một URL.

Version do server phát hành và kiểm tra; quyền sửa luôn lấy từ danh tính đã xác thực. Sticky session tới cùng API instance chưa đủ nếu instance đó vẫn đọc một replica bị lag.

**Đánh đổi:** cần quản lý version và read routing; một số request của chủ tài khoản vẫn phải đến primary. RYW không bảo đảm mọi người khác thấy thay đổi ngay.

Nếu cùng một profile bị sửa từ nhiều thiết bị, dùng optimistic concurrency theo version để báo conflict thay vì âm thầm ghi đè một thay đổi mới hơn. RYW riêng nó không ngăn lost update.

### 4.3. Post — RYW cho tác giả, eventual cho feed

**Tình huống:** sau khi tạo bài, tác giả phải thấy bài của mình; không cần chờ mọi follower đã nhận bài mới trả kết quả.

**Cách thực hiện:**

1. Cùng một transaction ở primary: ghi post và outbox event chứa post_id/version.
2. Trả bản post đã commit cho tác giả. Các lần đọc lại bài/trang cá nhân trong phiên đi qua primary hoặc nguồn đã được xác minh theo kịp.
3. Worker đọc outbox đã commit rồi cập nhật feed/read model.
4. Feed của follower đọc từ cache/projection nên có thể chưa thấy bài ngay.
5. Worker xử lý lại event phải idempotent, chẳng hạn UPSERT theo feed_owner + post_id, không thêm một bản feed mới cho mỗi lần retry.

**Đánh đổi:** thao tác tạo bài không phải chờ fan-out tới tất cả follower; đổi lại cần worker, retry và theo dõi backlog. Outbox tránh khoảng hở ghi post thành công nhưng mất event vì tiến trình chết trước khi publish.

Không chỉ hiển thị post bằng optimistic UI rồi gọi đó là RYW: sau khi nhận thành công, một lần GET lại trong phiên cũng phải đọc được post.

### 4.4. Comment — RYW và post-before-comment

**Tình huống:** người dùng bình luận vào một bài vừa tạo, nhưng replica/projection chưa có bài đó.

**Cách thực hiện:**

- Tạo comment tại primary, kiểm tra post tồn tại và quyền bình luận trong transaction; dùng foreign key bảo vệ quan hệ comment → post.
- Ghi comment và outbox event trong cùng transaction, chỉ trả thành công sau commit.
- Người gửi đọc lại comment từ primary hoặc nguồn đã theo kịp, tương tự RYW của post.
- Với projection bất đồng bộ, event comment có thể đến trước event post. Worker phải chờ/retry dependency hoặc lấy post từ nguồn có thẩm quyền; không công bố một comment mồ côi.
- Khi dựng màn hình, chỉ hiển thị comment nếu có post cha hợp lệ. Không chỉ dựa vào thứ tự đến của event ở hai queue khác nhau.
- Xử lý event trùng theo comment_id để một lần gửi không hiển thị nhiều comment.

**Đánh đổi:** thêm ràng buộc DB và kiểm tra dependency khiến đường ghi/worker phức tạp hơn. Người dùng khác vẫn có thể thấy comment trễ. Đây là bảo đảm quan hệ cha–con và RYW, chưa phải một cơ chế causal consistency tổng quát.

### 4.5. Like count — Eventual consistency

**Tình huống:** bài có nhiều lượt like đồng thời. Chênh lệch nhỏ tạm thời trong tổng số thường ít ảnh hưởng hơn độ trễ mỗi lần mở feed.

**Nguồn dữ liệu chuẩn:**

- Bảng likes lưu quan hệ người dùng đã like bài; UNIQUE(post_id, user_id) ngăn một user like cùng post nhiều lần.
- Like là thao tác đặt trạng thái “đã like”, unlike là đặt trạng thái “chưa like”. Retry cùng thao tác không tạo thêm một lượt.
- Chỉ phát outbox event khi trạng thái nguồn thực sự thay đổi.

**Tổng số hiển thị:**

1. Worker gộp thay đổi theo post_id rồi tính lại COUNT từ quan hệ likes tại primary.
2. Cache kết quả vào Redis để feed đọc nhanh. Mỗi post chỉ có một luồng cập nhật tổng được tuần tự hóa; snapshot cũ không được ghi đè kết quả mới.
3. Sự kiện trùng chỉ kích hoạt tính lại, không làm tăng tổng lần nữa. Có thể debounce các sự kiện dồn dập để không COUNT cho từng request.
4. Định kỳ đối soát lại từ nguồn để sửa cache thiếu hoặc cũ. Redis mất dữ liệu thì tái tạo; cache miss không được mặc định diễn giải là 0.
5. Nút like của người vừa thao tác dùng trạng thái trả về từ lần ghi; nếu cần đọc lại chính xác thì đọc nguồn. Tổng số công khai vẫn có thể chưa cập nhật.

**Đánh đổi:** giảm tải đọc và tránh tranh chấp một counter ở mỗi lượt like; tổng số không luôn chính xác tức thời. Tính lại COUNT cho post cực lớn vẫn có chi phí, nên cần đo tải trước khi chuyển sang bộ tổng hợp gia tăng có inbox deduplication được commit nguyên tử với counter.

Không chỉ dùng Redis INCR rồi retry tùy ý, vì request lặp có thể làm đếm dư. Cassandra counter cũng không tự giải quyết idempotency của thao tác người dùng.

## 5. Architecture diagram

~~~mermaid
flowchart TD
    CLIENT["Client: session version + committed write response"] --> API["Application API"]
    API --> AUTH["Auth: authoritative reads and writes"]
    API --> CONTENT["Profile / Post / Comment / Like writes"]
    API --> SESSION["Session reads: RYW"]
    API --> PUBLIC["Public reads: eventual"]

    AUTH --> PRIMARY[("PostgreSQL primary: users, profiles, posts, comments, likes, outbox")]
    CONTENT --> PRIMARY
    SESSION -->|Primary until a read source is verified caught up| PRIMARY
    PRIMARY -->|Synchronous replication for safe failover| STANDBY[("Eligible standby")]
    PRIMARY -->|Async replication| REPLICA[("Read replica")]
    PUBLIC --> REPLICA
    PUBLIC --> CACHE[("Redis: feed, public profile, like count")]

    PRIMARY -->|Committed outbox events| WORKER["Idempotent projection workers"]
    WORKER -->|Refresh current source data| PRIMARY
    WORKER --> CACHE
    WORKER --> DEPENDENCY["Check post-before-comment dependency"]
~~~

Sơ đồ độc lập nằm trong **architecture.mmd**. Các đường đọc auth và RYW không lấy dữ liệu từ public cache chưa được xác minh.

## 6. Hành vi khi có lỗi

| Tình huống | Hành vi mong muốn |
|---|---|
| Hai request cùng username | Một INSERT thành công, request còn lại nhận lỗi unique |
| Profile vừa sửa nhưng replica lag | Phiên người sửa đọc primary; người khác có thể tạm thấy bản cũ |
| Worker dừng sau khi post đã commit | Tác giả vẫn đọc được post; follower nhận sau khi worker xử lý lại outbox |
| Comment event đến trước post event | Chờ hoặc lấy dependency từ nguồn; không công bố comment mồ côi |
| Like request hoặc event bị gửi trùng | Quan hệ like không trùng; tổng được tính lại từ nguồn |
| Redis mất cache | Tái tạo có giới hạn tải; không suy diễn dữ liệu nguồn đã mất |
| Primary không có đường truy cập an toàn | Auth/ghi và RYW cần primary phải chờ hoặc trả lỗi; public cache có thể tiếp tục trả dữ liệu cũ phù hợp chính sách |
| Client timeout sau khi có thể đã commit | Với post/comment, dùng idempotency key gắn user + operation + payload để tra cứu/retry cùng thao tác, không tạo bản mới tùy ý |

Eventual consistency cần retry, xử lý event lỗi và reconciliation; chỉ dùng message queue không tự bảo đảm dữ liệu sẽ hội tụ. Bài không đặt ra một thời gian lag bắt buộc; thời gian hội tụ là chỉ số cần theo dõi khi vận hành.