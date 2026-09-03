INSERT INTO data_masking_policies (role_name, table_name, column_name, mask_type)
SELECT 'EDITOR', table_name, column_name, mask_type
FROM data_masking_policies
WHERE role_name = 'READ_ONLY';
