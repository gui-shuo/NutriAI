-- Add userId column to nutritionists table to link with user accounts
ALTER TABLE nutritionists ADD COLUMN user_id BIGINT NULL UNIQUE;
ALTER TABLE nutritionists ADD INDEX idx_nutritionist_user_id (user_id);

-- Expand role enum to include NUTRITIONIST
ALTER TABLE users MODIFY COLUMN role ENUM('USER','ADMIN','SUPER_ADMIN','NUTRITIONIST') NOT NULL DEFAULT 'USER';
