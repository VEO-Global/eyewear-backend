-- Drop old database if it exists
DROP DATABASE IF EXISTS eyewear_db;

-- Create database with UTF-8 support
CREATE DATABASE eyewear_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eyewear_db;
SET NAMES utf8mb4;

-- =============================================
-- 1. CORE SYSTEM & USERS
-- =============================================
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE -- CUSTOMER, SALES, OPERATIONS, MANAGER, ADMIN
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- User address book
CREATE TABLE user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_line TEXT NOT NULL, -- "123 Duong ABC, Phuong X"
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    ward VARCHAR(100),
    address_detail TEXT,
    is_default BOOLEAN DEFAULT FALSE, -- Default address
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================
-- 2. PRODUCT CATALOG
-- =============================================
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(100),
    description TEXT,
    base_price DECIMAL(15, 2) NOT NULL,
    material VARCHAR(100),
    gender VARCHAR(20),    -- Male, Female, Unisex
    model_3d_url TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    catalog_type VARCHAR(20) NOT NULL DEFAULT 'OLD',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE favorite_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorite_products_user_product UNIQUE (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_favorite_products_user_id
    ON favorite_products (user_id);

CREATE INDEX idx_favorite_products_product_id
    ON favorite_products (product_id);

CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    alt_text VARCHAR(255),
    is_thumbnail BOOLEAN DEFAULT FALSE,
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE, -- Stock keeping unit
    color VARCHAR(50) NOT NULL,
    size VARCHAR(20) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,   -- Variant-specific price
    stock_quantity INT NOT NULL DEFAULT 0,
    expected_restock_date DATETIME, -- Used for pre-order items
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_product_variants_product_color_size UNIQUE (product_id, color, size),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Lens products (assumed compatible with all frames)
CREATE TABLE lens_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50),           -- Single, Progressive
    refraction_index DECIMAL(4, 2),
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- =============================================
-- 3. CART (GIỎ HÀNG - LƯU DB)
-- =============================================
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL, -- Mỗi user 1 giỏ hàng
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    lens_product_id BIGINT, -- Null nếu chỉ mua gọng
    quantity INT DEFAULT 1,
    FOREIGN KEY (cart_id) REFERENCES carts(id),
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    FOREIGN KEY (lens_product_id) REFERENCES lens_products(id)
);

-- =============================================
-- 4. ORDERS (ĐƠN HÀNG)
-- =============================================
CREATE TABLE vouchers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percent INT,
    max_discount_amount DECIMAL(15, 2),
    start_date DATETIME,
    end_date DATETIME,
    quantity INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- Status: PENDING_PAYMENT -> PENDING_VERIFICATION -> WAITING_FOR_STOCK -> MANUFACTURING -> SHIPPING -> COMPLETED
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    order_type VARCHAR(20) NOT NULL, -- NORMAL, PRE_ORDER, PRESCRIPTION
    prescription_option VARCHAR(50),
    
    total_amount DECIMAL(15, 2) NOT NULL,
    shipping_fee DECIMAL(15, 2) DEFAULT 0,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    
    -- SNAPSHOT DATA (Lưu cứng text địa chỉ tại thời điểm đặt)
    shipping_address TEXT NOT NULL, 
    phone_number VARCHAR(20) NOT NULL,
    receiver_name VARCHAR(100) NOT NULL, -- Tên người nhận
    note TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    lens_product_id BIGINT,
    
    quantity INT NOT NULL,
    price DECIMAL(15, 2) NOT NULL, -- SNAPSHOT GIÁ (Giá lúc mua)
    
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    FOREIGN KEY (lens_product_id) REFERENCES lens_products(id)
);

-- =============================================
-- 5. PRESCRIPTION (DUYỆT TAY - NO AI)
-- =============================================
CREATE TABLE prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    lens_product_id BIGINT,
    
    -- Ảnh toa thuốc khách up (Bắt buộc)
    prescription_image_url TEXT NOT NULL,
    lens_name_snapshot VARCHAR(255),
    lens_price_snapshot DECIMAL(15, 2),
    lens_description_snapshot TEXT,
    
    -- Kết quả xác thực (Nhập tay bởi Sale)
    sphere_od DECIMAL(5, 2), -- Mắt Phải
    sphere_os DECIMAL(5, 2), -- Mắt Trái
    cylinder_od DECIMAL(5, 2),
    cylinder_os DECIMAL(5, 2),
    axis_od INT,
    axis_os INT,
    pd DECIMAL(5, 2),
    review_status VARCHAR(50),
    
    verified_by BIGINT, -- Sale nào duyệt?
    verified_at DATETIME,
    created_at DATETIME,
    staff_note TEXT,
    
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (lens_product_id) REFERENCES lens_products(id),
    FOREIGN KEY (verified_by) REFERENCES users(id)
);

-- =============================================
-- 6. PAYMENT & CONFIG
-- =============================================
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL, -- COD, BANK_TRANSFER
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, FAILED
    amount DECIMAL(15, 2) NOT NULL,
    transaction_code VARCHAR(100),
    payment_proof_img TEXT,
    paid_at DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE system_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE business_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT chk_business_policy_type
        CHECK (type IN ('PURCHASE', 'RETURN', 'WARRANTY', 'SHIPPING', 'PRIVACY'))
);

-- =============================================
-- 7. CONSULTATION APPOINTMENTS
-- =============================================
CREATE TABLE consultation_appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    appointment_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    CONSTRAINT chk_consultation_appointment_status
        CHECK (status IN ('pending', 'completed'))
);

CREATE INDEX idx_consultation_appointments_time
    ON consultation_appointments (appointment_time);
CREATE INDEX idx_consultation_appointments_phone
    ON consultation_appointments (phone_number);
CREATE INDEX idx_consultation_appointments_status
    ON consultation_appointments (status);

CREATE TABLE user_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NULL,
    message LONGTEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    source_module VARCHAR(50) NOT NULL,
    metadata_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    CONSTRAINT chk_user_notification_type
        CHECK (type IN ('success', 'error', 'warning', 'info')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_notifications_user_id
    ON user_notifications (user_id);
CREATE INDEX idx_user_notifications_user_read_created
    ON user_notifications (user_id, is_read, created_at);
CREATE INDEX idx_user_notifications_user_created
    ON user_notifications (user_id, created_at);
CREATE INDEX idx_user_notifications_expires_at
    ON user_notifications (expires_at);

-- DATA MẪU CƠ BẢN
INSERT INTO roles (name) VALUES ('CUSTOMER'), ('SALES'), ('OPERATIONS'), ('MANAGER'), ('ADMIN');
INSERT INTO categories (name) VALUES ('Kính râm'), ('Gọng kính');
INSERT INTO consultation_appointments (phone_number, appointment_time, status) VALUES
('0901234567', '2026-03-18 09:00:00', 'completed'),
('0912345678', '2026-03-18 14:30:00', 'completed'),
('0934567890', '2026-03-19 10:15:00', 'completed'),
('0945678901', '2026-03-24 08:30:00', 'pending'),
('0956789012', '2026-03-24 10:00:00', 'pending'),
('0967890123', '2026-03-24 15:00:00', 'pending'),
('0978901234', '2026-03-25 09:30:00', 'pending'),
('0989012345', '2026-03-25 16:00:00', 'pending');

-- =============================================
-- DATA 
-- =============================================
-- 1. PRODUCTS (Sản phẩm bắt buộc có link 3D)
-- category_id 1 = Kính râm, 2 = Gọng kính
INSERT INTO products 
(category_id, name, brand, description, base_price, material, gender, model_3d_url, status, catalog_type, is_active)
VALUES
(2,'Gọng Titan Cao Cấp','RayVision','Gọng kính titan cao cấp, bền nhẹ và sang trọng.',3200000,'Titanium','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260443/titanium_frame_glass_f1ffh9.glb','AVAILABLE','NEW',TRUE),

(2,'Kính Bảo Hộ Cận','OptiSafe','Gọng kính bảo hộ dành cho người cần lắp tròng cận.',2500000,'Polycarbonate','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260366/safety_glasses_prescription_mkbtst.glb','AVAILABLE','NEW',TRUE),

(2,'Gọng Phố Cổ 4','VistaWear','Mẫu gọng đeo hằng ngày với phong cách hiện đại, dễ phối đồ.',1800000,'Acetate','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260353/glasses_4_tsxlvu.glb','AVAILABLE','NEW',TRUE),

(2,'Gọng Tối Giản 08','NeoOptic','Thiết kế thanh lịch, mỏng nhẹ và phù hợp nhiều khuôn mặt.',2100000,'Stainless Steel','Female','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260352/glasses_08_ma8hbq.glb','AVAILABLE','NEW',TRUE),

(2,'Gọng Vuông Cổ Điển 05','ClassicEyes','Thiết kế vuông mang hơi hướng cổ điển, đậm cá tính.',1950000,'Acetate','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260348/glasses_05_eqqwbl.glb','AVAILABLE','NEW',TRUE),

(2,'Gọng Dẻo Hiện Đại','VisionPro','Gọng TR90 dẻo nhẹ, phù hợp sử dụng hằng ngày.',2300000,'TR90','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260346/glasses_jbal71.glb','AVAILABLE','NEW',TRUE),

(2,'Wayfarer Huyền Thoại','SunElite','Phong cách wayfarer kinh điển, luôn hợp xu hướng.',2800000,'Acetate','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260326/wayfarer_sunglasses_eyeglasses_rims_qe4yzv.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Thanh Mảnh 07','Optima','Thiết kế mảnh, gọn và tinh tế cho phong cách nữ tính.',1750000,'Metal','Female','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260324/glasses_07_ml2zlr.glb','AVAILABLE','OLD',TRUE),

(2,'Phong Cách Đô Thị 3','VistaWear','Gọng kính đơn giản, phù hợp đi học, đi làm mỗi ngày.',1600000,'Plastic','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260259/glasses_3_lsyyec.glb','AVAILABLE','OLD',TRUE),

(2,'Titan AirLite Siêu Nhẹ','RayVision','Gọng titan siêu nhẹ, đeo lâu vẫn thoải mái.',3500000,'Titanium','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260256/titanium_frame_glass_oegsdw.glb','AVAILABLE','OLD',TRUE),

(1,'Kính Thông Minh MetaQuest','FutureSight','Mẫu kính thông minh lấy cảm hứng từ thiết bị VR hiện đại.',5200000,'Composite','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260211/oak_ley_metaquest_glasses_vr_oamqll.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Chữ Nhật Bo Góc','UrbanEyes','Thiết kế chữ nhật bo tròn, dễ đeo và trẻ trung.',2000000,'Acetate','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260210/rounded_rectangle_eyeglasses_kfmklb.glb','AVAILABLE','OLD',TRUE),

(2,'Phiên Bản Zev Cao Cấp','HeritageOptic','Gọng thủ công cao cấp dành cho khách thích sự sang trọng.',4800000,'Acetate','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260190/glasses_-_moscot_zev-tt_se_wjpswa.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Cơ Bản 2','OptiCore','Mẫu gọng cơ bản, dễ đeo và phù hợp nhu cầu hằng ngày.',1500000,'Plastic','Female','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260190/glasses_2_swgysz.glb','AVAILABLE','OLD',TRUE),

(2,'Phong Cách Biểu Tượng','LuxView','Thiết kế cao cấp lấy cảm hứng từ các mẫu kính kinh điển.',4200000,'Metal','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260188/ray_ban_glasses_rrozi3.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Trong Suốt','ClearSight','Gọng acetate trong suốt trẻ trung và hiện đại.',1900000,'Acetate','Female','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260182/glass_eye_m6yobl.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Truyền Thống','SpecWorld','Mẫu gọng full-rim truyền thống, chắc chắn và dễ đeo.',1700000,'Metal','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260179/eyewear_specs_cdzo8u.glb','AVAILABLE','OLD',TRUE),

(2,'Phi Công Thép','SkyVision','Mẫu gọng dáng aviator mạnh mẽ và thời trang.',3100000,'Stainless Steel','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260178/aviator_glasses_i47nag.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Rigel','StarOptic','Gọng nhẹ cao cấp, đeo êm và bền cho nhu cầu lâu dài.',2600000,'TR90','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260164/glasses_rigel_bqekyu.glb','AVAILABLE','OLD',TRUE),

(2,'Gọng Milano','ItaliaEyes','Thiết kế mang cảm hứng Ý, thanh lịch và thời trang.',3900000,'Acetate','Female','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260161/eyeglasses_-_occhiali_spakik.glb','AVAILABLE','OLD',TRUE),

(2,'Đường Nét Thanh Lịch 09','NeoVision','Mẫu gọng mảnh với đường nét hiện đại, tinh gọn.',2200000,'Metal','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260158/glasses_09_hdsqv7.glb','AVAILABLE','OLD',TRUE),

(2,'A01 Phố Thị','StreetOptic','Mẫu gọng thời trang giá tốt, phù hợp người trẻ.',1400000,'Plastic','Male','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260155/eyeglasses_a01_mzv0g8.glb','AVAILABLE','OLD',TRUE),

(2,'Đen Cổ Điển','DarkVision','Gọng đen nhám thanh lịch, phù hợp nhiều phong cách.',2000000,'Acetate','Unisex','https://res.cloudinary.com/dw4q0ajrr/image/upload/v1772260138/black_eyeglasses_qfrtem.glb','AVAILABLE','OLD',TRUE);

INSERT INTO product_images (product_id, image_url, alt_text, is_thumbnail, is_primary, sort_order) VALUES
(1, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=1200&q=80', 'Góc chính gọng titan cao cấp', TRUE, TRUE, 0),
(1, 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1200&q=80', 'Góc nghiêng trái gọng titan cao cấp', FALSE, FALSE, 1),
(1, 'https://images.unsplash.com/photo-1508296695146-257a814070b4?auto=format&fit=crop&w=1200&q=80', 'Cận cảnh mặt trước gọng titan cao cấp', FALSE, FALSE, 2),
(2, 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=80', 'Góc chính kính bảo hộ cận', TRUE, TRUE, 0),
(2, 'https://images.unsplash.com/photo-1574258495973-f010dfbb5371?auto=format&fit=crop&w=1200&q=80', 'Cận cảnh tròng kính bảo hộ cận', FALSE, FALSE, 1),
(3, 'https://images.unsplash.com/photo-1577803645773-f96470509666?auto=format&fit=crop&w=1200&q=80', 'Góc chính gọng phố cổ 4', TRUE, TRUE, 0),
(3, 'https://images.unsplash.com/photo-1591076482161-42ce6da69f67?auto=format&fit=crop&w=1200&q=80', 'Góc nghiêng gọng phố cổ 4', FALSE, FALSE, 1),
(4, 'https://images.unsplash.com/photo-1591076482161-42ce6da69f67?auto=format&fit=crop&w=1200&q=80', 'Góc chính gọng tối giản 08', TRUE, TRUE, 0),
(5, 'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=1200&q=80', 'Góc chính gọng vuông cổ điển 05', TRUE, TRUE, 0),
(6, 'https://images.unsplash.com/photo-1574258495973-f010dfbb5371?auto=format&fit=crop&w=1200&q=80', 'Góc chính gọng dẻo hiện đại', TRUE, TRUE, 0),
(6, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=1200&q=80', 'Góc nghiêng gọng dẻo hiện đại', FALSE, FALSE, 1),
(7, 'https://images.unsplash.com/photo-1508296695146-257a814070b4?auto=format&fit=crop&w=1200&q=80', 'Góc chính wayfarer huyền thoại', TRUE, TRUE, 0),
(8, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=1200&q=80', 'Góc chính gọng thanh mảnh 07', TRUE, TRUE, 0),
(9, 'https://images.unsplash.com/photo-1508296695146-257a814070b4?auto=format&fit=crop&w=1200&q=80', 'Góc chính phong cách đô thị 3', TRUE, TRUE, 0),
(10, 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1200&q=80', 'Góc chính titan airlite siêu nhẹ', TRUE, TRUE, 0);


-- 2. PRODUCT_VARIANT
-- SKU (10 ký tự) = [BR][SZ][CL][PID][RND]
-- BR   : 2 ký tự viết tắt brand  
-- SZ   : Size (S / M / L)  
-- CL   : Color code  
-- PID  : Product ID (2 số)  
-- RND  : 4 ký tự random để tránh trùng
INSERT INTO product_variants
(product_id, sku, color, size, price, stock_quantity, expected_restock_date, is_active)
VALUES

-- 1 Titanium Edge Pro
(1,'RVSB01A7K2','Black','S',3200000,0,'2026-04-15',TRUE),
(1,'RVMB01F3L8','Black','M',3260000,0,'2026-04-20',TRUE),
(1,'RVLB01D1Q4','Black','L',3290000,0,'2026-04-25',TRUE),
(1,'RVSS01T2Z8','Silver','S',3200000,0,'2026-04-28',TRUE),
(1,'RVMS01H4P6','Silver','M',3255000,0,'2026-05-01',TRUE),
(1,'RVLS01K9W3','Silver','L',3285000,0,'2026-05-04',TRUE),

-- 2 Safety Shield RX
(2,'OSSC02J8P3','Clear','S',2500000,0,'2026-04-18',TRUE),
(2,'OSMC02H4W6','Clear','M',2555000,0,'2026-04-21',TRUE),
(2,'OSLC02K2X9','Clear','L',2590000,0,'2026-04-26',TRUE),

-- 3 Urban Classic 4
(3,'VWSB03T7A1','Black','S',1800000,0,'2026-04-18',TRUE),
(3,'VWMB03R4C6','Black','M',1855000,0,'2026-04-22',TRUE),
(3,'VWLB03P1Z7','Black','L',1890000,0,'2026-04-27',TRUE),
(3,'VWST03M8Q2','Tortoise','S',1800000,0,'2026-04-30',TRUE),
(3,'VWMT03J3W5','Tortoise','M',1860000,0,'2026-05-02',TRUE),
(3,'VWLT03F1E9','Tortoise','L',1895000,0,'2026-05-06',TRUE),

-- 4 Minimal Frame 08
(4,'NOSG04B6N3','Gold','S',2100000,0,'2026-04-30',TRUE),
(4,'NOMG04D2L5','Gold','M',2160000,0,'2026-05-05',TRUE),
(4,'NOLG04F8R2','Gold','L',2190000,0,'2026-05-10',TRUE),

-- 5 Retro Square 05
(5,'CEST05H2A9','Tortoise','S',1950000,0,'2026-05-04',TRUE),
(5,'CEMT05K4P1','Tortoise','M',2005000,0,'2026-05-07',TRUE),
(5,'CELT05Q7W8','Tortoise','L',2040000,0,'2026-05-12',TRUE),

-- 6 Modern Flex
(6,'VPSB06K8J2','Black','S',2300000,0,'2026-05-01',TRUE),
(6,'VPMB06D3H5','Black','M',2355000,0,'2026-05-04',TRUE),
(6,'VPLB06L2Q8','Black','L',2390000,0,'2026-05-07',TRUE),

-- 7 Wayfarer Legend
(7,'SESB07A8M2','Black','S',2800000,9,'2026-05-02',TRUE),
(7,'SEMB07H6L5','Black','M',2860000,10,'2026-05-06',TRUE),
(7,'SELB07T2R9','Black','L',2895000,12,'2026-05-10',TRUE),
(7,'SESG07Q1P4','Gold','S',2800000,8,'2026-05-12',TRUE),
(7,'SEMG07J3F8','Gold','M',2855000,9,'2026-05-15',TRUE),
(7,'SELG07Z7X1','Gold','L',2890000,11,'2026-05-18',TRUE),

-- 8 Sleek Vision 07
(8,'OPSS08B9L2','Silver','S',1750000,9,'2026-04-22',TRUE),
(8,'OPMS08K3T6','Silver','M',1805000,8,'2026-04-25',TRUE),
(8,'OPLS08D2Q1','Silver','L',1840000,11,'2026-04-29',TRUE),

-- 9 Metro Style 3
(9,'VWSB09T7L2','Black','S',1600000,10,'2026-04-20',TRUE),
(9,'VWMB09P5C4','Black','M',1650000,12,'2026-04-23',TRUE),
(9,'VWLB09Q2R8','Black','L',1690000,9,'2026-04-26',TRUE),

-- 10 Titanium AirLite
(10,'RVSS10A4L9','Silver','S',3500000,8,'2026-05-01',TRUE),
(10,'RVMS10D7P3','Silver','M',3560000,10,'2026-05-05',TRUE),
(10,'RVLS10F2Q7','Silver','L',3590000,11,'2026-05-09',TRUE),

-- 11 MetaQuest VR Glass
(11,'FSUB11H7Q2','Black','S',5200000,9,'2026-05-02',TRUE),
(11,'FSUM11A3T5','Black','M',5260000,8,'2026-05-07',TRUE),
(11,'FSUL11D9L1','Black','L',5295000,10,'2026-05-11',TRUE),

-- 12 Rounded Rectangle Pro
(12,'UESB12K4P3','Black','S',2000000,8,'2026-05-01',TRUE),
(12,'UEMB12T9A5','Black','M',2055000,11,'2026-05-04',TRUE),
(12,'UELB12R2C7','Black','L',2090000,10,'2026-05-08',TRUE),
(12,'UESG12H7F2','Gold','S',2000000,9,'2026-05-11',TRUE),
(12,'UEMG12P3Z6','Gold','M',2060000,8,'2026-05-14',TRUE),
(12,'UELG12Q1X9','Gold','L',2095000,12,'2026-05-18',TRUE),

-- 13 Moscot Zev Edition
(13,'HOSB13L4K7','Black','S',4800000,9,'2026-05-03',TRUE),
(13,'HOMB13A7Q1','Black','M',4860000,11,'2026-05-06',TRUE),
(13,'HOLB13P9C4','Black','L',4890000,10,'2026-05-10',TRUE),

-- 14 Classic Vision 2
(14,'OCSB14M2T8','Black','S',1500000,8,'2026-04-21',TRUE),
(14,'OCMB14H6D1','Black','M',1555000,9,'2026-04-24',TRUE),
(14,'OCLB14F3Z5','Black','L',1590000,11,'2026-04-27',TRUE),
(14,'OCSC14R7K2','Clear','S',1500000,10,'2026-04-29',TRUE),
(14,'OCMC14J1L9','Clear','M',1560000,8,'2026-05-02',TRUE),
(14,'OCLC14A8W3','Clear','L',1595000,12,'2026-05-05',TRUE),

-- 15 RayBan Inspired
(15,'LVSB15Q7M3','Black','S',4200000,11,'2026-05-01',TRUE),
(15,'LVMB15A4C6','Black','M',4260000,10,'2026-05-04',TRUE),
(15,'LVLB15P1X8','Black','L',4290000,9,'2026-05-08',TRUE),

-- 16 Crystal Clear Eye
(16,'CSSC16L8R1','Clear','S',1900000,9,'2026-04-20',TRUE),
(16,'CSMC16K4D3','Clear','M',1955000,8,'2026-04-23',TRUE),
(16,'CSLC16J9Q6','Clear','L',1990000,10,'2026-04-27',TRUE),
(16,'CSSG16T2Z4','Gold','S',1900000,11,'2026-04-30',TRUE),
(16,'CSMG16P3L7','Gold','M',1960000,9,'2026-05-03',TRUE),
(16,'CSLG16F1A8','Gold','L',1995000,12,'2026-05-07',TRUE),

-- 17 Specs Classic
(17,'SWMB17T6K2','Metal','S',1700000,10,'2026-04-22',TRUE),
(17,'SWMM17D8P4','Metal','M',1755000,9,'2026-04-25',TRUE),
(17,'SWML17Q3X7','Metal','L',1790000,11,'2026-04-28',TRUE),

-- 18 Aviator Steel
(18,'SVSB18A5R2','Silver','S',3100000,8,'2026-05-02',TRUE),
(18,'SVMB18H7D4','Silver','M',3160000,11,'2026-05-05',TRUE),
(18,'SVLB18K2Z8','Silver','L',3190000,10,'2026-05-08',TRUE),
(18,'SVSB18G1F6','Black','S',3100000,9,'2026-05-10',TRUE),
(18,'SVMB18L9P3','Black','M',3155000,8,'2026-05-13',TRUE),
(18,'SVLB18T4C1','Black','L',3185000,12,'2026-05-16',TRUE),

-- 19 Rigel Frame
(19,'SOSB19H4L8','Black','S',2600000,11,'2026-05-01',TRUE),
(19,'SOMB19F7Q2','Black','M',2655000,10,'2026-05-04',TRUE),
(19,'SOLB19A9D6','Black','L',2690000,9,'2026-05-07',TRUE),

-- 20 Occhiali Milano
(20,'IESB20K8R4','Black','S',3900000,9,'2026-05-02',TRUE),
(20,'IEMB20T6P2','Black','M',3960000,8,'2026-05-05',TRUE),
(20,'IELB20D3Q7','Black','L',3990000,11,'2026-05-08',TRUE),
(20,'IESC20A1Z9','Clear','S',3900000,10,'2026-05-10',TRUE),
(20,'IEMC20L4F3','Clear','M',3955000,9,'2026-05-13',TRUE),
(20,'IELC20H7X5','Clear','L',3985000,12,'2026-05-17',TRUE),

-- 21 Elegant Line 09
(21,'NVSB21J6M3','Black','S',2200000,10,'2026-04-22',TRUE),
(21,'NVMB21K2Q7','Black','M',2260000,9,'2026-04-25',TRUE),
(21,'NVLB21F9P1','Black','L',2290000,8,'2026-04-29',TRUE),

-- 22 A01 Urban
(22,'STSB22L3T5','Black','S',1400000,11,'2026-04-18',TRUE),
(22,'STMB22H6D2','Black','M',1455000,10,'2026-04-21',TRUE),
(22,'STLB22Q1C9','Black','L',1490000,9,'2026-04-25',TRUE),
(22,'STSS22A8F4','Silver','S',1400000,8,'2026-04-28',TRUE),
(22,'STMS22R4K6','Silver','M',1460000,12,'2026-05-01',TRUE),
(22,'STLS22Z7P3','Silver','L',1495000,10,'2026-05-04',TRUE),

-- 23 Black Classic
(23,'DVSB23M7Q1','Black','S',2000000,9,'2026-04-19',TRUE),
(23,'DVMB23H3T4','Black','M',2055000,10,'2026-04-22',TRUE),
(23,'DVLB23F8A6','Black','L',2090000,11,'2026-04-26',TRUE);

-- . USERS
INSERT INTO users
(role_id, email, password_hash, full_name, phone, avatar_url, is_active, created_at)
VALUES

-- ===== 30 CUSTOMER (role_id = 1) =====
(1,'nguyenminhanh@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Nguyễn Minh Anh','0903847291','https://i.pravatar.cc/150?img=1',TRUE,'2025-09-14 10:21:33'),
(1,'tranthikimngan@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Thị Kim Ngân','0916728453','https://i.pravatar.cc/150?img=2',TRUE,'2025-09-28 18:45:12'),
(1,'levanthanh@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Lê Văn Thành','0935281746','https://i.pravatar.cc/150?img=3',TRUE,'2025-10-03 09:11:07'),
(1,'phamthuthao@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Thu Thảo','0983175624','https://i.pravatar.cc/150?img=4',TRUE,'2025-10-19 14:36:55'),
(1,'hoangminhduc@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Hoàng Minh Đức','0972468135','https://i.pravatar.cc/150?img=5',TRUE,'2025-11-02 21:04:40'),
(1,'dangngocanh@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đặng Ngọc Anh','0964827153','https://i.pravatar.cc/150?img=6',TRUE,'2025-11-17 11:25:18'),
(1,'buitrungkien@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Bùi Trung Kiên','0395728461','https://i.pravatar.cc/150?img=7',TRUE,'2025-12-06 16:02:29'),
(1,'vothimy@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Võ Thị My','0384617295','https://i.pravatar.cc/150?img=8',TRUE,'2025-12-22 20:41:06'),
(1,'dinhquangvinh@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đinh Quang Vinh','0372846159','https://i.pravatar.cc/150?img=9',TRUE,'2026-01-08 08:14:52'),
(1,'phanthanhphong@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phan Thành Phong','0365172948','https://i.pravatar.cc/150?img=10',TRUE,'2026-01-21 19:53:37'),
(1,'nguyenthanhdat@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Nguyễn Thành Đạt','0907153842','https://i.pravatar.cc/150?img=11',TRUE,'2026-02-05 12:18:44'),
(1,'tranminhquan@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Minh Quân','0912847563','https://i.pravatar.cc/150?img=12',TRUE,'2026-02-18 17:26:11'),
(1,'levanthien@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Lê Văn Thiện','0926715834','https://i.pravatar.cc/150?img=13',TRUE,'2026-02-27 09:47:03'),
(1,'phamducanh@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Đức Anh','0934172685','https://i.pravatar.cc/150?img=14',TRUE,'2026-03-02 15:34:21'),
(1,'hoangngocbao@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Hoàng Ngọc Bảo','0965712846','https://i.pravatar.cc/150?img=15',TRUE,'2026-03-06 22:10:09'),
(1,'nguyenthilan@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Nguyễn Thị Lan','0973816524','https://i.pravatar.cc/150?img=16',TRUE,'2025-10-12 10:12:12'),
(1,'tranthuytrang@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Thùy Trang','0986241753','https://i.pravatar.cc/150?img=17',TRUE,'2025-10-29 15:31:44'),
(1,'phamminhtuan@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Minh Tuấn','0391847265','https://i.pravatar.cc/150?img=18',TRUE,'2025-11-09 13:05:19'),
(1,'levandung@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Lê Văn Dũng','0385271946','https://i.pravatar.cc/150?img=19',TRUE,'2025-11-21 20:45:07'),
(1,'dangthuyanh@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đặng Thùy Anh','0376928451','https://i.pravatar.cc/150?img=20',TRUE,'2025-12-04 08:22:33'),
(1,'vominhchau@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Võ Minh Châu','0368452173','https://i.pravatar.cc/150?img=21',TRUE,'2025-12-15 11:42:10'),
(1,'buitrangan@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Bùi Trà Ngân','0905274816','https://i.pravatar.cc/150?img=22',TRUE,'2026-01-04 18:55:29'),
(1,'dinhthanhson@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đinh Thành Sơn','0916384725','https://i.pravatar.cc/150?img=23',TRUE,'2026-01-14 09:08:44'),
(1,'hoangkimyen@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Hoàng Kim Yến','0937481625','https://i.pravatar.cc/150?img=24',TRUE,'2026-01-29 21:11:13'),
(1,'phamquynhchi@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Quỳnh Chi','0964728513','https://i.pravatar.cc/150?img=25',TRUE,'2026-02-09 16:32:55'),
(1,'tranvanloc@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Văn Lộc','0975263841','https://i.pravatar.cc/150?img=26',TRUE,'2026-02-16 14:28:41'),
(1,'nguyenkimlien@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Nguyễn Kim Liên','0987341625','https://i.pravatar.cc/150?img=27',TRUE,'2026-02-23 10:15:19'),
(1,'levanphuc@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Lê Văn Phúc','0396284715','https://i.pravatar.cc/150?img=28',TRUE,'2026-03-01 12:42:30'),
(1,'dangngocmai@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đặng Ngọc Mai','0384716259','https://i.pravatar.cc/150?img=29',TRUE,'2026-03-05 19:05:44'),
(1,'vothanhha@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Võ Thanh Hà','0375148263','https://i.pravatar.cc/150?img=30',TRUE,'2026-03-07 17:20:55'),

-- ===== SALES (role_id = 2) =====
(2,'minhtuan.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Nguyễn Minh Tuấn','0908437261','https://i.pravatar.cc/150?img=31',TRUE,'2025-10-10 09:12:00'),
(2,'thanhhang.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Thanh Hằng','0917362548','https://i.pravatar.cc/150?img=32',TRUE,'2025-10-14 11:30:22'),
(2,'quanghuy.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Quang Huy','0936281745','https://i.pravatar.cc/150?img=33',TRUE,'2025-11-01 15:22:33'),
(2,'kimngan.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Lê Kim Ngân','0968152743','https://i.pravatar.cc/150?img=34',TRUE,'2025-11-18 17:44:11'),
(2,'minhtri.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Võ Minh Trí','0973628451','https://i.pravatar.cc/150?img=35',TRUE,'2025-12-03 10:00:02'),
(2,'anhthu.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Bùi Anh Thư','0982746153','https://i.pravatar.cc/150?img=36',TRUE,'2025-12-20 12:50:30'),
(2,'thanhdat.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đặng Thành Đạt','0395726148','https://i.pravatar.cc/150?img=37',TRUE,'2026-01-07 08:18:55'),
(2,'ngocanh.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Hoàng Ngọc Anh','0384638152','https://i.pravatar.cc/150?img=38',TRUE,'2026-01-25 14:33:20'),
(2,'minhchau.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Đinh Minh Châu','0378152463','https://i.pravatar.cc/150?img=39',TRUE,'2026-02-11 16:12:40'),
(2,'thutrang.sales@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phan Thu Trang','0362748159','https://i.pravatar.cc/150?img=40',TRUE,'2026-02-26 09:41:17'),

-- ===== OPERATIONS =====
(3,'vanduc.operations@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Lê Văn Đức','0903846257','https://i.pravatar.cc/150?img=41',TRUE,'2025-10-21 11:11:11'),
(3,'kimyen.operations@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Kim Yến','0915273648','https://i.pravatar.cc/150?img=42',TRUE,'2025-11-13 16:22:18'),
(3,'thanhson.operations@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Nguyễn Thành Sơn','0936847251','https://i.pravatar.cc/150?img=43',TRUE,'2026-01-09 08:55:44'),
(3,'ngocmai.operations@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Ngọc Mai','0962846175','https://i.pravatar.cc/150?img=44',TRUE,'2026-02-20 13:10:30'),

-- ===== MANAGER =====
(4,'minhquan.manager@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Trần Minh Quân','0973846251','https://i.pravatar.cc/150?img=45',TRUE,'2025-11-05 09:40:12'),
(4,'thanhha.manager@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Võ Thanh Hà','0982746351','https://i.pravatar.cc/150?img=46',TRUE,'2026-01-15 10:21:55'),
(4,'quynhchi.manager@gmail.com','$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC','Phạm Quỳnh Chi','0395724618','https://i.pravatar.cc/150?img=47',TRUE,'2026-02-27 17:09:33'),

-- ===== ADMIN =====
(5,'admin.system@gmail.com','$2b$10$zX7QpL4mYk8H2vN9rT1UeC5bW3dF6sA0jKqP9xR2M5cV8nG1hD4E','System Admin','0963847251','https://i.pravatar.cc/150?img=48',TRUE,'2025-09-10 08:00:00'),
(5,'admin@veo.com', '$2a$10$7clppnCzRAHln6wQnhBOruC3RoHZa022OjHafdqoHhr6LCbEqdkFa', 'Admin Tối Cao', '0999999999', 'https://i.pravatar.cc/150?img=49', TRUE, null), -- Admin123@
(1, 'khach@veo.com', '$2a$10$dgyzLBvwjb7JVHn6ziFLtuTfKn.6awWC98Pld34sqP.lcOhIo1rXC', 'Khách Hàng VIP', '0888888888', 'https://i.pravatar.cc/150?img=50', TRUE, null), -- Customer123@
(5, 'admin@gmail.com', '$2a$10$Y5EkVfS3gsqvLiTRQ.QsD.1Q9jKBiRh81.4Fa0WZ92yK29qSWXWSK', 'Admin', '0111111111', 'https://i.pravatar.cc/150?img=51', TRUE, '2025-09-10 08:00:00'), -- Admin@123
(4, 'manager@gmail.com', '$2a$10$MSgmpBcPsxXhI4OFrbk.jeAIV9iIOOz0X0ueEtxRe4z342YT0/4J2', 'Manager', '0888888888', 'https://i.pravatar.cc/150?img=52', TRUE, '2025-09-10 09:00:00'), -- Manager@123
(2, 'sale@gmail.com', '$2a$10$w5Qft4vA8BKOtyuI0dfzHuVgUh/oIeAdSZcgz2nPai4rvGJFaLTWu', 'Sale Staff', '0777777777', 'https://i.pravatar.cc/150?img=53', TRUE, '2025-09-10 10:00:00'), -- Sale@123
(3, 'operation@gmail.com', '$2a$10$QoWj7WBRWyIll.yc5FDTiOe86OdCJg/bu3TT89LFnSnPVrlvGI3tq', 'Operation Staff', '0666666666', 'https://i.pravatar.cc/150?img=54', TRUE, '2025-09-10 11:00:00'); -- Operation@123

INSERT INTO carts (user_id, created_at) VALUES
(1,'2026-03-01 09:10:00'),
(2,'2026-03-01 10:15:00'),
(3,'2026-03-02 08:20:00'),
(4,'2026-03-02 10:40:00'),
(5,'2026-03-02 13:10:00'),
(6,'2026-03-03 07:10:00'),
(7,'2026-03-03 09:30:00'),
(8,'2026-03-03 12:15:00'),
(9,'2026-03-03 15:30:00'),
(10,'2026-03-04 08:05:00'),
(11,'2026-03-04 09:45:00'),
(12,'2026-03-04 10:55:00'),
(13,'2026-03-04 12:05:00'),
(14,'2026-03-04 13:25:00'),
(15,'2026-03-04 15:10:00'),
(16,'2026-03-05 08:05:00'),
(17,'2026-03-05 09:40:00'),
(18,'2026-03-05 11:55:00'),
(19,'2026-03-05 13:20:00'),
(20,'2026-03-05 15:10:00'),
(21,'2026-03-06 08:20:00'),
(22,'2026-03-06 09:30:00'),
(23,'2026-03-06 10:40:00'),
(24,'2026-03-06 12:10:00'),
(25,'2026-03-07 08:05:00'),
(26,'2026-03-07 09:20:00'),
(27,'2026-03-07 10:30:00'),
(28,'2026-03-07 11:50:00'),
(29,'2026-03-07 14:40:00'),
(30,'2026-03-07 16:20:00');

-- LENS_PRODUCTS
INSERT INTO lens_products
(name, type, refraction_index, description, price, is_active)
VALUES
('Tròng Đơn Tròng','Vision Correction',1.50,'Tròng cơ bản hỗ trợ điều chỉnh một tầm nhìn, gần hoặc xa.',500000,TRUE),
('Tròng Hai Tròng','Vision Correction',1.50,'Tròng có hai vùng nhìn, hỗ trợ nhìn gần và nhìn xa.',900000,TRUE),
('Tròng Ba Tròng','Vision Correction',1.50,'Tròng có ba vùng nhìn gồm xa, trung gian và gần.',1200000,TRUE),
('Tròng Đa Tròng','Vision Correction',1.60,'Tròng đa tròng không vạch, chuyển vùng nhìn mượt mà.',2500000,TRUE),
('Tròng Đổi Màu','Adaptive Lens',1.56,'Tròng tự động sẫm màu khi ra nắng và trong lại khi ở trong nhà.',1800000,TRUE),
('Tròng Phân Cực','Sun Protection',1.56,'Tròng giảm chói từ các bề mặt phản chiếu như đường hoặc mặt nước.',1600000,TRUE),
('Tròng Chống Ánh Sáng Xanh','Digital Protection',1.56,'Tròng hỗ trợ lọc ánh sáng xanh từ màn hình điện tử.',900000,TRUE),
('Tròng Chiết Suất Cao','Thin Lens',1.67,'Tròng mỏng nhẹ hơn, phù hợp người có độ kính cao.',2200000,TRUE),
('Tròng Phi Cầu','Optical Design',1.60,'Thiết kế tròng phẳng hơn, giảm méo ảnh và tăng thẩm mỹ.',2000000,TRUE),
('Tròng Chống Phản Quang','Lens Coating',1.50,'Tròng phủ lớp chống chói và giảm phản xạ ánh sáng.',700000,TRUE),
('Tròng Polycarbonate','Impact Resistant',1.59,'Tròng nhẹ và có khả năng chịu va đập tốt.',1200000,TRUE),
('Tròng Trivex','Impact Resistant',1.53,'Tròng cao cấp nhẹ, trong và bền chắc.',1500000,TRUE),
('Tròng Chống Trầy','Lens Coating',1.50,'Tròng được phủ lớp giúp hạn chế trầy xước bề mặt.',650000,TRUE),
('Tròng Chống Tia UV','Protective Lens',1.50,'Tròng giúp ngăn tia cực tím có hại cho mắt.',750000,TRUE),
('Tròng Lái Xe','Special Purpose',1.56,'Tròng tối ưu cho việc lái xe với khả năng tăng tương phản và giảm chói.',1100000,TRUE),
('Tròng Dùng Máy Tính','Special Purpose',1.56,'Tròng tối ưu cho khoảng nhìn trung gian khi làm việc với máy tính.',950000,TRUE),
('Tròng Đọc Sách','Vision Correction',1.50,'Tròng hỗ trợ nhìn gần, phù hợp cho nhu cầu đọc sách.',600000,TRUE),
('Tròng Bảo Hộ','Protective Lens',1.59,'Tròng bền chắc dùng cho kính bảo hộ trong môi trường làm việc.',1300000,TRUE);

INSERT INTO cart_items (cart_id, product_variant_id, lens_product_id, quantity) VALUES
-- Cart 1 (PRESCRIPTION)
(1,2,5,1),
(1,7,3,1),
-- Cart 2
(2,4,6,1),
-- Cart 3
(3,6,7,1),
(3,11,4,1),
-- Cart 4
(4,3,8,1),
-- Cart 5
(5,5,2,1),
-- Cart 6
(6,8,9,1),
(6,14,3,1),
-- Cart 7
(7,10,5,1),
-- Cart 8
(8,13,10,1),
-- Cart 9 (NORMAL)
(9,16,NULL,1),
-- Cart 10 (PRE_ORDER)
(10,17,NULL,1),
(10,9,NULL,1),
-- Cart 11
(11,19,11,1),
-- Cart 12
(12,20,8,1),
-- Cart 13 (NORMAL)
(13,23,NULL,1),
-- Cart 14 (NORMAL)
(14,6,NULL,1),
-- Cart 15 (PRE_ORDER)
(15,9,NULL,1),
(15,14,NULL,1),
-- Cart 16 (NORMAL)
(16,5,NULL,1),
-- Cart 17 (PRE_ORDER)
(17,7,NULL,1),
-- Cart 18 (PRE_ORDER)
(18,12,NULL,1),
-- Cart 19 (PRE_ORDER)
(19,3,NULL,1),
-- Cart 20 (NORMAL)
(20,15,NULL,1),
-- Cart 21 (PRESCRIPTION)
(21,1,6,1),
-- Cart 22
(22,4,5,1),
-- Cart 23
(23,11,7,1),
-- Cart 24
(24,8,3,1),
-- Cart 25
(25,6,4,1),
-- Cart 26 (NORMAL)
(26,18,NULL,1),
-- Cart 27 (PRE_ORDER)
(27,13,NULL,1),
-- Cart 28 (PRE_ORDER)
(28,20,NULL,1),
-- Cart 29 (NORMAL)
(29,21,NULL,1),
-- Cart 30 (NORMAL)
(30,22,NULL,1);

-- VOUCHERS
INSERT INTO vouchers 
(code, discount_percent, max_discount_amount, start_date, end_date, quantity, is_active)
VALUES
('WELCOME10', 10, 50000, '2026-03-01', '2026-12-31', 100, 1),
('SUMMER15', 15, 80000, '2026-05-01', '2026-07-31', 50, 1),
('FLASH20', 20, 100000, '2026-03-10', '2026-03-12', 30, 1),
('NEWUSER25', 25, 120000, '2026-03-01', '2026-06-01', 200, 1),
('VIP30', 30, 200000, '2026-04-01', '2026-12-31', 20, 1),
('EXPIRED10', 10, 50000, '2025-10-01', '2025-12-31', 100, 0),
('WEEKEND15', 15, 70000, '2026-03-14', '2026-03-15', 40, 1);

-- USER_ADDRESSES
INSERT INTO eyewear_db.user_addresses (user_id, address_line, city, district, is_default) VALUES
-- User 1
(1,'12 Nguyen Trai','Ho Chi Minh City','District 1',1),
(1,'88 Le Thanh Ton','Ho Chi Minh City','District 1',0),
-- User 2
(2,'45 Le Loi','Ho Chi Minh City','District 1',1),
(2,'120 Nguyen Hue','Ho Chi Minh City','District 1',0),
(2,'15 Ton Duc Thang','Ho Chi Minh City','District 1',0),
-- User 3
(3,'78 Tran Hung Dao','Ho Chi Minh City','District 5',1),
-- User 4
(4,'102 Vo Van Tan','Ho Chi Minh City','District 3',1),
(4,'55 Nguyen Dinh Chieu','Ho Chi Minh City','District 3',0),
-- User 5
(5,'56 Cach Mang Thang 8','Ho Chi Minh City','District 10',1),
(5,'230 Ly Thuong Kiet','Ho Chi Minh City','District 10',0),
-- User 6
(6,'210 Nguyen Thi Minh Khai','Ho Chi Minh City','District 3',1),
-- User 7
(7,'15 Phan Xich Long','Ho Chi Minh City','Phu Nhuan',1),
(7,'89 Hoa Lan','Ho Chi Minh City','Phu Nhuan',0),
-- User 8
(8,'88 Cong Hoa','Ho Chi Minh City','Tan Binh',1),
(8,'150 Truong Chinh','Ho Chi Minh City','Tan Binh',0),
(8,'12 Ba Van','Ho Chi Minh City','Tan Binh',0),
-- User 9
(9,'134 Le Van Sy','Ho Chi Minh City','Phu Nhuan',1),
-- User 10
(10,'320 Hoang Van Thu','Ho Chi Minh City','Tan Binh',1),
(10,'42 Nguyen Trong Tuyen','Ho Chi Minh City','Phu Nhuan',0),
-- User 11
(11,'25 Nguyen Chi Thanh','Hanoi','Dong Da',1),
(11,'80 Lang Ha','Hanoi','Dong Da',0),
-- User 12
(12,'90 Chua Boc','Hanoi','Dong Da',1),
-- User 13
(13,'12 Thai Ha','Hanoi','Dong Da',1),
(13,'88 Tay Son','Hanoi','Dong Da',0),
-- User 14
(14,'75 Xuan Thuy','Hanoi','Cau Giay',1),
(14,'22 Tran Thai Tong','Hanoi','Cau Giay',0),
(14,'150 Cau Giay','Hanoi','Cau Giay',0),
-- User 15
(15,'50 Tran Duy Hung','Hanoi','Cau Giay',1),
-- User 16
(16,'102 Nguyen Khanh Toan','Hanoi','Cau Giay',1),
(16,'35 Quan Hoa','Hanoi','Cau Giay',0),
-- User 17
(17,'8 Ba Trieu','Hanoi','Hai Ba Trung',1),
-- User 18
(18,'60 Minh Khai','Hanoi','Hai Ba Trung',1),
(18,'210 Bach Mai','Hanoi','Hai Ba Trung',0),
-- User 19
(19,'11 Kim Ma','Hanoi','Ba Dinh',1),
(19,'70 Giang Vo','Hanoi','Ba Dinh',0),
-- User 20
(20,'45 Dao Tan','Hanoi','Ba Dinh',1),
-- User 21
(21,'10 Bach Dang','Da Nang','Hai Chau',1),
(21,'88 Hung Vuong','Da Nang','Hai Chau',0),
-- User 22
(22,'56 Nguyen Van Linh','Da Nang','Hai Chau',1),
-- User 23
(23,'77 Le Duan','Da Nang','Thanh Khe',1),
(23,'150 Nguyen Tat Thanh','Da Nang','Thanh Khe',0),
-- User 24
(24,'90 Dien Bien Phu','Da Nang','Thanh Khe',1),
-- User 25
(25,'34 Ngo Quyen','Da Nang','Son Tra',1),
(25,'88 Vo Nguyen Giap','Da Nang','Son Tra',0),
-- User 26
(26,'18 Ho Nghinh','Da Nang','Son Tra',1),
-- User 27
(27,'120 Ton Duc Thang','Da Nang','Lien Chieu',1),
(27,'300 Nguyen Luong Bang','Da Nang','Lien Chieu',0),
-- User 28
(28,'55 Nguyen Tat Thanh','Da Nang','Lien Chieu',1),
-- User 29
(29,'200 Pham Van Dong','Da Nang','Son Tra',1),
(29,'75 Vo Van Kiet','Da Nang','Son Tra',0),
-- User 30
(30,'66 Tran Phu','Da Nang','Hai Chau',1),
(30,'15 Hai Phong','Da Nang','Hai Chau',0);

-- SYSTEM_CONFIGS
SELECT * FROM eyewear_db.system_configs;INSERT INTO eyewear_db.system_configs (config_key, config_value, description) VALUES
-- Site
('site.name','VisionCare Store','Tên website bán kính'),
('site.email','support@visioncare.com','Email hỗ trợ khách hàng'),
('site.phone','0909123456','Hotline chăm sóc khách hàng'),
('site.currency','VND','Đơn vị tiền tệ hệ thống'),
-- Shipping
('shipping.base_fee','15000','Phí giao hàng cơ bản'),
('shipping.fee_per_km','5000','Phí giao hàng mỗi km'),
('shipping.min_fee','0','Phí giao hàng tối thiểu'),
('shipping.max_fee','150000','Phí giao hàng tối đa'),
('shipping.free_order_threshold','500000','Đơn hàng tối thiểu để miễn phí vận chuyển'),
('shipping.free_distance_km','3','Khoảng cách miễn phí vận chuyển'),
-- Cart
('cart.max_items','20','Số lượng sản phẩm tối đa trong giỏ hàng'),
-- Voucher
('voucher.enabled','true','Cho phép sử dụng mã giảm giá'),
('voucher.max_per_order','1','Số voucher tối đa mỗi đơn hàng'),
-- Review
('review.enabled','true','Cho phép khách hàng đánh giá sản phẩm'),
('review.min_rating','1','Mức đánh giá tối thiểu'),
-- Order
('order.auto_cancel_hours','24','Tự động hủy đơn nếu chưa thanh toán sau số giờ quy định'),
-- Payment
('payment.cod_enabled','true','Cho phép thanh toán khi nhận hàng'),
('payment.vnpay_enabled','true','Cho phép thanh toán qua VNPay'),
-- Inventory
('inventory.low_stock_threshold','5','Ngưỡng cảnh báo sắp hết hàng'),
-- Return Policy
('return.allowed_days','7','Số ngày cho phép đổi trả sản phẩm'),
-- System
('system.maintenance_mode','false','Bật/tắt chế độ bảo trì hệ thống'),
-- Homepage
('homepage.featured_products_limit','8','Số sản phẩm nổi bật hiển thị trang chủ');
-- BUSINESS_POLICIES
INSERT INTO business_policies (type, title, content, is_active, updated_by) VALUES
('PURCHASE', 'Purchase Policy', 'Khách hàng có thể đặt mua sản phẩm trực tiếp trên website hoặc tại cửa hàng EyeCare Store.
Đối với đơn hàng kính thuốc, khách hàng cần cung cấp thông tin độ cận/chỉ số mắt chính xác hoặc sử dụng dịch vụ đo mắt tại cửa hàng.
Một số sản phẩm yêu cầu đặt cọc trước (30% – 50%), đặc biệt là kính làm theo yêu cầu (custom lens).
Sau khi đặt hàng thành công, hệ thống sẽ gửi xác nhận qua email hoặc số điện thoại.
Đơn hàng chỉ được xử lý khi thông tin khách hàng đầy đủ và hợp lệ.
Thời gian xử lý đơn hàng:
Kính có sẵn: 1 – 2 ngày
Kính theo yêu cầu: 3 – 5 ngày
Khách hàng có trách nhiệm kiểm tra sản phẩm khi nhận hàng.', TRUE, 'system'),
('RETURN', 'Return Policy', 'Khách hàng có thể yêu cầu đổi/trả trong vòng 7 ngày kể từ ngày nhận hàng.
Điều kiện áp dụng:
Sản phẩm lỗi từ nhà sản xuất
Giao sai sản phẩm, sai độ kính
Sản phẩm còn nguyên vẹn, chưa qua sử dụng (đối với đổi ý)
Không áp dụng đổi trả đối với:
Kính làm theo yêu cầu riêng (custom lens)
Sản phẩm bị hư hỏng do người dùng
Quy trình:
Gửi yêu cầu đổi trả qua hệ thống hoặc hotline
Cung cấp hình ảnh/video nếu cần
Xác nhận từ hệ thống → tiến hành đổi/trả
Hoàn tiền:
Trong vòng 5 – 7 ngày làm việc
Qua phương thức thanh toán ban đầu', TRUE, 'system'),
('WARRANTY', 'Warranty Policy', 'Tất cả sản phẩm kính mắt được bảo hành từ 3 – 12 tháng tùy loại.
Phạm vi bảo hành:
Lỗi kỹ thuật từ nhà sản xuất
Bong tróc lớp phủ tròng kính
Lỏng ốc, lệch gọng do sản xuất
Không bảo hành trong các trường hợp:
Rơi vỡ, va đập mạnh
Sử dụng sai cách
Tự ý sửa chữa bên ngoài
Dịch vụ hỗ trợ:
Vệ sinh kính miễn phí
Siết ốc, chỉnh gọng miễn phí trọn đời
Thời gian xử lý bảo hành: 3 – 7 ngày', TRUE, 'system'),
('SHIPPING', 'Shipping Policy', 'EyeCare Store hỗ trợ giao hàng toàn quốc.
Thời gian giao hàng:
Nội thành: 1 – 2 ngày
Tỉnh thành khác: 2 – 5 ngày
Phí vận chuyển:
Miễn phí với đơn hàng từ 500.000đ
Đơn dưới mức này sẽ tính phí theo khu vực
Đối với kính theo yêu cầu:
Thời gian giao hàng sẽ tính sau khi hoàn tất sản xuất
Khách hàng được kiểm tra sản phẩm trước khi thanh toán (nếu hỗ trợ COD).
Trường hợp giao hàng thất bại:
Nhân viên sẽ liên hệ lại tối đa 2 lần
Sau đó đơn hàng có thể bị hủy', TRUE, 'system'),
('PRIVACY', 'Privacy Policy', 'EyeCare Store cam kết bảo mật tuyệt đối thông tin cá nhân của khách hàng.
Thông tin thu thập bao gồm:
Họ tên, số điện thoại, email
Địa chỉ giao hàng
Thông tin đơn hàng
Mục đích sử dụng:   
Xử lý đơn hàng
Hỗ trợ khách hàng
Cải thiện dịch vụ
Cam kết:
Không chia sẻ thông tin cho bên thứ ba nếu không có sự đồng ý
Chỉ sử dụng cho mục đích nội bộ
Dữ liệu được lưu trữ và bảo vệ bằng các biện pháp bảo mật phù hợp.
Khách hàng có quyền yêu cầu chỉnh sửa hoặc xóa thông tin cá nhân bất kỳ lúc nào.', TRUE, 'system');

--  ORDER 
INSERT INTO orders
(user_id,status,order_type,total_amount,shipping_fee,discount_amount,
shipping_address,phone_number,receiver_name,note,created_at,updated_at)
VALUES
(1,'COMPLETED','PRESCRIPTION',3250000,25000,50000,'12 Nguyen Trai, District 1','0903847291','Nguyễn Minh Anh','Please call before delivery','2026-03-01 10:00:00',NULL),
(2,'COMPLETED','PRESCRIPTION',2600000,30000,0,'45 Le Loi, District 1','0916728453','Trần Thị Kim Ngân',NULL,'2026-03-01 11:00:00',NULL),
(3,'SHIPPING','PRESCRIPTION',1855000,35000,0,'78 Tran Hung Dao, District 5','0935281746','Lê Văn Thành','Deliver after 6 PM','2026-03-02 09:00:00',NULL),
(4,'SHIPPING','PRESCRIPTION',2160000,40000,100000,'102 Vo Van Tan, District 3','0983175624','Phạm Thu Thảo',NULL,'2026-03-02 11:30:00',NULL),
(5,'MANUFACTURING','PRESCRIPTION',2005000,28000,0,'56 Cach Mang Thang 8, District 10','0972468135','Hoàng Minh Đức',NULL,'2026-03-02 14:20:00',NULL),
(6,'MANUFACTURING','PRESCRIPTION',2355000,32000,0,'210 Nguyen Thi Minh Khai, District 3','0964827153','Đặng Ngọc Anh','Leave at reception desk','2026-03-03 08:00:00',NULL),
(7,'MANUFACTURING','PRESCRIPTION',2860000,38000,150000,'15 Phan Xich Long, Phu Nhuan','0395728461','Bùi Trung Kiên',NULL,'2026-03-03 10:15:00',NULL),
(8,'SHIPPING','PRESCRIPTION',1805000,26000,0,'88 Cong Hoa, Tan Binh','0384617295','Võ Thị My',NULL,'2026-03-03 13:20:00',NULL),
(9,'COMPLETED','NORMAL',1650000,22000,50000,'134 Le Van Sy, Phu Nhuan','0372846159','Đinh Quang Vinh',NULL,'2026-03-03 16:10:00',NULL),
(10,'COMPLETED','PRE_ORDER',3560000,45000,80000,'320 Hoang Van Thu, Tan Binh','0365172948','Phan Thành Phong','Call when arriving','2026-03-04 09:00:00',NULL),
(11,'MANUFACTURING','PRESCRIPTION',5260000,60000,150000,'25 Nguyen Chi Thanh, Hanoi','0907153842','Nguyễn Thành Đạt',NULL,'2026-03-04 10:30:00',NULL),
(12,'MANUFACTURING','PRESCRIPTION',2055000,55000,0,'90 Chua Boc, Hanoi','0912847563','Trần Minh Quân',NULL,'2026-03-04 11:50:00',NULL),
(13,'SHIPPING','NORMAL',4860000,65000,100000,'12 Thai Ha, Hanoi','0926715834','Lê Văn Thiện',NULL,'2026-03-04 13:00:00',NULL),
(14,'SHIPPING','NORMAL',1555000,52000,0,'75 Xuan Thuy, Hanoi','0934172685','Phạm Đức Anh',NULL,'2026-03-04 14:30:00',NULL),
(15,'COMPLETED','PRE_ORDER',4260000,70000,80000,'50 Tran Duy Hung, Hanoi','0965712846','Hoàng Ngọc Bảo',NULL,'2026-03-04 16:10:00',NULL),
(16,'COMPLETED','NORMAL',1955000,58000,50000,'102 Nguyen Khanh Toan, Hanoi','0973816524','Nguyễn Thị Lan','Ring the doorbell twice','2026-03-05 09:10:00',NULL),
(17,'MANUFACTURING','PRE_ORDER',1755000,54000,0,'8 Ba Trieu, Hanoi','0986241753','Trần Thùy Trang',NULL,'2026-03-05 10:30:00',NULL),
(18,'SHIPPING','PRE_ORDER',3160000,62000,70000,'60 Minh Khai, Hanoi','0391847265','Phạm Minh Tuấn',NULL,'2026-03-05 12:40:00',NULL),
(19,'WAITING_FOR_STOCK','PRE_ORDER',2655000,50000,0,'11 Kim Ma, Hanoi','0385271946','Lê Văn Dũng',NULL,'2026-03-05 14:00:00',NULL),
(20,'PENDING_PAYMENT','NORMAL',3960000,57000,100000,'45 Dao Tan, Hanoi','0376928451','Đặng Thùy Anh',NULL,'2026-03-05 16:00:00',NULL),
(21,'PENDING_VERIFICATION','PRESCRIPTION',2260000,48000,0,'10 Bach Dang, Da Nang','0368452173','Võ Minh Châu',NULL,'2026-03-06 09:10:00',NULL),
(22,'PENDING_VERIFICATION','PRESCRIPTION',1455000,46000,50000,'56 Nguyen Van Linh, Da Nang','0905274816','Bùi Trà Ngân','Please pack carefully','2026-03-06 10:20:00',NULL),
(23,'PENDING_VERIFICATION','PRESCRIPTION',2090000,52000,0,'77 Le Duan, Da Nang','0916384725','Đinh Thành Sơn',NULL,'2026-03-06 11:30:00',NULL),
(24,'PENDING_VERIFICATION','PRESCRIPTION',1650000,47000,0,'90 Dien Bien Phu, Da Nang','0937481625','Hoàng Kim Yến',NULL,'2026-03-06 13:10:00',NULL),
(25,'MANUFACTURING','PRESCRIPTION',1955000,49000,50000,'34 Ngo Quyen, Da Nang','0964728513','Phạm Quỳnh Chi',NULL,'2026-03-07 09:00:00',NULL),
(26,'SHIPPING','NORMAL',2655000,53000,0,'18 Ho Nghinh, Da Nang','0975263841','Trần Văn Lộc',NULL,'2026-03-07 10:20:00',NULL),
(27,'SHIPPING','PRE_ORDER',1755000,51000,80000,'120 Ton Duc Thang, Da Nang','0987341625','Nguyễn Kim Liên',NULL,'2026-03-07 11:40:00',NULL),
(28,'WAITING_FOR_STOCK','PRE_ORDER',3560000,56000,0,'55 Nguyen Tat Thanh, Da Nang','0396284715','Lê Văn Phúc',NULL,'2026-03-07 13:00:00',NULL),
(29,'COMPLETED','NORMAL',2055000,50000,50000,'200 Pham Van Dong, Da Nang','0384716259','Đặng Ngọc Mai',NULL,'2026-03-07 15:20:00',NULL),
(30,'PENDING_PAYMENT','NORMAL',2290000,54000,0,'66 Tran Phu, Da Nang','0375148263','Võ Thanh Hà','Customer will pay on delivery','2026-03-07 17:10:00',NULL);

-- PRESCRIPTIONS
INSERT INTO prescriptions (order_id,prescription_image_url,sphere_od,sphere_os,cylinder_od,cylinder_os,axis_od,axis_os,pd,verified_by,verified_at,staff_note) VALUES
(1,'prescriptions/p1.jpg',-1.25,-1.50,-0.50,-0.75,90,85,62,4,'2026-03-08 09:00:00','Độ cận ổn định'),
(2,'prescriptions/p2.jpg',-2.00,-2.25,-0.75,-0.50,80,95,63,4,'2026-03-08 09:05:00',NULL),
(3,'prescriptions/p3.jpg',-0.75,-1.00,0.00,-0.25,0,70,61,4,'2026-03-08 09:10:00',NULL),
(4,'prescriptions/p4.jpg',-3.25,-3.00,-1.00,-0.75,95,90,64,4,'2026-03-08 09:15:00',NULL),
(5,'prescriptions/p5.jpg',-1.50,-1.75,-0.50,-0.50,85,88,62,4,'2026-03-08 09:20:00','Có loạn nhẹ'),
(6,'prescriptions/p6.jpg',-2.75,-2.50,-1.25,-1.00,92,87,63,4,'2026-03-08 09:25:00',NULL),
(7,'prescriptions/p7.jpg',-0.50,-0.75,0.00,-0.25,0,75,61,4,'2026-03-08 09:30:00',NULL),
(8,'prescriptions/p8.jpg',-4.00,-3.75,-1.50,-1.25,100,95,64,4,'2026-03-08 09:35:00',NULL),
(9,'prescriptions/p9.jpg',-1.00,-1.25,-0.25,-0.50,80,85,62,4,'2026-03-08 09:40:00',NULL),
(10,'prescriptions/p10.jpg',-2.25,-2.00,-0.75,-0.75,90,90,63,4,'2026-03-08 09:45:00',NULL),
(11,'prescriptions/p11.jpg',-3.50,-3.75,-1.00,-1.25,85,92,65,4,'2026-03-08 09:50:00','Khuyến nghị tròng chiết suất cao'),
(12,'prescriptions/p12.jpg',-0.75,-0.75,0.00,0.00,0,0,60,4,'2026-03-08 09:55:00',NULL),
(13,'prescriptions/p13.jpg',-1.25,-1.00,-0.50,-0.50,95,100,62,4,'2026-03-08 10:00:00',NULL),
(14,'prescriptions/p14.jpg',-2.50,-2.75,-0.75,-1.00,88,90,64,4,'2026-03-08 10:05:00',NULL),
(15,'prescriptions/p15.jpg',-1.75,-2.00,-0.50,-0.75,80,85,63,4,'2026-03-08 10:10:00','PD hơi lệch nhẹ'),
(16,'prescriptions/p16.jpg',-3.00,-3.25,-1.00,-1.00,90,95,64,4,'2026-03-08 10:15:00',NULL),
(17,'prescriptions/p17.jpg',-0.25,-0.50,0.00,-0.25,0,80,60,4,'2026-03-08 10:20:00',NULL),
(18,'prescriptions/p18.jpg',-4.25,-4.00,-1.75,-1.50,100,105,65,4,'2026-03-08 10:25:00',NULL),
(19,'prescriptions/p19.jpg',-1.50,-1.25,-0.50,-0.50,90,92,62,4,'2026-03-08 10:30:00','Khách dùng máy tính nhiều'),
(20,'prescriptions/p20.jpg',-2.00,-2.25,-0.75,-0.75,85,88,63,4,'2026-03-08 10:35:00',NULL),
(21,'prescriptions/p21.jpg',-3.75,-3.50,-1.25,-1.00,95,90,64,NULL,NULL,NULL),
(22,'prescriptions/p22.jpg',-1.00,-0.75,0.00,-0.25,0,70,61,NULL,NULL,NULL),
(23,'prescriptions/p23.jpg',-1.25,-1.50,-0.50,-0.75,88,90,62,NULL,NULL,NULL),
(24,'prescriptions/p24.jpg',-2.75,-3.00,-1.00,-1.00,92,95,64,NULL,NULL,NULL),
(25,'prescriptions/p25.jpg',-1.00,-1.25,-0.25,-0.50,80,85,62,4,'2026-03-08 10:40:00',NULL),
(26,'prescriptions/p26.jpg',-3.25,-3.50,-1.25,-1.00,95,90,65,4,'2026-03-08 10:45:00',NULL),
(27,'prescriptions/p27.jpg',-0.75,-0.50,0.00,-0.25,0,75,61,4,'2026-03-08 10:50:00',NULL),
(28,'prescriptions/p28.jpg',-4.50,-4.25,-1.75,-1.50,105,100,66,4,'2026-03-08 10:55:00',NULL),
(29,'prescriptions/p29.jpg',-1.50,-1.75,-0.50,-0.75,90,92,63,4,'2026-03-08 11:00:00',NULL),
(30,'prescriptions/p30.jpg',-2.25,-2.50,-0.75,-1.00,85,88,64,4,'2026-03-08 11:05:00',NULL);

-- ORDER_ITEMS
INSERT INTO order_items
(order_id, product_variant_id, lens_product_id, quantity, price)
VALUES
(1,2,1,1,3760000),
(2,8,1,1,3055000),
(3,11,1,1,2355000),
(4,17,1,1,2660000),
(5,20,1,1,2505000),
(6,23,1,1,2855000),
(7,26,1,1,3360000),
(8,32,1,1,2305000),
(9,35,NULL,1,1650000),
(10,38,NULL,1,3560000),
(11,41,4,1,7760000),
(12,44,1,1,2555000),
(13,47,NULL,1,4860000),
(14,50,NULL,1,1555000),
(15,53,NULL,1,4260000),
(16,56,NULL,1,1955000),
(17,59,NULL,1,1755000),
(18,62,NULL,1,3160000),
(19,65,NULL,1,2655000),
(20,68,NULL,1,3960000),
(21,71,1,1,2760000),
(22,74,1,1,1955000),
(23,77,1,1,2590000),
(24,80,1,1,2150000),
(25,20,1,1,2505000),
(26,65,NULL,1,2655000),
(27,35,NULL,1,1650000),
(28,38,NULL,1,3560000),
(29,44,NULL,1,2055000),
(30,71,NULL,1,2290000);

-- PAYMENTS
INSERT INTO payments (id, order_id, method, status, amount, transaction_code, payment_proof_img, paid_at) VALUES
(1, 1, 'MOMO', 'PAID', 3250000.00, 'TXN0001', 'payments/p1.jpg', '2026-03-01 10:15:00'),
(2, 2, 'BANK_TRANSFER', 'PAID', 2600000.00, 'TXN0002', 'payments/p2.jpg', '2026-03-01 11:20:00'),
(3, 3, 'MOMO', 'PAID', 1855000.00, 'TXN0003', 'payments/p3.jpg', '2026-03-02 09:20:00'),
(4, 4, 'BANK_TRANSFER', 'PAID', 2160000.00, 'TXN0004', 'payments/p4.jpg', '2026-03-02 11:50:00'),
(5, 5, 'MOMO', 'PAID', 2005000.00, 'TXN0005', 'payments/p5.jpg', '2026-03-02 14:40:00'),
(6, 6, 'BANK_TRANSFER', 'PAID', 2355000.00, 'TXN0006', 'payments/p6.jpg', '2026-03-03 08:20:00'),
(7, 7, 'MOMO', 'PAID', 2860000.00, 'TXN0007', 'payments/p7.jpg', '2026-03-03 10:40:00'),
(8, 8, 'BANK_TRANSFER', 'PAID', 1805000.00, 'TXN0008', 'payments/p8.jpg', '2026-03-03 13:40:00'),
(9, 9, 'MOMO', 'PAID', 1650000.00, 'TXN0009', 'payments/p9.jpg', '2026-03-03 16:30:00'),
(10, 10, 'BANK_TRANSFER', 'PAID', 3560000.00, 'TXN0010', 'payments/p10.jpg', '2026-03-04 09:20:00'),
(11, 11, 'MOMO', 'PAID', 5260000.00, 'TXN0011', 'payments/p11.jpg', '2026-03-04 10:50:00'),
(12, 12, 'BANK_TRANSFER', 'PAID', 2055000.00, 'TXN0012', 'payments/p12.jpg', '2026-03-04 12:10:00'),
(13, 13, 'MOMO', 'PAID', 4860000.00, 'TXN0013', 'payments/p13.jpg', '2026-03-04 13:20:00'),
(14, 14, 'BANK_TRANSFER', 'PAID', 1555000.00, 'TXN0014', 'payments/p14.jpg', '2026-03-04 14:50:00'),
(15, 15, 'MOMO', 'PAID', 4260000.00, 'TXN0015', 'payments/p15.jpg', '2026-03-04 16:30:00'),
(16, 16, 'BANK_TRANSFER', 'PAID', 1955000.00, 'TXN0016', 'payments/p16.jpg', '2026-03-05 09:30:00'),
(17, 17, 'MOMO', 'PAID', 1755000.00, 'TXN0017', 'payments/p17.jpg', '2026-03-05 10:50:00'),
(18, 18, 'BANK_TRANSFER', 'PAID', 3160000.00, 'TXN0018', 'payments/p18.jpg', '2026-03-05 13:00:00'),
(19, 19, 'MOMO', 'PAID', 2655000.00, 'TXN0019', 'payments/p19.jpg', '2026-03-05 14:20:00'),
(20, 20, 'COD', 'UNPAID', 3960000.00, NULL, NULL, NULL),
(21, 21, 'COD', 'UNPAID', 2260000.00, NULL, NULL, NULL),
(22, 22, 'COD', 'UNPAID', 1455000.00, NULL, NULL, NULL),
(23, 23, 'COD', 'UNPAID', 2090000.00, NULL, NULL, NULL),
(24, 24, 'COD', 'UNPAID', 1650000.00, NULL, NULL, NULL),
(25, 25, 'MOMO', 'PAID', 1955000.00, 'TXN0025', 'payments/p25.jpg', '2026-03-07 09:20:00'),
(26, 26, 'BANK_TRANSFER', 'PAID', 2655000.00, 'TXN0026', 'payments/p26.jpg', '2026-03-07 10:40:00'),
(27, 27, 'MOMO', 'PAID', 1755000.00, 'TXN0027', 'payments/p27.jpg', '2026-03-07 12:00:00'),
(28, 28, 'BANK_TRANSFER', 'PAID', 3560000.00, 'TXN0028', 'payments/p28.jpg', '2026-03-07 13:20:00'),
(29, 29, 'MOMO', 'PAID', 2055000.00, 'TXN0029', 'payments/p29.jpg', '2026-03-07 15:40:00'),
(30, 30, 'COD', 'UNPAID', 2290000.00, NULL, NULL, NULL);
-- 1. CART_ITEMS
