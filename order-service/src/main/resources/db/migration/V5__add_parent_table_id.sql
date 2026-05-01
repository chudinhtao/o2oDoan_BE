ALTER TABLE orders.tables ADD COLUMN parent_table_id UUID;
ALTER TABLE orders.tables ADD CONSTRAINT fk_parent_table FOREIGN KEY (parent_table_id) REFERENCES orders.tables(id);
