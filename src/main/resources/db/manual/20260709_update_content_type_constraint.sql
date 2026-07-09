-- Script cập nhật check constraint cho bảng content_items
-- Cho phép thêm kiểu dữ liệu EMAIL_TEMPLATE mới của Java Enum ContentType vào DB

ALTER TABLE content_items DROP CONSTRAINT IF EXISTS content_items_type_check;

ALTER TABLE content_items ADD CONSTRAINT content_items_type_check CHECK (type IN ('BLOG', 'STATIC_PAGE', 'BANNER', 'SITE_SECTION', 'EMAIL_TEMPLATE'));
