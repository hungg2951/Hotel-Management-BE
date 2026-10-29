/* =====================================================================
   HOTEL MANAGEMENT SYSTEM - Oracle AI Database 26ai
   Script: 00_create_tablespace_user.sql
   MUC DICH:
   - Tao tablespace du lieu 'hotel_data' va tablespace tam 'hotel_temp'
   - Tao user 'hotel_app' voi DEFAULT TABLESPACE hotel_data
   - Cap quota chi tren 'hotel_data' (KHONG cap he thong UNLIMITED TABLESPACE)
   - Cap cac quyen co ban (KHONG dung role RESOURCE)

   LUU Y QUAN TRONG:
   - File nay CHAY TAY bang tai khoan SYS (hoac SYSTEM co quyen DBA)
   - Phai ket noi vao dung PDB FREEPDB1 (KHONG chay o CDB$ROOT)
   - KHONG dua file nay vao thu muc Flyway migration
   ===================================================================== */

-- 1. Chuyen session sang Pluggable Database FREEPDB1
ALTER SESSION SET CONTAINER = FREEPDB1;

-- 2. Truy van kiem tra thu muc chua datafile hien tai trong FREEPDB1
-- Chay cau lenh nay de lay thu muc chua file .dbf hien tai cua he thong
SELECT file_name FROM dba_data_files WHERE ROWNUM = 1;

-- Vi du: Neu file_name tra ve la 'C:\APP\ORACLE\ORADATA\FREE\FREEPDB1\SYSTEM01.DBF'
-- thi thu muc chua file la: 'C:\APP\ORACLE\ORADATA\FREE\FREEPDB1\'

-- ---------------------------------------------------------------------
-- 3. Tao tablespace du lieu 'hotel_data'
-- (Thay the 'C:\app\oracle\oradata\FREE\FREEPDB1\' bang thu muc thuc te cua may ban)
-- ---------------------------------------------------------------------
CREATE TABLESPACE hotel_data
    DATAFILE 'hotel_data01.dbf' SIZE 100M AUTOEXTEND ON NEXT 50M MAXSIZE 10G
    EXTENT MANAGEMENT LOCAL AUTOALLOCATE
    SEGMENT SPACE MANAGEMENT AUTO;

-- LUU Y NEU BAO LOI ORA-02236 (Do chua bat Oracle Managed Files):
-- Ban chi can them duong dan day du tu ket qua o Muc 2, vi du:
-- CREATE TABLESPACE hotel_data
--     DATAFILE 'C:\app\oracle\oradata\FREE\FREEPDB1\hotel_data01.dbf' SIZE 100M AUTOEXTEND ON NEXT 50M MAXSIZE 10G
--     EXTENT MANAGEMENT LOCAL AUTOALLOCATE
--     SEGMENT SPACE MANAGEMENT AUTO;

-- HOAC bat OMF mot lan duy nhat cho he thong:
-- ALTER SYSTEM SET db_create_file_dest = 'C:\app\oracle\oradata\FREE\FREEPDB1';

-- ---------------------------------------------------------------------
-- 4. Tao tablespace tam 'hotel_temp'
-- ---------------------------------------------------------------------
CREATE TEMPORARY TABLESPACE hotel_temp
    TEMPFILE 'hotel_temp01.dbf' SIZE 50M AUTOEXTEND ON NEXT 25M MAXSIZE 2G;

-- Tuong tu, neu can duong dan day du:
-- CREATE TEMPORARY TABLESPACE hotel_temp
--     TEMPFILE 'C:\app\oracle\oradata\FREE\FREEPDB1\hotel_temp01.dbf' SIZE 50M AUTOEXTEND ON NEXT 25M MAXSIZE 2G;

-- ---------------------------------------------------------------------
-- 5. Tao user hotel_app
-- Luu y: Thay the mat khau 'HotelApp2026#' bang mat khau ban dat trong file .env (DB_PASSWORD)
-- ---------------------------------------------------------------------
CREATE USER hotel_app IDENTIFIED BY "HotelApp2026#"
    DEFAULT TABLESPACE hotel_data
    TEMPORARY TABLESPACE hotel_temp
    ACCOUNT UNLOCK;

-- 6. Cap quota tren tablespace hotel_data (QUOTA chi han che tren hotel_data)
ALTER USER hotel_app QUOTA UNLIMITED ON hotel_data;

-- 7. Cap quyen toi thieu can thiet (KHONG cap role RESOURCE, KHONG cap UNLIMITED TABLESPACE)
GRANT CREATE SESSION TO hotel_app;
GRANT CREATE TABLE TO hotel_app;
GRANT CREATE VIEW TO hotel_app;
GRANT CREATE SEQUENCE TO hotel_app;
GRANT CREATE PROCEDURE TO hotel_app;
GRANT CREATE TRIGGER TO hotel_app;
GRANT CREATE SYNONYM TO hotel_app;

-- 8. Truy van kiem tra sau khi tao
-- 8.1. Kiem tra user va default tablespace
SELECT username, default_tablespace, temporary_tablespace, account_status
FROM dba_users
WHERE username = 'HOTEL_APP';

-- 8.2. Kiem tra quota (chi duoc phep tren HOTEL_DATA)
SELECT tablespace_name, bytes, max_bytes
FROM dba_ts_quotas
WHERE username = 'HOTEL_APP';

-- 8.3. Kiem tra quyen he thong
SELECT privilege
FROM dba_sys_privs
WHERE grantee = 'HOTEL_APP';

-- 8.4. Kiem tra role duoc cap
SELECT granted_role
FROM dba_role_privs
WHERE grantee = 'HOTEL_APP';
