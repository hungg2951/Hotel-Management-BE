/* =====================================================================
   HOTEL MANAGEMENT SYSTEM - Oracle AI Database 26ai
   26 bang | PK / FK / UNIQUE / CHECK | index cho FK | du lieu mau

   CACH CHAY (SQL*Plus / SQLcl / DBeaver / SQL Developer):
   1) Chay file 00_create_tablespace_user.sql bang SYS trong PDB FREEPDB1
      (tao tablespace hotel_data va user hotel_app dung tablespace do).
   2) Dang nhap bang hotel_app, chay file nay tu tren xuong duoi.
      Moi bang/index se tu nam trong tablespace mac dinh cua user (hotel_data).
   3) Muon lam lai tu dau: chay phan DROP o cuoi file (dang bi comment).

   QUY UOC:
   - PK: NUMBER GENERATED ALWAYS AS IDENTITY (JPA dung GenerationType.IDENTITY)
   - Tien VND: NUMBER(15,0)
   - Boolean: kieu BOOLEAN (Oracle 23ai/26ai)
   - Dat ten: pk_ / fk_ / uq_ / ck_ / ix_
   - Tranh tu khoa Oracle: dung users, app_role, hotel_service
   ===================================================================== */

/* ---------------------------------------------------------------------
   1. NHOM HE THONG
   --------------------------------------------------------------------- */
CREATE TABLE hotel (
    hotel_id        NUMBER GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR2(200)  NOT NULL,
    address         VARCHAR2(500),
    phone           VARCHAR2(20),
    email           VARCHAR2(150),
    tax_code        VARCHAR2(20),
    star_rating     NUMBER(1),
    check_in_time   VARCHAR2(5)    DEFAULT '14:00' NOT NULL,
    check_out_time  VARCHAR2(5)    DEFAULT '12:00' NOT NULL,
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_hotel PRIMARY KEY (hotel_id),
    CONSTRAINT ck_hotel_star CHECK (star_rating BETWEEN 1 AND 5)
);

CREATE TABLE app_role (
    role_id     NUMBER GENERATED ALWAYS AS IDENTITY,
    role_code   VARCHAR2(30)  NOT NULL,   -- ADMIN, MANAGER, RECEPTIONIST, HOUSEKEEPER, ACCOUNTANT
    role_name   VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_app_role PRIMARY KEY (role_id),
    CONSTRAINT uq_app_role_code UNIQUE (role_code)
);

CREATE TABLE employee (
    employee_id  NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id     NUMBER         NOT NULL,
    full_name    VARCHAR2(150)  NOT NULL,
    phone        VARCHAR2(20),
    email        VARCHAR2(150),
    position     VARCHAR2(100),
    hire_date    DATE,
    is_active    BOOLEAN        DEFAULT TRUE NOT NULL,
    created_at   TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_employee PRIMARY KEY (employee_id),
    CONSTRAINT fk_employee_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (hotel_id)
);

CREATE TABLE users (
    user_id        NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id       NUMBER         NOT NULL,
    employee_id    NUMBER,
    username       VARCHAR2(50)   NOT NULL,
    password_hash  VARCHAR2(255)  NOT NULL,   -- BCrypt
    is_active      BOOLEAN        DEFAULT TRUE NOT NULL,
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_employee UNIQUE (employee_id),
    CONSTRAINT fk_users_hotel    FOREIGN KEY (hotel_id)    REFERENCES hotel (hotel_id),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employee (employee_id)
);

CREATE TABLE user_role (
    user_id  NUMBER NOT NULL,
    role_id  NUMBER NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES app_role (role_id)
);

/* ---------------------------------------------------------------------
   2. NHOM PHONG VA GIA
   --------------------------------------------------------------------- */
CREATE TABLE room_type (
    room_type_id  NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id      NUMBER         NOT NULL,
    name          VARCHAR2(100)  NOT NULL,
    description   VARCHAR2(1000),
    max_adults    NUMBER(2)      DEFAULT 2 NOT NULL,
    max_children  NUMBER(2)      DEFAULT 0 NOT NULL,
    bed_type      VARCHAR2(50),
    area_sqm      NUMBER(6,1),
    base_price    NUMBER(15,0)   NOT NULL,
    is_active     BOOLEAN        DEFAULT TRUE NOT NULL,
    created_at    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_room_type PRIMARY KEY (room_type_id),
    CONSTRAINT uq_room_type_name UNIQUE (hotel_id, name),
    CONSTRAINT fk_room_type_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (hotel_id),
    CONSTRAINT ck_room_type_price CHECK (base_price >= 0),
    CONSTRAINT ck_room_type_adults CHECK (max_adults >= 1)
);

CREATE TABLE room (
    room_id              NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id             NUMBER        NOT NULL,
    room_type_id         NUMBER        NOT NULL,
    room_number          VARCHAR2(10)  NOT NULL,
    floor_no             NUMBER(3),
    status               VARCHAR2(20)  DEFAULT 'AVAILABLE' NOT NULL,
    housekeeping_status  VARCHAR2(20)  DEFAULT 'CLEAN' NOT NULL,
    is_active            BOOLEAN       DEFAULT TRUE NOT NULL,
    note                 VARCHAR2(500),
    CONSTRAINT pk_room PRIMARY KEY (room_id),
    CONSTRAINT uq_room_number UNIQUE (hotel_id, room_number),
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id)      REFERENCES hotel (hotel_id),
    CONSTRAINT fk_room_type  FOREIGN KEY (room_type_id)  REFERENCES room_type (room_type_id),
    CONSTRAINT ck_room_status CHECK (status IN ('AVAILABLE','OCCUPIED','MAINTENANCE','OUT_OF_ORDER')),
    CONSTRAINT ck_room_hk CHECK (housekeeping_status IN ('CLEAN','DIRTY','CLEANING','INSPECTED'))
);

CREATE TABLE amenity (
    amenity_id  NUMBER GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_amenity PRIMARY KEY (amenity_id),
    CONSTRAINT uq_amenity_name UNIQUE (name)
);

CREATE TABLE room_type_amenity (
    room_type_id  NUMBER NOT NULL,
    amenity_id    NUMBER NOT NULL,
    CONSTRAINT pk_room_type_amenity PRIMARY KEY (room_type_id, amenity_id),
    CONSTRAINT fk_rta_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (room_type_id) ON DELETE CASCADE,
    CONSTRAINT fk_rta_amenity   FOREIGN KEY (amenity_id)   REFERENCES amenity (amenity_id)
);

CREATE TABLE rate_plan (
    rate_plan_id         NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id             NUMBER         NOT NULL,
    code                 VARCHAR2(30)   NOT NULL,
    name                 VARCHAR2(100)  NOT NULL,
    includes_breakfast   BOOLEAN        DEFAULT FALSE NOT NULL,
    cancellation_policy  VARCHAR2(500),
    free_cancel_days     NUMBER(3)      DEFAULT 1 NOT NULL,
    is_active            BOOLEAN        DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_rate_plan PRIMARY KEY (rate_plan_id),
    CONSTRAINT uq_rate_plan_code UNIQUE (hotel_id, code),
    CONSTRAINT fk_rate_plan_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (hotel_id)
);

CREATE TABLE room_rate (
    room_rate_id  NUMBER GENERATED ALWAYS AS IDENTITY,
    room_type_id  NUMBER        NOT NULL,
    rate_plan_id  NUMBER        NOT NULL,
    date_from     DATE          NOT NULL,
    date_to       DATE          NOT NULL,
    day_type      VARCHAR2(10)  DEFAULT 'ALL' NOT NULL,
    price         NUMBER(15,0)  NOT NULL,
    CONSTRAINT pk_room_rate PRIMARY KEY (room_rate_id),
    CONSTRAINT fk_room_rate_type FOREIGN KEY (room_type_id) REFERENCES room_type (room_type_id),
    CONSTRAINT fk_room_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (rate_plan_id),
    CONSTRAINT ck_room_rate_dates CHECK (date_to >= date_from),
    CONSTRAINT ck_room_rate_daytype CHECK (day_type IN ('ALL','WEEKDAY','WEEKEND','HOLIDAY')),
    CONSTRAINT ck_room_rate_price CHECK (price >= 0)
);

CREATE TABLE promotion (
    promotion_id    NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id        NUMBER         NOT NULL,
    code            VARCHAR2(30)   NOT NULL,
    description     VARCHAR2(500),
    discount_type   VARCHAR2(10)   NOT NULL,
    discount_value  NUMBER(15,2)   NOT NULL,
    valid_from      DATE           NOT NULL,
    valid_to        DATE           NOT NULL,
    min_nights      NUMBER(3)      DEFAULT 1 NOT NULL,
    max_uses        NUMBER(8),
    used_count      NUMBER(8)      DEFAULT 0 NOT NULL,
    is_active       BOOLEAN        DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_promotion PRIMARY KEY (promotion_id),
    CONSTRAINT uq_promotion_code UNIQUE (hotel_id, code),
    CONSTRAINT fk_promotion_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (hotel_id),
    CONSTRAINT ck_promotion_type CHECK (discount_type IN ('PERCENT','FIXED')),
    CONSTRAINT ck_promotion_value CHECK (discount_value > 0),
    CONSTRAINT ck_promotion_dates CHECK (valid_to >= valid_from)
);

/* ---------------------------------------------------------------------
   3. NHOM KHACH HANG VA DAT PHONG
   --------------------------------------------------------------------- */
CREATE TABLE guest (
    guest_id       NUMBER GENERATED ALWAYS AS IDENTITY,
    full_name      VARCHAR2(150)  NOT NULL,
    phone          VARCHAR2(20),
    email          VARCHAR2(150),
    gender         VARCHAR2(10),
    date_of_birth  DATE,
    nationality    VARCHAR2(50)   DEFAULT 'Viet Nam',
    id_type        VARCHAR2(10),
    id_number      VARCHAR2(30),
    address        VARCHAR2(500),
    loyalty_tier   VARCHAR2(10)   DEFAULT 'NONE' NOT NULL,
    loyalty_points NUMBER(10)     DEFAULT 0 NOT NULL,
    note           VARCHAR2(500),
    created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_guest PRIMARY KEY (guest_id),
    CONSTRAINT uq_guest_identity UNIQUE (id_type, id_number),
    CONSTRAINT ck_guest_idtype CHECK (id_type IN ('CCCD','CMND','PASSPORT')),
    CONSTRAINT ck_guest_gender CHECK (gender IN ('MALE','FEMALE','OTHER')),
    CONSTRAINT ck_guest_tier CHECK (loyalty_tier IN ('NONE','SILVER','GOLD','PLATINUM'))
);

CREATE TABLE reservation (
    reservation_id    NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id          NUMBER         NOT NULL,
    reservation_code  VARCHAR2(20)   NOT NULL,
    guest_id          NUMBER         NOT NULL,
    promotion_id      NUMBER,
    booking_source    VARCHAR2(15)   DEFAULT 'WALK_IN' NOT NULL,
    status            VARCHAR2(15)   DEFAULT 'CONFIRMED' NOT NULL,
    check_in_date     DATE           NOT NULL,
    check_out_date    DATE           NOT NULL,
    adults            NUMBER(2)      DEFAULT 1 NOT NULL,
    children          NUMBER(2)      DEFAULT 0 NOT NULL,
    total_amount      NUMBER(15,0)   DEFAULT 0 NOT NULL,
    deposit_amount    NUMBER(15,0)   DEFAULT 0 NOT NULL,
    note              VARCHAR2(1000),
    created_by        NUMBER,
    created_at        TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at        TIMESTAMP,
    CONSTRAINT pk_reservation PRIMARY KEY (reservation_id),
    CONSTRAINT uq_reservation_code UNIQUE (reservation_code),
    CONSTRAINT fk_resv_hotel     FOREIGN KEY (hotel_id)     REFERENCES hotel (hotel_id),
    CONSTRAINT fk_resv_guest     FOREIGN KEY (guest_id)     REFERENCES guest (guest_id),
    CONSTRAINT fk_resv_promotion FOREIGN KEY (promotion_id) REFERENCES promotion (promotion_id),
    CONSTRAINT fk_resv_created_by FOREIGN KEY (created_by)  REFERENCES users (user_id),
    CONSTRAINT ck_resv_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT ck_resv_source CHECK (booking_source IN ('WALK_IN','PHONE','WEBSITE','OTA','OTHER')),
    CONSTRAINT ck_resv_status CHECK (status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW')),
    CONSTRAINT ck_resv_amounts CHECK (total_amount >= 0 AND deposit_amount >= 0)
);

/* Mot don co the nhieu phong. room_id de NULL cho den khi gan so phong cu the. */
CREATE TABLE reservation_room (
    reservation_room_id  NUMBER GENERATED ALWAYS AS IDENTITY,
    reservation_id       NUMBER        NOT NULL,
    room_type_id         NUMBER        NOT NULL,
    room_id              NUMBER,
    rate_plan_id         NUMBER        NOT NULL,
    price_per_night      NUMBER(15,0)  NOT NULL,
    status               VARCHAR2(15)  DEFAULT 'BOOKED' NOT NULL,
    CONSTRAINT pk_reservation_room PRIMARY KEY (reservation_room_id),
    CONSTRAINT fk_rr_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_room_type   FOREIGN KEY (room_type_id)   REFERENCES room_type (room_type_id),
    CONSTRAINT fk_rr_room        FOREIGN KEY (room_id)        REFERENCES room (room_id),
    CONSTRAINT fk_rr_rate_plan   FOREIGN KEY (rate_plan_id)   REFERENCES rate_plan (rate_plan_id),
    CONSTRAINT ck_rr_status CHECK (status IN ('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
    CONSTRAINT ck_rr_price CHECK (price_per_night >= 0)
);

/* CHONG DAT TRUNG PHONG: moi (phong, dem) chi thuoc toi da 1 booking.
   Insert 1 dong cho moi dem o khi gan phong; trung se bao ORA-00001.
   Huy booking / doi phong => xoa cac dong tuong ung. */
CREATE TABLE room_night (
    room_id              NUMBER NOT NULL,
    stay_date            DATE   NOT NULL,
    reservation_room_id  NUMBER NOT NULL,
    CONSTRAINT pk_room_night PRIMARY KEY (room_id, stay_date),
    CONSTRAINT fk_rn_room FOREIGN KEY (room_id) REFERENCES room (room_id),
    CONSTRAINT fk_rn_reservation_room FOREIGN KEY (reservation_room_id)
        REFERENCES reservation_room (reservation_room_id) ON DELETE CASCADE
);

/* ---------------------------------------------------------------------
   4. NHOM LUU TRU VA THANH TOAN
   --------------------------------------------------------------------- */
CREATE TABLE stay (
    stay_id              NUMBER GENERATED ALWAYS AS IDENTITY,
    reservation_room_id  NUMBER       NOT NULL,
    room_id              NUMBER       NOT NULL,
    actual_check_in      TIMESTAMP    NOT NULL,
    actual_check_out     TIMESTAMP,
    status               VARCHAR2(15) DEFAULT 'IN_HOUSE' NOT NULL,
    checked_in_by        NUMBER,
    checked_out_by       NUMBER,
    CONSTRAINT pk_stay PRIMARY KEY (stay_id),
    CONSTRAINT uq_stay_reservation_room UNIQUE (reservation_room_id),
    CONSTRAINT fk_stay_reservation_room FOREIGN KEY (reservation_room_id) REFERENCES reservation_room (reservation_room_id),
    CONSTRAINT fk_stay_room        FOREIGN KEY (room_id)        REFERENCES room (room_id),
    CONSTRAINT fk_stay_checkin_by  FOREIGN KEY (checked_in_by)  REFERENCES users (user_id),
    CONSTRAINT fk_stay_checkout_by FOREIGN KEY (checked_out_by) REFERENCES users (user_id),
    CONSTRAINT ck_stay_status CHECK (status IN ('IN_HOUSE','CHECKED_OUT')),
    CONSTRAINT ck_stay_times CHECK (actual_check_out IS NULL OR actual_check_out >= actual_check_in)
);

/* Khach luu tru cung phong - phuc vu khai bao luu tru */
CREATE TABLE stay_guest (
    stay_id     NUMBER  NOT NULL,
    guest_id    NUMBER  NOT NULL,
    is_primary  BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT pk_stay_guest PRIMARY KEY (stay_id, guest_id),
    CONSTRAINT fk_sg_stay  FOREIGN KEY (stay_id)  REFERENCES stay (stay_id) ON DELETE CASCADE,
    CONSTRAINT fk_sg_guest FOREIGN KEY (guest_id) REFERENCES guest (guest_id)
);

CREATE TABLE hotel_service (
    service_id  NUMBER GENERATED ALWAYS AS IDENTITY,
    hotel_id    NUMBER        NOT NULL,
    name        VARCHAR2(150) NOT NULL,
    category    VARCHAR2(15)  DEFAULT 'OTHER' NOT NULL,
    unit        VARCHAR2(20)  DEFAULT 'lan' NOT NULL,
    unit_price  NUMBER(15,0)  NOT NULL,
    is_active   BOOLEAN       DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_hotel_service PRIMARY KEY (service_id),
    CONSTRAINT uq_hotel_service_name UNIQUE (hotel_id, name),
    CONSTRAINT fk_hotel_service_hotel FOREIGN KEY (hotel_id) REFERENCES hotel (hotel_id),
    CONSTRAINT ck_hotel_service_cat CHECK (category IN ('FOOD','LAUNDRY','MINIBAR','SPA','TRANSPORT','OTHER')),
    CONSTRAINT ck_hotel_service_price CHECK (unit_price >= 0)
);

CREATE TABLE folio (
    folio_id      NUMBER GENERATED ALWAYS AS IDENTITY,
    stay_id       NUMBER       NOT NULL,
    status        VARCHAR2(10) DEFAULT 'OPEN' NOT NULL,
    total_amount  NUMBER(15,0) DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    closed_at     TIMESTAMP,
    CONSTRAINT pk_folio PRIMARY KEY (folio_id),
    CONSTRAINT uq_folio_stay UNIQUE (stay_id),
    CONSTRAINT fk_folio_stay FOREIGN KEY (stay_id) REFERENCES stay (stay_id),
    CONSTRAINT ck_folio_status CHECK (status IN ('OPEN','CLOSED'))
);

CREATE TABLE folio_item (
    item_id      NUMBER GENERATED ALWAYS AS IDENTITY,
    folio_id     NUMBER        NOT NULL,
    item_type    VARCHAR2(10)  NOT NULL,
    service_id   NUMBER,
    description  VARCHAR2(300),
    quantity     NUMBER(8)     DEFAULT 1 NOT NULL,
    unit_price   NUMBER(15,0)  NOT NULL,
    amount       NUMBER(15,0)  NOT NULL,   -- am khi item_type = DISCOUNT
    posted_at    TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    posted_by    NUMBER,
    CONSTRAINT pk_folio_item PRIMARY KEY (item_id),
    CONSTRAINT fk_fi_folio   FOREIGN KEY (folio_id)   REFERENCES folio (folio_id),
    CONSTRAINT fk_fi_service FOREIGN KEY (service_id) REFERENCES hotel_service (service_id),
    CONSTRAINT fk_fi_posted_by FOREIGN KEY (posted_by) REFERENCES users (user_id),
    CONSTRAINT ck_fi_type CHECK (item_type IN ('ROOM','SERVICE','PENALTY','DISCOUNT','TAX')),
    CONSTRAINT ck_fi_qty CHECK (quantity > 0)
);

/* Thanh toan gan vao folio (luc o) hoac reservation (tien coc truoc khi den) */
CREATE TABLE payment (
    payment_id      NUMBER GENERATED ALWAYS AS IDENTITY,
    folio_id        NUMBER,
    reservation_id  NUMBER,
    method          VARCHAR2(10)  NOT NULL,
    amount          NUMBER(15,0)  NOT NULL,
    status          VARCHAR2(10)  DEFAULT 'SUCCESS' NOT NULL,
    reference_code  VARCHAR2(100),
    paid_at         TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    received_by     NUMBER,
    CONSTRAINT pk_payment PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_folio       FOREIGN KEY (folio_id)       REFERENCES folio (folio_id),
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id),
    CONSTRAINT fk_payment_received_by FOREIGN KEY (received_by)    REFERENCES users (user_id),
    CONSTRAINT ck_payment_target CHECK (folio_id IS NOT NULL OR reservation_id IS NOT NULL),
    CONSTRAINT ck_payment_method CHECK (method IN ('CASH','CARD','TRANSFER','EWALLET')),
    CONSTRAINT ck_payment_status CHECK (status IN ('SUCCESS','REFUNDED','FAILED')),
    CONSTRAINT ck_payment_amount CHECK (amount > 0)
);

CREATE TABLE invoice (
    invoice_id       NUMBER GENERATED ALWAYS AS IDENTITY,
    folio_id         NUMBER         NOT NULL,
    invoice_no       VARCHAR2(30)   NOT NULL,
    buyer_name       VARCHAR2(200),
    buyer_tax_code   VARCHAR2(20),
    buyer_address    VARCHAR2(500),
    subtotal         NUMBER(15,0)   NOT NULL,
    vat_rate         NUMBER(4,2)    DEFAULT 8 NOT NULL,
    vat_amount       NUMBER(15,0)   NOT NULL,
    total_amount     NUMBER(15,0)   NOT NULL,
    issued_at        TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    issued_by        NUMBER,
    CONSTRAINT pk_invoice PRIMARY KEY (invoice_id),
    CONSTRAINT uq_invoice_no UNIQUE (invoice_no),
    CONSTRAINT uq_invoice_folio UNIQUE (folio_id),
    CONSTRAINT fk_invoice_folio FOREIGN KEY (folio_id)  REFERENCES folio (folio_id),
    CONSTRAINT fk_invoice_issued_by FOREIGN KEY (issued_by) REFERENCES users (user_id),
    CONSTRAINT ck_invoice_vat CHECK (vat_rate BETWEEN 0 AND 100)
);

/* ---------------------------------------------------------------------
   5. NHOM VAN HANH
   --------------------------------------------------------------------- */
CREATE TABLE housekeeping_task (
    task_id      NUMBER GENERATED ALWAYS AS IDENTITY,
    room_id      NUMBER        NOT NULL,
    assigned_to  NUMBER,
    task_type    VARCHAR2(15)  DEFAULT 'CLEANING' NOT NULL,
    status       VARCHAR2(15)  DEFAULT 'PENDING' NOT NULL,
    note         VARCHAR2(500),
    created_at   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    started_at   TIMESTAMP,
    finished_at  TIMESTAMP,
    CONSTRAINT pk_housekeeping_task PRIMARY KEY (task_id),
    CONSTRAINT fk_hk_room     FOREIGN KEY (room_id)     REFERENCES room (room_id),
    CONSTRAINT fk_hk_employee FOREIGN KEY (assigned_to) REFERENCES employee (employee_id),
    CONSTRAINT ck_hk_type CHECK (task_type IN ('CLEANING','TURNDOWN','INSPECTION','DEEP_CLEAN')),
    CONSTRAINT ck_hk_status CHECK (status IN ('PENDING','IN_PROGRESS','DONE'))
);

CREATE TABLE maintenance_request (
    request_id   NUMBER GENERATED ALWAYS AS IDENTITY,
    room_id      NUMBER         NOT NULL,
    reported_by  NUMBER,
    assigned_to  NUMBER,
    description  VARCHAR2(1000) NOT NULL,
    priority     VARCHAR2(10)   DEFAULT 'MEDIUM' NOT NULL,
    status       VARCHAR2(15)   DEFAULT 'OPEN' NOT NULL,
    created_at   TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    resolved_at  TIMESTAMP,
    CONSTRAINT pk_maintenance_request PRIMARY KEY (request_id),
    CONSTRAINT fk_mr_room        FOREIGN KEY (room_id)     REFERENCES room (room_id),
    CONSTRAINT fk_mr_reported_by FOREIGN KEY (reported_by) REFERENCES users (user_id),
    CONSTRAINT fk_mr_assigned_to FOREIGN KEY (assigned_to) REFERENCES employee (employee_id),
    CONSTRAINT ck_mr_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT ck_mr_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED'))
);

CREATE TABLE audit_log (
    log_id       NUMBER GENERATED ALWAYS AS IDENTITY,
    user_id      NUMBER,
    action_type  VARCHAR2(30)  NOT NULL,   -- CREATE, UPDATE, DELETE, CANCEL, LOGIN ...
    table_name   VARCHAR2(50)  NOT NULL,
    record_id    VARCHAR2(50),
    old_value    CLOB,
    new_value    CLOB,
    created_at   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (log_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

/* ---------------------------------------------------------------------
   6. INDEX CHO KHOA NGOAI VA TRUY VAN THUONG GAP
   (Oracle KHONG tu tao index cho FK)
   --------------------------------------------------------------------- */
CREATE INDEX ix_employee_hotel      ON employee (hotel_id);
CREATE INDEX ix_users_hotel         ON users (hotel_id);
CREATE INDEX ix_user_role_role      ON user_role (role_id);
CREATE INDEX ix_room_type_hotel     ON room_type (hotel_id);
CREATE INDEX ix_room_room_type      ON room (room_type_id);
CREATE INDEX ix_room_status         ON room (hotel_id, status);
CREATE INDEX ix_rta_amenity         ON room_type_amenity (amenity_id);
CREATE INDEX ix_room_rate_lookup    ON room_rate (room_type_id, rate_plan_id, date_from, date_to);
CREATE INDEX ix_room_rate_plan      ON room_rate (rate_plan_id);
CREATE INDEX ix_guest_phone         ON guest (phone);
CREATE INDEX ix_guest_name          ON guest (full_name);
CREATE INDEX ix_resv_dates          ON reservation (hotel_id, check_in_date, check_out_date);
CREATE INDEX ix_resv_guest          ON reservation (guest_id);
CREATE INDEX ix_resv_status         ON reservation (status);
CREATE INDEX ix_resv_promotion      ON reservation (promotion_id);
CREATE INDEX ix_resv_created_by     ON reservation (created_by);
CREATE INDEX ix_rr_reservation      ON reservation_room (reservation_id);
CREATE INDEX ix_rr_room_type        ON reservation_room (room_type_id);
CREATE INDEX ix_rr_room             ON reservation_room (room_id);
CREATE INDEX ix_rr_rate_plan        ON reservation_room (rate_plan_id);
CREATE INDEX ix_rn_reservation_room ON room_night (reservation_room_id);
CREATE INDEX ix_stay_room           ON stay (room_id);
CREATE INDEX ix_stay_status         ON stay (status);
CREATE INDEX ix_sg_guest            ON stay_guest (guest_id);
CREATE INDEX ix_fi_folio            ON folio_item (folio_id);
CREATE INDEX ix_fi_service          ON folio_item (service_id);
CREATE INDEX ix_payment_folio       ON payment (folio_id);
CREATE INDEX ix_payment_reservation ON payment (reservation_id);
CREATE INDEX ix_payment_paid_at     ON payment (paid_at);
CREATE INDEX ix_hk_room             ON housekeeping_task (room_id);
CREATE INDEX ix_hk_assigned         ON housekeeping_task (assigned_to, status);
CREATE INDEX ix_mr_room             ON maintenance_request (room_id);
CREATE INDEX ix_audit_user          ON audit_log (user_id);
CREATE INDEX ix_audit_table_record  ON audit_log (table_name, record_id);

/* ---------------------------------------------------------------------
   7. DU LIEU MAU
   (Tai khoan admin: tao tu ung dung Java bang BCrypt, khong insert tay o day)
   --------------------------------------------------------------------- */
INSERT INTO hotel (name, address, phone, email, tax_code, star_rating)
VALUES ('Khach san Demo', '1 Pho Demo, Ha Noi', '0241234567', 'info@demohotel.vn', '0101234567', 3);

INSERT INTO app_role (role_code, role_name) VALUES ('ADMIN',        'Quan tri he thong');
INSERT INTO app_role (role_code, role_name) VALUES ('MANAGER',      'Quan ly khach san');
INSERT INTO app_role (role_code, role_name) VALUES ('RECEPTIONIST', 'Le tan');
INSERT INTO app_role (role_code, role_name) VALUES ('HOUSEKEEPER',  'Buong phong');
INSERT INTO app_role (role_code, role_name) VALUES ('ACCOUNTANT',   'Ke toan');

INSERT INTO room_type (hotel_id, name, description, max_adults, max_children, bed_type, area_sqm, base_price)
SELECT hotel_id, 'Standard', 'Phong tieu chuan', 2, 1, 'Double', 22, 600000 FROM hotel;
INSERT INTO room_type (hotel_id, name, description, max_adults, max_children, bed_type, area_sqm, base_price)
SELECT hotel_id, 'Deluxe', 'Phong cao cap, huong pho', 2, 2, 'King', 30, 900000 FROM hotel;
INSERT INTO room_type (hotel_id, name, description, max_adults, max_children, bed_type, area_sqm, base_price)
SELECT hotel_id, 'Suite', 'Phong suite co phong khach rieng', 3, 2, 'King', 48, 1600000 FROM hotel;

INSERT INTO room (hotel_id, room_type_id, room_number, floor_no)
SELECT rt.hotel_id, rt.room_type_id, v.room_number, v.floor_no
FROM room_type rt
JOIN (
    SELECT 'Standard' tname, '101' room_number, 1 floor_no FROM dual UNION ALL
    SELECT 'Standard', '102', 1 FROM dual UNION ALL
    SELECT 'Standard', '103', 1 FROM dual UNION ALL
    SELECT 'Deluxe',   '201', 2 FROM dual UNION ALL
    SELECT 'Deluxe',   '202', 2 FROM dual UNION ALL
    SELECT 'Deluxe',   '203', 2 FROM dual UNION ALL
    SELECT 'Suite',    '301', 3 FROM dual UNION ALL
    SELECT 'Suite',    '302', 3 FROM dual
) v ON v.tname = rt.name;

INSERT INTO amenity (name) VALUES ('WiFi');
INSERT INTO amenity (name) VALUES ('Dieu hoa');
INSERT INTO amenity (name) VALUES ('TV');
INSERT INTO amenity (name) VALUES ('Minibar');
INSERT INTO amenity (name) VALUES ('Bon tam');

INSERT INTO room_type_amenity (room_type_id, amenity_id)
SELECT rt.room_type_id, a.amenity_id
FROM room_type rt CROSS JOIN amenity a
WHERE a.name IN ('WiFi','Dieu hoa','TV')
   OR (a.name = 'Minibar' AND rt.name IN ('Deluxe','Suite'))
   OR (a.name = 'Bon tam' AND rt.name = 'Suite');

INSERT INTO rate_plan (hotel_id, code, name, includes_breakfast, cancellation_policy, free_cancel_days)
SELECT hotel_id, 'BAR', 'Gia niem yet', FALSE, 'Huy mien phi truoc 1 ngay', 1 FROM hotel;
INSERT INTO rate_plan (hotel_id, code, name, includes_breakfast, cancellation_policy, free_cancel_days)
SELECT hotel_id, 'BB', 'Gia kem an sang', TRUE, 'Huy mien phi truoc 2 ngay', 2 FROM hotel;

INSERT INTO room_rate (room_type_id, rate_plan_id, date_from, date_to, day_type, price)
SELECT rt.room_type_id, rp.rate_plan_id, DATE '2026-01-01', DATE '2026-12-31', 'ALL',
       rt.base_price + CASE rp.code WHEN 'BB' THEN 100000 ELSE 0 END
FROM room_type rt CROSS JOIN rate_plan rp;

INSERT INTO hotel_service (hotel_id, name, category, unit, unit_price)
SELECT hotel_id, 'Giat ui', 'LAUNDRY', 'kg', 30000 FROM hotel;
INSERT INTO hotel_service (hotel_id, name, category, unit, unit_price)
SELECT hotel_id, 'Nuoc suoi', 'MINIBAR', 'chai', 15000 FROM hotel;
INSERT INTO hotel_service (hotel_id, name, category, unit, unit_price)
SELECT hotel_id, 'Bua sang', 'FOOD', 'suat', 80000 FROM hotel;
INSERT INTO hotel_service (hotel_id, name, category, unit, unit_price)
SELECT hotel_id, 'Don san bay', 'TRANSPORT', 'chuyen', 250000 FROM hotel;

COMMIT;

/* ---------------------------------------------------------------------
   8. KIEM TRA SAU KHI CHAY
   --------------------------------------------------------------------- */
-- So bang (ky vong: 26)
-- SELECT COUNT(*) FROM user_tables;

-- Danh sach khoa ngoai
-- SELECT c.table_name, c.constraint_name, r.table_name AS references_table
-- FROM user_constraints c JOIN user_constraints r ON c.r_constraint_name = r.constraint_name
-- WHERE c.constraint_type = 'R' ORDER BY c.table_name;

/* ---------------------------------------------------------------------
   9. XOA TOAN BO (BO COMMENT KHI CAN LAM LAI TU DAU)
   --------------------------------------------------------------------- */
/*
DROP TABLE audit_log            CASCADE CONSTRAINTS PURGE;
DROP TABLE maintenance_request  CASCADE CONSTRAINTS PURGE;
DROP TABLE housekeeping_task    CASCADE CONSTRAINTS PURGE;
DROP TABLE invoice              CASCADE CONSTRAINTS PURGE;
DROP TABLE payment              CASCADE CONSTRAINTS PURGE;
DROP TABLE folio_item           CASCADE CONSTRAINTS PURGE;
DROP TABLE folio                CASCADE CONSTRAINTS PURGE;
DROP TABLE hotel_service        CASCADE CONSTRAINTS PURGE;
DROP TABLE stay_guest           CASCADE CONSTRAINTS PURGE;
DROP TABLE stay                 CASCADE CONSTRAINTS PURGE;
DROP TABLE room_night           CASCADE CONSTRAINTS PURGE;
DROP TABLE reservation_room     CASCADE CONSTRAINTS PURGE;
DROP TABLE reservation          CASCADE CONSTRAINTS PURGE;
DROP TABLE guest                CASCADE CONSTRAINTS PURGE;
DROP TABLE promotion            CASCADE CONSTRAINTS PURGE;
DROP TABLE room_rate            CASCADE CONSTRAINTS PURGE;
DROP TABLE rate_plan            CASCADE CONSTRAINTS PURGE;
DROP TABLE room_type_amenity    CASCADE CONSTRAINTS PURGE;
DROP TABLE amenity              CASCADE CONSTRAINTS PURGE;
DROP TABLE room                 CASCADE CONSTRAINTS PURGE;
DROP TABLE room_type            CASCADE CONSTRAINTS PURGE;
DROP TABLE user_role            CASCADE CONSTRAINTS PURGE;
DROP TABLE users                CASCADE CONSTRAINTS PURGE;
DROP TABLE employee             CASCADE CONSTRAINTS PURGE;
DROP TABLE app_role             CASCADE CONSTRAINTS PURGE;
DROP TABLE hotel                CASCADE CONSTRAINTS PURGE;
*/
