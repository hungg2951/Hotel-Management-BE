# HƯỚNG DẪN CHI TIẾT SETUP ORACLE DATABASE BẰNG SQL*PLUS (ĐỀ SỐ 7)

Tài liệu này hướng dẫn từng bước cấu hình **Tablespace**, **User**, **Phân quyền** và **Khởi tạo dữ liệu** cho hệ thống Quản lý Khách sạn bằng công cụ **SQL*Plus** đã được cài đặt trực tiếp trên hệ điều hành Windows.

---

## 1. Kiến trúc Oracle Pluggable Database (CDB & PDB)

Từ phiên bản Oracle 12c đến 23ai/26ai, Oracle áp dụng kiến trúc đa người thuê (Multitenant):
- **CDB$ROOT (Container Database)**: Cơ sở dữ liệu gốc chứa metadata chung của toàn bộ hệ thống. **Không được tạo user ứng dụng hoặc tablespace nghiệp vụ tại đây** (nếu cố tạo sẽ bị lỗi `ORA-65096: invalid common user or role name`).
- **FREEPDB1 (Pluggable Database - PDB)**: Cơ sở dữ liệu con độc lập dành cho ứng dụng. Toàn bộ bảng, tablespace, user `hotel_app` của đề thi **phải nằm trong PDB này**.

---

## 2. Các bước thực hiện tuần tự trên SQL*Plus

### BƯỚC 1: Mở Terminal và Đăng nhập bằng quyền `SYSDBA`

1. Nhấn phím `Windows + R`, gõ `cmd` hoặc `powershell` rồi nhấn **Enter**.
2. Đăng nhập vào SQL*Plus với quyền quản trị tối cao (`SYSDBA`):

```powershell
# Cách 1: Xác thực qua quyền Windows Administrator (nhanh nhất)
sqlplus / as sysdba

# Cách 2: Nếu được yêu cầu chỉ định mật khẩu SYS
sqlplus sys as sysdba
# (Sau đó nhập mật khẩu SYS khi được hỏi)

# Cách 3: Kết nối thẳng vào Pluggable Database FREEPDB1
sqlplus "sys/YourSysPasswordHere123#@localhost:1521/FREEPDB1 as sysdba"
```

> **Giải thích**: 
> - Quyền `SYSDBA` là quyền quản trị cao nhất, cho phép tạo mới tablespace, quản lý tập tin vật lý (`.dbf`) và cấp quyền cho user khác.
> - Dấu ngoặc kép `""` ở Cách 3 giúp ngăn Windows hiểu nhầm ký tự đặc biệt như `#`, `@` trong mật khẩu.

---

### BƯỚC 2: Kiểm tra và chuyển Session vào PDB `FREEPDB1`

Sau khi vào dấu nhắc lệnh `SQL>`, thực thi:

```sql
-- Kiểm tra container hiện tại
SHOW CON_NAME;
```

Nếu kết quả trả về `CDB$ROOT`, bạn **phải chuyển sang PDB `FREEPDB1`**:

```sql
-- 1. Xem danh sách và trạng thái các PDB có trong máy
SHOW PDBS;

-- 2. Đảm bảo FREEPDB1 đang mở (nếu trạng thái là MOUNTED, cần mở bằng lệnh sau)
ALTER PLUGGABLE DATABASE FREEPDB1 OPEN;

-- 3. Chuyển phiên làm việc vào FREEPDB1
ALTER SESSION SET CONTAINER = FREEPDB1;

-- 4. Xác nhận lại container hiện tại (kỳ vọng trả về FREEPDB1)
SHOW CON_NAME;
```

> **Giải thích**:
> - Lệnh `ALTER SESSION SET CONTAINER = FREEPDB1;` đảm bảo mọi câu lệnh tạo tablespace và user ở các bước sau được ghi đúng vào PDB của bài thi, không làm bẩn CDB gốc.

---

### BƯỚC 3: Tạo Tablespace dữ liệu `hotel_data`

#### 3.1. Tìm đường dẫn thư mục chứa datafile của PDB
Trên hệ thống Oracle cài đặt trực tiếp trên Windows, tính năng Oracle Managed Files (OMF) thường chưa được bật sẵn. Do đó, Oracle yêu cầu bắt buộc phải chỉ định tên file hoặc đường dẫn file: `DATAFILE 'đường_dẫn/tên_file.dbf'`.

Để lấy đúng thư mục đang chứa các file `.dbf` của PDB `FREEPDB1`, bạn chạy truy vấn sau:

```sql
SELECT file_name FROM dba_data_files WHERE ROWNUM = 1;
```

*Ví dụ kết quả hiển thị:*
```text
FILE_NAME
--------------------------------------------------------------------------------
C:\APP\ORACLE\ORADATA\FREE\FREEPDB1\SYSTEM01.DBF
```
-> Thư mục lưu trữ chính là: `C:\APP\ORACLE\ORADATA\FREE\FREEPDB1\`

#### 3.2. Chạy câu lệnh tạo Tablespace với đường dẫn file

Thay thế phần đường dẫn bên dưới bằng thư mục bạn vừa tìm được ở bước 3.1:

```sql
CREATE TABLESPACE hotel_data
    DATAFILE 'đường dẫn datafile\hotel_data01.dbf' SIZE 100M AUTOEXTEND ON NEXT 50M MAXSIZE 10G
    EXTENT MANAGEMENT LOCAL AUTOALLOCATE
    SEGMENT SPACE MANAGEMENT AUTO;
```
Nếu bạn không biết đường dẫn datafile thì chạy lệnh sau:
```SELECT file_name FROM dba_data_files;```

*(Nếu thư mục làm việc hiện tại của SQL*Plus đã là thư mục dữ liệu, bạn có thể gõ ngắn gọn `DATAFILE 'hotel_data01.dbf' SIZE 100M ...`)*.

> **Giải thích từng mệnh đề**:
> - `CREATE TABLESPACE hotel_data`: Đặt tên vùng không gian lưu trữ bảng và index là `hotel_data`.
> - `DATAFILE '.../hotel_data01.dbf' SIZE 100M`: Tên và vị trí tập tin dữ liệu vật lý với kích thước khởi tạo 100 Megabytes.
> - `AUTOEXTEND ON NEXT 50M`: Khi dung lượng 100M bị đầy, Oracle sẽ tự động mở rộng thêm mỗi lần 50 Megabytes mà không làm gián đoạn ứng dụng.
> - `MAXSIZE 10G`: Giới hạn tối đa kích thước tablespace là 10 Gigabytes, tránh việc cơ sở dữ liệu chiếm dụng hết dung lượng ổ cứng máy chủ.
> - `EXTENT MANAGEMENT LOCAL AUTOALLOCATE`: Oracle tự động quản lý kích thước các khối cấp phát bộ nhớ (extent) ngay tại header của datafile, tối ưu tốc độ đọc ghi.
> - `SEGMENT SPACE MANAGEMENT AUTO (ASSM)`: Tự động quản lý danh sách khối trống bằng bitmap, tối ưu hiệu năng chạy đồng thời đa luồng.

---

### BƯỚC 4: Tạo Tablespace tạm `hotel_temp`

Tương tự như Bước 3, tablespace tạm cần chỉ định `TEMPFILE 'đường_dẫn/tên_file.dbf'`:

```sql
CREATE TEMPORARY TABLESPACE hotel_temp
    TEMPFILE 'C:\APP\ADMIN\PRODUCT\26AI\ORADATA\FREE\FREEPDB1\hotel_temp01.dbf' SIZE 50M AUTOEXTEND ON NEXT 25M MAXSIZE 2G;
```

> **Giải thích**:
> - Tablespace tạm (`TEMPORARY TABLESPACE`) dùng để thực hiện các thuật toán sắp xếp (`ORDER BY`), gộp nhóm (`GROUP BY`), phép nối băm (`HASH JOIN`), và các phép toán bảng tạm.
> - Dùng `TEMPFILE` thay vì `DATAFILE` giúp Oracle bỏ qua việc ghi Redo Log cho các dữ liệu tạm thời, tăng tốc độ xử lý câu truy vấn.

---

> **Mẹo thêm: Nếu muốn bật tự động sinh tên file (OMF)**
> Nếu không muốn phải gõ đường dẫn dài mỗi lần tạo file, bạn có thể thiết lập tham số OMF một lần duy nhất:
> ```sql
> ALTER SYSTEM SET db_create_file_dest = 'C:\APP\ORACLE\ORADATA\FREE\FREEPDB1';
> ```
> Sau khi chạy lệnh trên, các câu lệnh tạo tablespace chỉ cần gõ `DATAFILE SIZE 100M ...` mà không cần ghi tên file.

---

### BƯỚC 5: Tạo User ứng dụng `hotel_app`

```sql
CREATE USER hotel_app IDENTIFIED BY "HotelApp2026#"
    DEFAULT TABLESPACE hotel_data
    TEMPORARY TABLESPACE hotel_temp
    ACCOUNT UNLOCK;
```

> **Lưu ý**: Hãy thay thế `HotelApp2026#` bằng mật khẩu bạn định cấu hình cho biến `DB_PASSWORD` trong file `.env`.
>
> **Giải thích**:
> - `IDENTIFIED BY "..."`: Thiết lập mật khẩu cho tài khoản. Dùng cặp dấu nháy kép `""` để giữ nguyên chữ hoa/thường và các ký tự đặc biệt.
> - `DEFAULT TABLESPACE hotel_data`: Quy định mặc định toàn bộ bảng (`CREATE TABLE`) và chỉ mục (`CREATE INDEX`) do user này tạo ra sẽ tự động lưu vào tablespace `hotel_data`, không bị rơi nhầm vào tablespace hệ thống `SYSTEM` hay `SYSAUX`.
> - `TEMPORARY TABLESPACE hotel_temp`: Chỉ định vùng nhớ tạm khi thực hiện các câu truy vấn phức tạp.
> - `ACCOUNT UNLOCK`: Kích hoạt tài khoản ngay lập tức ở trạng thái sẵn sàng đăng nhập.

---

### BƯỚC 6: Cấp Quota trên Tablespace `hotel_data`

```sql
ALTER USER hotel_app QUOTA UNLIMITED ON hotel_data;
```

> **Giải thích (Tiêu chí chấm điểm DBA chuẩn)**:
> - **Nguyên tắc đặc quyền tối thiểu (Least Privilege)**: Không được cấp quyền hệ thống `UNLIMITED TABLESPACE` cho user. Nếu cấp quyền đó, user có thể tạo dữ liệu tràn lan lên cả tablespace `SYSTEM` và `SYSAUX`, dẫn đến sập toàn bộ CSDL.
> - Lệnh trên chỉ cấp hạn ngạch (Quota) riêng trên tablespace `hotel_data`.

---

### BƯỚC 7: Cấp các quyền hệ thống tối thiểu (System Privileges)

```sql
GRANT CREATE SESSION TO hotel_app;
GRANT CREATE TABLE TO hotel_app;
GRANT CREATE VIEW TO hotel_app;
GRANT CREATE SEQUENCE TO hotel_app;
GRANT CREATE PROCEDURE TO hotel_app;
GRANT CREATE TRIGGER TO hotel_app;
GRANT CREATE SYNONYM TO hotel_app;
```

> **Giải thích**:
> - `CREATE SESSION`: Quyền tối thiểu để user có thể kết nối (đăng nhập) vào database.
> - `CREATE TABLE`: Quyền tạo bảng trong schema của mình.
> - `CREATE VIEW`: Quyền tạo view xem dữ liệu báo cáo.
> - `CREATE SEQUENCE`: Quyền tạo sequence sinh số tăng tự động.
> - `CREATE PROCEDURE`: Quyền tạo hàm (Function), thủ tục (Stored Procedure) và Package PL/SQL.
> - `CREATE TRIGGER`: Quyền tạo trigger tự động (ví dụ trigger audit, trigger cập nhật trạng thái).
> - `CREATE SYNONYM`: Quyền tạo từ đồng nghĩa cho đối tượng.
> - **Tại sao không cấp Role `RESOURCE`?**: Trong các phiên bản Oracle hiện đại (23ai/26ai), role `RESOURCE` đã được cảnh báo lỗi thời (deprecated), và ở các bản cũ nó tự động cấp kèm quyền `UNLIMITED TABLESPACE` vi phạm nguyên tắc bảo mật. Việc cấp quyền tường minh từng quyền như trên đạt điểm tối đa trong kỳ thi.

---

### BƯỚC 8: Truy vấn kiểm tra và nghiệm thu (Verification)

Chạy các câu lệnh sau trực tiếp trên SQL*Plus để kiểm tra kết quả:

```sql
-- 1. Kiểm tra tài khoản và default tablespace
SELECT username, default_tablespace, temporary_tablespace, account_status
FROM dba_users
WHERE username = 'HOTEL_APP';
-- Kỳ vọng: username = HOTEL_APP, default = HOTEL_DATA, temp = HOTEL_TEMP, status = OPEN

-- 2. Kiểm tra hạn ngạch Quota
SELECT tablespace_name, bytes / 1024 / 1024 AS used_mb, max_bytes
FROM dba_ts_quotas
WHERE username = 'HOTEL_APP';
-- Kỳ vọng: Chỉ xuất hiện dòng HOTEL_DATA, max_bytes = -1 (UNLIMITED trên đúng tablespace này)

-- 3. Kiểm tra danh sách quyền hệ thống đã cấp
SELECT privilege
FROM dba_sys_privs
WHERE grantee = 'HOTEL_APP'
ORDER BY privilege;
-- Kỳ vọng: Hiển thị đủ 7 quyền: CREATE PROCEDURE, SEQUENCE, SESSION, SYNONYM, TABLE, TRIGGER, VIEW
```

---

### BƯỚC 9: Đăng nhập thử nghiệm bằng User `hotel_app`

Thực hiện lệnh chuyển đổi user ngay trong SQL*Plus:

```sql
CONNECT hotel_app/"HotelApp2026#"@localhost:1521/FREEPDB1;

-- Kiểm tra user hiện tại
SHOW USER;
-- Kết quả hiển thị: USER is "HOTEL_APP"
```

Khi kết nối thành công, bạn đã hoàn tất 100% phần cấu hình hạ tầng database cho đề thi!

---

## 3. Khởi tạo 18 bảng dữ liệu

Sau khi tạo xong user `hotel_app`, bạn có 2 lựa chọn để khởi tạo 18 bảng và nạp dữ liệu mẫu:

### Cách 1: Để Spring Boot (Flyway) tự động chạy (Khuyên dùng)
1. Cấu hình file [.env](file:///.env):
   ```properties
   DB_HOST=localhost
   DB_PORT=1521
   DB_SERVICE_NAME=FREEPDB1
   DB_USERNAME=hotel_app
   DB_PASSWORD=HotelApp2026#
   ```
2. Khởi động backend Spring Boot:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Flyway sẽ tự động đọc file `V1__init_schema.sql` và tạo đủ 18 bảng cùng toàn bộ index và dữ liệu mẫu vào tablespace `hotel_data`.

### Cách 2: Chạy trực tiếp script SQL trên SQL*Plus
Khi đang đăng nhập bằng user `hotel_app`:
```sql
@c:\Users\Admin\Desktop\java_UTC\hotel-management\src\main\resources\db\migration\V1__init_schema.sql
```
Sau đó kiểm tra số bảng đã tạo:
```sql
SELECT COUNT(*) FROM user_tables;
-- Kỳ vọng: 18 bảng (hoặc 19 nếu đã có bảng flyway_schema_history)

-- Kiểm tra xem tất cả các bảng có nằm trong tablespace HOTEL_DATA không:
SELECT table_name, tablespace_name FROM user_tables WHERE tablespace_name != 'HOTEL_DATA';
-- Kỳ vọng: "no rows selected" (Tất cả bảng đều nằm đúng trong HOTEL_DATA)
```

---

## 4. Các lỗi thường gặp và cách xử lý (Troubleshooting)

| Mã lỗi | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| **ORA-65096**: invalid common user or role name | Đang ở `CDB$ROOT` mà tạo user cục bộ | Chạy `ALTER SESSION SET CONTAINER = FREEPDB1;` trước khi tạo user. |
| **ORA-01031**: insufficient privileges | Chưa có quyền `SYSDBA` | Đăng nhập bằng `sqlplus / as sysdba` hoặc `sqlplus sys as sysdba`. |
| **ORA-01920**: user name conflicts with another user name | User `hotel_app` đã tồn tại từ trước | Xóa user cũ làm lại: `DROP USER hotel_app CASCADE;` |
| **ORA-01543**: tablespace already exists | Tablespace `hotel_data` đã có sẵn | Xóa tablespace cũ: `DROP TABLESPACE hotel_data INCLUDING CONTENTS AND DATAFILES;` |
| **ORA-01017**: invalid username/password | Sai mật khẩu hoặc chưa mở khóa | Kiểm tra lại mật khẩu hoặc chạy `ALTER USER hotel_app IDENTIFIED BY "MậtKhẩuMới" ACCOUNT UNLOCK;` bằng quyền SYS. |
