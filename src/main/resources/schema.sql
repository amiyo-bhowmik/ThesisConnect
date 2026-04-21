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
