CREATE TABLE IF NOT EXISTS return_image (
    id BIGSERIAL PRIMARY KEY,
    return_no BIGINT NOT NULL,
    file_id BIGINT NOT NULL UNIQUE,
    image_url VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_return_image_return
        FOREIGN KEY (return_no) REFERENCES "return"(return_no) ON DELETE CASCADE,
    CONSTRAINT fk_return_image_file
        FOREIGN KEY (file_id) REFERENCES uploaded_file(file_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_return_image_return_no
    ON return_image (return_no);
