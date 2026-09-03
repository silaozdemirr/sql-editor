CREATE TABLE data_masking_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    mask_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO data_masking_policies (role_name, table_name, column_name, mask_type) 
VALUES ('READ_ONLY', '*', 'tc_kimlik', 'LAST_4');

INSERT INTO data_masking_policies (role_name, table_name, column_name, mask_type) 
VALUES ('READ_ONLY', '*', 'password', 'FULL');
