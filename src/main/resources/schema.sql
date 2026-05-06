CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    department VARCHAR(120),
    university VARCHAR(150),
    academic_details VARCHAR(500),
    bio VARCHAR(800),
    profile_picture VARCHAR(255),
    is_looking_for_group BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_research_interests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    interest VARCHAR(80) NOT NULL,
    CONSTRAINT fk_user_research_interests_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skill VARCHAR(80) NOT NULL,
    CONSTRAINT fk_user_skills_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS thesis_groups (
    group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic VARCHAR(160) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    admin_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_thesis_groups_admin
        FOREIGN KEY (admin_user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_members (
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_group_members_group
        FOREIGN KEY (group_id) REFERENCES thesis_groups(group_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS join_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_user_id BIGINT NOT NULL,
    recipient_user_id BIGINT,
    group_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    reviewed_by_user_id BIGINT,
    CONSTRAINT fk_join_requests_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_group
        FOREIGN KEY (group_id) REFERENCES thesis_groups(group_id) ON DELETE CASCADE,
    CONSTRAINT fk_join_requests_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(400) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS documents (
    document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    uploaded_by_user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    upload_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 1,
    file_size BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_documents_uploaded_by
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_group
        FOREIGN KEY (group_id) REFERENCES thesis_groups(group_id) ON DELETE CASCADE,
    CONSTRAINT chk_documents_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

CREATE TABLE IF NOT EXISTS document_versions (
    version_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    uploaded_by_user_id BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE,
    CONSTRAINT fk_document_versions_uploaded_by
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uk_document_versions_number
        UNIQUE (document_id, version_number)
);

CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(1200) NOT NULL,
    author_user_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_document
        FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS direct_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    content VARCHAR(1200) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_direct_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_direct_messages_receiver
        FOREIGN KEY (receiver_user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    content VARCHAR(1200) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_messages_group
        FOREIGN KEY (group_id) REFERENCES thesis_groups(group_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS direct_message_pins (
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    pinned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, message_id),
    CONSTRAINT fk_direct_message_pins_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_direct_message_pins_message
        FOREIGN KEY (message_id) REFERENCES direct_messages(message_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_message_pins (
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    pinned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, message_id),
    CONSTRAINT fk_group_message_pins_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_message_pins_message
        FOREIGN KEY (message_id) REFERENCES group_messages(message_id) ON DELETE CASCADE
);
