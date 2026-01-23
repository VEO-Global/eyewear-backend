-- Xóa DB cũ nếu có để làm lại cho sạch
DROP DATABASE IF EXISTS eyewear_db;
CREATE DATABASE eyewear_db;
USE eyewear_db;

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

-- Bảng Sổ địa chỉ (User Address Book)
CREATE TABLE user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_line TEXT NOT NULL, -- "123 Đường ABC, Phường X"
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE, -- Địa chỉ mặc định
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================
-- 2. PRODUCT CATALOG (SẢN PHẨM)
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
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    is_thumbnail BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(50) UNIQUE, -- Mã kho
    color VARCHAR(50),
    size VARCHAR(20),
    price DECIMAL(15, 2),   -- Giá riêng (nếu có)
    stock_quantity INT DEFAULT 0,
    expected_restock_date DATETIME, -- Dùng cho PRE-ORDER
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Tròng kính (Giả định lắp vừa mọi gọng)
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
    
    -- Ảnh toa thuốc khách up (Bắt buộc)
    prescription_image_url TEXT NOT NULL,
    
    -- Kết quả xác thực (Nhập tay bởi Sale)
    sphere_od DECIMAL(5, 2), -- Mắt Phải
    sphere_os DECIMAL(5, 2), -- Mắt Trái
    cylinder_od DECIMAL(5, 2),
    cylinder_os DECIMAL(5, 2),
    axis_od INT,
    axis_os INT,
    pd DECIMAL(5, 2),
    
    verified_by BIGINT, -- Sale nào duyệt?
    verified_at DATETIME,
    staff_note TEXT,
    
    FOREIGN KEY (order_id) REFERENCES orders(id),
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

-- DATA MẪU CƠ BẢN
INSERT INTO roles (name) VALUES ('CUSTOMER'), ('SALES'), ('OPERATIONS'), ('MANAGER'), ('ADMIN');
INSERT INTO categories (name) VALUES ('Kính râm'), ('Gọng kính'), ('Kính trẻ em');


