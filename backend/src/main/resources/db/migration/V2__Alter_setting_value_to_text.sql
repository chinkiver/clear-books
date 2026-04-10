-- 修改 system_settings 表的 setting_value 字段为 MEDIUMTEXT 类型，以支持存储 Base64 编码的图片
-- TEXT (64KB) 不够存储大图片，MEDIUMTEXT 支持 16MB
ALTER TABLE system_settings MODIFY COLUMN setting_value MEDIUMTEXT;
