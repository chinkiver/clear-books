-- 更新 system_settings 表结构，确保 setting_value 字段能够存储大文本（Base64 图片）
-- 执行此脚本修复数据库字段类型问题

-- 检查当前表结构
DESCRIBE system_settings;

-- 修改 setting_value 字段为 LONGTEXT 以支持更大的 Base64 数据
ALTER TABLE system_settings 
MODIFY COLUMN setting_value LONGTEXT;

-- 验证修改后的表结构
DESCRIBE system_settings;

-- 查看当前存储的设置（用于调试）
SELECT setting_key, LEFT(setting_value, 100) as value_preview, LENGTH(setting_value) as value_length 
FROM system_settings;
