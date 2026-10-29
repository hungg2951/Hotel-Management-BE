# Hệ thống Quản lý Khách sạn (Hotel Management System) - Base Source Code

Bộ khung nền tảng (Base Architecture) chuẩn doanh nghiệp cho đồ án hệ thống quản lý khách sạn, được xây dựng trên công nghệ hiện đại nhất: **Java 25 (LTS)**, **Spring Boot 4.1.x**, và **Oracle AI Database 26ai (Free)**.

---

## 1. Giới thiệu hệ thống và phạm vi

- **Mục tiêu**: Cung cấp nền tảng kiến trúc vững chắc, cấu hình chuẩn mực, kết nối cơ sở dữ liệu và bảo mật hoàn chỉnh để nhóm sinh viên tập trung phát triển nghiệp vụ khách sạn mà không phải mất thời gian dựng khung và cấu hình ban đầu.
- **Phạm vi đồ án hiện tại**: Quản lý một khách sạn độc lập.
- **Khả năng mở rộng**: Toàn bộ các bảng nghiệp vụ cốt lõi đều được thiết kế sẵn cột `hotel_id` để sẵn sàng nâng cấp thành chuỗi khách sạn (Multi-property / Hotel Chain) trong tương lai mà không cần cấu trúc lại cơ sở dữ liệu.
- **Tình trạng nghiệp vụ**: Chưa triển khai logic nghiệp vụ chuyên sâu (đặt phòng phức hợp, check-in/out, gạch nợ folio, tính phí minibar, tích điểm thành viên, báo cáo doanh thu). Nhóm sinh viên sẽ xây dựng các chức năng này dựa trên các package và entity đã được chuẩn bị sẵn.

---

## 2. Công nghệ và phiên bản

| Thành phần | Công nghệ / Thư viện | Phiên bản | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Hệ điều hành** | Windows 10/11 | 64-bit | Môi trường phát triển chính |
| **Ngôn ngữ** | Java (JDK) | **25.0.4.1 (LTS)** | Tận dụng Virtual Threads, Pattern Matching |
| **Framework** | Spring Boot | **4.1.2** | Phiên bản mới nhất (Spring Framework 7) |
| **ORM / JPA** | Hibernate | **7.x** | Quản lý entity, `ddl-auto: validate` |
| **Bảo mật** | Spring Security & JJWT | 7.x / JJWT 0.12.6 | Stateless JWT Authentication |
| **Cơ sở dữ liệu** | Oracle AI Database 26ai Free | `latest` (Docker) | Hỗ trợ kiểu `BOOLEAN`, Identity Columns |
| **JDBC Driver** | Oracle JDBC Thin Driver (`ojdbc11`) | **23.26.3.0.0** | Tối ưu cho Oracle 23ai / 26ai |
| **Schema Migration** | Flyway + `flyway-database-oracle` | 11.x | Quản lý phiên bản migration cơ sở dữ liệu |
| **Định dạng JSON** | Jackson | **3.1.7** (`tools.jackson`) | Chuẩn JSON của Spring Boot 4 |
| **Build Tool** | Apache Maven Wrapper (`mvnw`) | 3.10.0 | Đi kèm project, không cần cài riêng Maven |

---

## 3. Yêu cầu cài đặt môi trường

1. **Java Development Kit (JDK 25 LTS)**:
   - Cài đặt bản JDK 25 từ Oracle hoặc OpenJDK.
   - Thiết lập biến môi trường `JAVA_HOME` trỏ đến thư mục cài đặt (Ví dụ: `C:\Program Files\Java\jdk-25.0.4.1`).
   - Kiểm tra bằng lệnh:
     ```powershell
     java -version
     ```
     Kỳ vọng hiển thị: `java version "25.0.x" ... Java(TM) SE Runtime Environment ...`
2. **Docker Desktop**:
   - Cài đặt [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).
   - Bật WSL2 Backend và đảm bảo Docker Desktop đang ở trạng thái **Running**.
   - Kiểm tra bằng lệnh:
     ```powershell
     docker ps
     ```
3. **IDE đề xuất**: IntelliJ IDEA (Ultimate hoặc Community) phiên bản 2024.3+ để hỗ trợ cú pháp Java 25 đầy đủ.
4. **Git**: Đã cài đặt Git for Windows.

---

## 4. Hướng dẫn chạy hệ thống từng bước

### Bước 1: Khởi tạo file cấu hình môi trường `.env`
Sao chép file mẫu `.env.example` thành file `.env` tại thư mục gốc của project:
```powershell
Copy-Item .env.example .env
```
Mở file `.env` và kiểm tra/chỉnh sửa các thông số theo mong muốn (mật khẩu SYS, mật khẩu user `hotel_app`, secret JWT, v.v.).

### Bước 2: Khởi động container Oracle AI Database 26ai
Tại thư mục gốc của project, chạy lệnh Docker Compose:
```powershell
docker compose up -d
```
> **Lưu ý**: Lần đầu tiên chạy, Docker sẽ tải image `container-registry.oracle.com/database/free:latest` (khoảng 3 - 4 GB) và khởi tạo cơ sở dữ liệu. Bạn có thể theo dõi tiến trình khởi tạo bằng lệnh:
> ```powershell
> docker logs -f hotel-oracle26ai
> ```
> Khi log xuất hiện dòng chữ `DATABASE IS READY TO USE!`, cơ sở dữ liệu đã sẵn sàng kết nối.

### Bước 3: Tạo Tablespace và User bằng script `00_create_tablespace_user.sql`
Vì lý do bảo mật và phân quyền chuẩn DBA, việc tạo tablespace và user ứng dụng **không đưa vào Flyway** mà thực hiện bằng script quản trị của SYS trong Pluggable Database `FREEPDB1`:

1. Kết nối vào container bằng công cụ `sqlplus` hoặc dùng SQL Developer / DBeaver với quyền `SYSDBA`:
   - **Cách 1: Chạy trực tiếp qua Docker exec (Khuyên dùng)**:
     ```powershell
     # Chép file script vào container
     docker cp 00_create_tablespace_user.sql hotel-oracle26ai:/tmp/00_create_tablespace_user.sql

     # Chạy script bằng SQL*Plus với user SYS
     docker exec -it hotel-oracle26ai sqlplus sys/YourSysPasswordHere123#@FREEPDB1 as sysdba @/tmp/00_create_tablespace_user.sql
     ```
   - **Cách 2: Chạy qua SQL Developer / DBeaver**:
     - Host: `localhost` | Port: `1521` | Service Name: `FREEPDB1`
     - Role: `SYSDBA` | User: `sys` | Password: (Mật khẩu đặt tại `ORACLE_PWD` trong `.env`)
     - Mở file [00_create_tablespace_user.sql](file:///00_create_tablespace_user.sql) và nhấn **Run Script** (F5).

2. **Kiểm tra kết quả thực hiện của script**:
   - Tablespace `hotel_data` (100MB, autoextend) và `hotel_temp` đã được tạo.
   - User `hotel_app` có `DEFAULT TABLESPACE hotel_data`, có quota trên `hotel_data` và **không** có quyền `UNLIMITED TABLESPACE` hay role `RESOURCE`.
   - Kiểm tra bằng truy vấn:
     ```sql
     SELECT username, default_tablespace, temporary_tablespace FROM dba_users WHERE username = 'HOTEL_APP';
     SELECT tablespace_name, bytes, max_bytes FROM dba_ts_quotas WHERE username = 'HOTEL_APP';
     ```

### Bước 4: Khởi động ứng dụng Spring Boot
Chạy ứng dụng bằng Maven Wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
Khi ứng dụng khởi động:
1. **Flyway** sẽ tự động kết nối bằng tài khoản `hotel_app` vào `FREEPDB1`.
2. Thực thi file migration `V1__init_schema.sql` tạo đủ **26 bảng** và chèn dữ liệu mẫu. Vì user `hotel_app` có `DEFAULT TABLESPACE hotel_data`, toàn bộ 26 bảng, index và bảng `flyway_schema_history` sẽ tự động nằm hoàn toàn trong tablespace `hotel_data`.
3. Hibernate thực hiện `ddl-auto: validate` để đối soát toàn bộ 26 Entity Java với schema database thực tế.
4. `AdminInitializer` kiểm tra và tự động khởi tạo tài khoản quản trị `admin` với mật khẩu mã hóa BCrypt đọc từ biến môi trường.

### Bước 5: Kiểm tra trạng thái cơ sở dữ liệu sau khởi động
Đăng nhập vào database bằng user `hotel_app`:
```powershell
docker exec -it hotel-oracle26ai sqlplus hotel_app/YourHotelAppPasswordHere123#@FREEPDB1
```
Thực thi các câu lệnh kiểm tra:
```sql
-- 1. Kiểm tra số lượng bảng (kỳ vọng: 27 bảng gồm 26 bảng nghiệp vụ + 1 bảng flyway_schema_history)
SELECT COUNT(*) FROM user_tables;

-- 2. Kiểm tra toàn bộ bảng nằm đúng trong tablespace HOTEL_DATA
SELECT table_name, tablespace_name FROM user_tables WHERE tablespace_name != 'HOTEL_DATA';
-- (Kỳ vọng: Không có dòng nào trả về)

-- 3. Kiểm tra dữ liệu mẫu của hạng phòng (kỳ vọng: 3)
SELECT COUNT(*) FROM room_type;

-- 4. Xem 3 hạng phòng mẫu
SELECT room_type_id, name, base_price, is_active FROM room_type;
```

---

## 5. Danh sách biến môi trường

Các biến môi trường được định nghĩa trong file `.env` và được đọc tự động vào `application.yml`:

| Tên biến môi trường | Giá trị mặc định | Giải thích ý nghĩa |
| :--- | :--- | :--- |
| `ORACLE_PORT` | `1521` | Cổng ánh xạ ra máy host của Oracle Database |
| `ORACLE_PWD` | `YourSysPasswordHere123#` | Mật khẩu tài khoản quản trị cao cấp `SYS` / `SYSTEM` |
| `DB_HOST` | `localhost` | Địa chỉ máy chủ database mà ứng dụng kết nối tới |
| `DB_PORT` | `1521` | Cổng kết nối database của ứng dụng |
| `DB_SERVICE_NAME` | `FREEPDB1` | Tên Pluggable Database (PDB) của Oracle Free 26ai |
| `DB_USERNAME` | `hotel_app` | Tên tài khoản database dành riêng cho ứng dụng |
| `DB_PASSWORD` | `YourHotelAppPasswordHere123#`| Mật khẩu tài khoản `hotel_app` |
| `JWT_SECRET` | *(chuỗi bí mật 256-bit)* | Khóa bí mật dùng để ký và giải mã JWT token |
| `JWT_EXPIRATION_MS` | `86400000` | Thời gian sống của JWT token tính bằng ms (86400000 = 24 giờ) |
| `INITIAL_ADMIN_USERNAME`| `admin` | Tên đăng nhập của tài khoản Admin khởi tạo lần đầu |
| `INITIAL_ADMIN_PASSWORD`| `Admin@Hotel2026!` | Mật khẩu khởi tạo của Admin (được hash bằng BCrypt) |
| `INITIAL_ADMIN_EMAIL` | `admin@demohotel.vn` | Email của tài khoản Admin |

---

## 6. Cấu trúc thư mục và quy tắc kiến trúc

### Cấu trúc package
Source code được tổ chức theo module chức năng (package by feature), lớp chính `HotelManagementApplication.java` nằm trực tiếp trong package gốc `com.hotel`:

```text
com.hotel
├── HotelManagementApplication.java        # Main class khởi động Spring Boot
├── common                                 # Thành phần dùng chung toàn hệ thống
│   ├── enums                              # Các Enum khớp CHECK constraint DB
│   ├── exception                          # BusinessException, GlobalExceptionHandler
│   ├── response                           # ApiResponse record chuẩn hoá JSON
│   └── security                           # SecurityConfig, JwtTokenProvider, Filter, CurrentUserApi
├── auth                                   # Quản lý tài khoản, nhân viên, phân quyền
│   ├── controller                         # AuthController (login)
│   ├── dto                                # LoginRequest, LoginResponse
│   ├── entity                             # Hotel, AppRole, Employee, User, UserRole
│   ├── repository                         # UserRepository, HotelRepository, ...
│   └── service                            # AuthService, AdminInitializer
├── room                                   # Quản lý phòng, hạng phòng, tiện ích, giá, khuyến mãi
│   ├── controller                         # RoomTypeController
│   ├── dto                                # RoomTypeResponse
│   ├── entity                             # RoomType, Room, Amenity, RoomRate, RatePlan, Promotion...
│   ├── repository                         # RoomTypeRepository, RoomRepository, ...
│   └── service                            # RoomTypeService
├── guest                                  # Quản lý hồ sơ khách hàng, CCCD/Passport, điểm thưởng
│   ├── controller / dto / entity / repository / service (Guest)
├── booking                                # Đặt phòng, giữ chỗ, chống trùng phòng
│   ├── controller / dto / entity / repository / service (Reservation, ReservationRoom, RoomNight)
├── frontdesk                              # Lưu trú thực tế, check-in, check-out
│   ├── controller / dto / entity / repository / service (Stay, StayGuest)
├── billing                                # Thu ngân, hoá đơn, dịch vụ, thanh toán
│   ├── controller / dto / entity / repository / service (HotelService, Folio, FolioItem, Payment, Invoice)
├── housekeeping                           # Vận hành, buồng phòng, bảo trì thiết bị
│   ├── controller / dto / entity / repository / service (HousekeepingTask, MaintenanceRequest)
└── report                                 # Nhật ký kiểm toán, báo cáo thống kê
    └── controller / dto / entity / repository / service (AuditLog)
```

### Quy tắc kiến trúc bắt buộc
1. **Phân tách ranh giới Package (Loose Coupling)**:
   - Các entity trong **cùng một package** có thể liên kết trực tiếp bằng quan hệ JPA như `@ManyToOne` (Ví dụ: `Room` -> `RoomType` trong package `room`, `User` -> `Hotel` trong package `auth`).
   - Các entity **khác package** **TUYỆT ĐỐI KHÔNG** dùng `@ManyToOne` hay `@OneToMany`, mà chỉ tham chiếu qua ID kiểu `Long` (Ví dụ: `Reservation` trong `booking` chỉ lưu `Long guestId`, `Long hotelId`; `Stay` trong `frontdesk` chỉ lưu `Long roomId`). Điều này giúp mã nguồn phân tách module rõ ràng, tránh liên kết chéo vòng lặp và sẵn sàng tách Microservices khi cần.
2. **Controller & API Response**:
   - Controller chỉ tiếp nhận request và trả về đối tượng `ResponseEntity<ApiResponse<T>>`.
   - **Không bao giờ** trả trực tiếp Entity ra ngoài API. Toàn bộ dữ liệu trao đổi qua API đều phải dùng **DTO dạng record**.
3. **Quản lý dữ liệu người dùng hiện tại**:
   - Sử dụng lớp tiện ích `CurrentUserApi` để lấy `userId`, `username`, `hotelId` từ `SecurityContext` trong các nghiệp vụ cần ghi nhận vết người tạo/người nhận (`created_by`, `received_by`, `posted_by`).

---

## 7. Các quy ước hệ thống

1. **Chuẩn URI API**:
   - Toàn bộ API nghiệp vụ bắt đầu bằng tiền tố `/api/v1/...`.
2. **Định dạng dữ liệu trả về (`ApiResponse<T>`)**:
   ```json
   {
     "success": true,
     "code": "SUCCESS",
     "message": "Thành công",
     "data": { ... },
     "timestamp": "2026-10-26T03:15:00.000Z"
   }
   ```
3. **Mã lỗi chuẩn**:
   - `BAD_REQUEST`: Tham số đầu vào không hợp lệ.
   - `VALIDATION_ERROR`: Lỗi dữ liệu validate form/DTO (kèm chi tiết từng trường).
   - `NOT_FOUND`: Không tìm thấy tài nguyên theo yêu cầu.
   - `UNAUTHORIZED`: Chưa đăng nhập hoặc token không hợp lệ.
   - `FORBIDDEN`: Không có quyền truy cập chức năng yêu cầu.
   - `CONFLICT` / `DUPLICATE`: Trùng lặp dữ liệu (Đặc biệt lỗi ORA-00001 chống đặt trùng phòng `(room_id, stay_date)` được bắt tự động và trả mã `DUPLICATE` với HTTP 409).
   - `DATA_INTEGRITY_VIOLATION`: Vi phạm ràng buộc khóa ngoại (ORA-02291, ORA-02292).
   - `INTERNAL_SERVER_ERROR`: Lỗi hệ thống nội bộ (không bao giờ để lộ chi tiết kỹ thuật/stacktrace ra bên ngoài).
4. **Quy ước đặt tên file Flyway Migration**:
   - Định dạng: `V<yyyyMMddHHmm>__mo_ta_ngan_gon.sql`
   - Ví dụ: `V202610261000__add_customer_feedback_table.sql`.
5. **Tiền tệ VND**:
   - Mọi số tiền VND trong database là `NUMBER(15,0)`, trong Entity Java và DTO ánh xạ thành kiểu `Long` (không dùng float hay double để tránh sai số dấu phẩy động).

---

## 8. Hướng dẫn kiểm tra và gọi API mẫu

### 1. Đăng nhập lấy JWT Token (Admin)
- **Endpoint**: `POST /api/v1/auth/login` (Công khai)
- **Lệnh curl**:
  ```powershell
  curl -X POST http://localhost:8080/api/v1/auth/login `
    -H "Content-Type: application/json" `
    -d '{\"username\":\"admin\",\"password\":\"Admin@Hotel2026!\"}'
  ```
- **Phản hồi mẫu**:
  ```json
  {
    "success": true,
    "code": "SUCCESS",
    "message": "Đăng nhập thành công",
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
      "tokenType": "Bearer",
      "expiresInMs": 86400000,
      "username": "admin",
      "userId": 1,
      "roles": ["ROLE_ADMIN"]
    },
    "timestamp": "2026-10-26T03:20:00.000Z"
  }
  ```

### 2. Gọi endpoint mẫu lấy danh sách hạng phòng (Cần Token)
- **Endpoint**: `GET /api/v1/room-types`
- **Lệnh curl (Có Bearer Token)**:
  ```powershell
  $TOKEN = "eyJhbGciOiJIUzI1NiIsIn..." # Thay bằng token vừa nhận ở bước 1

  curl -X GET http://localhost:8080/api/v1/room-types `
    -H "Authorization: Bearer $TOKEN"
  ```
- **Phản hồi mẫu**:
  ```json
  {
    "success": true,
    "code": "SUCCESS",
    "message": "Lấy danh sách hạng phòng thành công",
    "data": [
      {
        "roomTypeId": 1,
        "hotelId": 1,
        "name": "Standard",
        "description": "Phong tieu chuan",
        "maxAdults": 2,
        "maxChildren": 1,
        "bedType": "Double",
        "areaSqm": 22.0,
        "basePrice": 600000,
        "isActive": true
      },
      {
        "roomTypeId": 2,
        "hotelId": 1,
        "name": "Deluxe",
        "description": "Phong cao cap, huong pho",
        "maxAdults": 2,
        "maxChildren": 2,
        "bedType": "King",
        "areaSqm": 30.0,
        "basePrice": 900000,
        "isActive": true
      },
      {
        "roomTypeId": 3,
        "hotelId": 1,
        "name": "Suite",
        "description": "Phong suite co phong khach rieng",
        "maxAdults": 3,
        "maxChildren": 2,
        "bedType": "King",
        "areaSqm": 48.0,
        "basePrice": 1600000,
        "isActive": true
      }
    ],
    "timestamp": "2026-10-26T03:21:00.000Z"
  }
  ```

### 3. Thử nghiệm bảo mật (Không có Token)
Gọi thử endpoint được bảo vệ khi không truyền token:
```powershell
curl -i -X GET http://localhost:8080/api/v1/room-types
```
Kỳ vọng nhận mã HTTP **401 Unauthorized**:
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "Yêu cầu xác thực tài khoản để truy cập tài nguyên này",
  "timestamp": "2026-10-26T03:22:00.000Z"
}
```

---

## 9. Lỗi thường gặp và cách khắc phục

1. **Lỗi `ORA-12514: TNS:listener does not currently know of service requested in connect descriptor`**:
   - **Nguyên nhân**: Container Oracle mới khởi động, Pluggable Database `FREEPDB1` đang trong quá trình mở (OPEN) hoặc listener chưa kịp đăng ký service `FREEPDB1`.
   - **Cách xử lý**: Đợi 1 - 2 phút cho đến khi `docker logs hotel-oracle26ai` hiển thị `DATABASE IS READY TO USE!`. Sau đó kiểm tra lại bằng lệnh `docker exec hotel-oracle26ai lsnrctl status`.
2. **Lỗi `ORA-01017: invalid username/password; logon denied`**:
   - **Nguyên nhân**: Mật khẩu của `hotel_app` trong `.env` không trùng khớp với mật khẩu đã tạo trong script `00_create_tablespace_user.sql`, hoặc chưa chạy script tạo user `hotel_app`.
   - **Cách xử lý**: Chạy lại script [00_create_tablespace_user.sql](file:///00_create_tablespace_user.sql) với mật khẩu chính xác như trong file `.env`.
3. **Lỗi `Unsupported Database: Oracle`**:
   - **Nguyên nhân**: Flyway Core bản 10+ tách riêng module hỗ trợ Oracle sang dependency mở rộng.
   - **Cách xử lý**: Đảm bảo dependency `org.flywaydb:flyway-database-oracle` đã có trong [pom.xml](file:///pom.xml) (đã được cấu hình sẵn trong project này).
4. **Lỗi `SchemaManagementException: Schema-validation: missing table...` hoặc lệch kiểu dữ liệu**:
   - **Nguyên nhân**: Entity có tên bảng hoặc tên cột, kiểu dữ liệu không khớp với schema Flyway đã tạo.
   - **Cách xử lý**: Ứng dụng dùng chế độ `ddl-auto: validate`. Toàn bộ 26 entity đã được ánh xạ chính xác với schema `V1__init_schema.sql`. Nếu bạn tạo thêm bảng mới, hãy viết migration mới và tạo entity tương ứng trước khi khởi động ứng dụng.
5. **Lỗi `ByteBuddy / Mockito attach on JDK 25`**:
   - **Nguyên nhân**: JDK 25 siết chặt cơ chế dynamic agent loading.
   - **Cách xử lý**: Đã được cấu hình tham số `<argLine>-XX:+EnableDynamicAgentLoading</argLine>` trong `pom.xml` và `mock-maker-subclass` trong `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`.

---

## 10. Các giả định và công việc tiếp theo

1. **Về Swagger / OpenAPI**:
   - Thư viện `springdoc-openapi` (bản 2.8.x) hiện chưa tương thích chính thức với kiến trúc module phân tách mới của Spring Boot 4.1.x (`spring-boot-starter-webmvc` và Jackson 3 `tools.jackson`). Do đó, Swagger UI tạm thời không được kích hoạt để đảm bảo hệ thống build ổn định 100%. Khi thư viện phát hành bản hỗ trợ Spring Boot 4.x, chỉ cần thêm `springdoc-openapi-starter-webmvc-ui` vào `pom.xml`.
2. **Về kiểm thử unit test & integration test**:
   - Smoke test `HotelManagementApplicationTests` sử dụng `@MockitoBean` cho các repository để cho phép kiểm tra ngữ cảnh Spring Boot hoàn toàn độc lập mà không bắt buộc Docker container phải chạy sẵn trong lúc build CI/CD (`mvnw clean verify`).
3. **Công việc tiếp theo dành cho nhóm sinh viên (Giai đoạn nghiệp vụ)**:
   - **Package `booking`**: Viết service đặt phòng nhiều phòng, tính toán giá phòng theo ngày (`room_rate`), áp dụng mã giảm giá (`promotion`), và gán phòng bằng cách ghi nhận bảng `room_night` để chống đặt trùng phòng.
   - **Package `frontdesk`**: Viết chức năng làm thủ tục check-in (tạo bản ghi `stay`, khai báo `stay_guest`, tạo `folio` thanh toán tương ứng), và thủ tục check-out.
   - **Package `billing`**: Viết chức năng thêm phí dịch vụ (`hotel_service`) vào `folio_item`, nhận thanh toán (`payment`), và xuất hóa đơn VAT (`invoice`).
   - **Package `housekeeping`**: Viết chức năng phân công công việc dọn buồng phòng và tiếp nhận yêu cầu sửa chữa thiết bị phòng.
   - **Package `report`**: Viết các truy vấn thống kê công suất sử dụng phòng (occupancy rate) và doanh thu theo khoảng thời gian.
