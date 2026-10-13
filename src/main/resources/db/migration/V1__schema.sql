-- Placement Prep Tracker schema (MySQL 8; also runs on H2 in MySQL mode for dev and tests)

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(80)  NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    daily_goal    INT          NOT NULL DEFAULT 3,
    created_at    TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_goal CHECK (daily_goal BETWEEN 1 AND 50)
);

CREATE TABLE applications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    company      VARCHAR(100)  NOT NULL,
    job_title    VARCHAR(100)  NOT NULL,
    location     VARCHAR(80),
    source       VARCHAR(20)   NOT NULL,
    stage        VARCHAR(20)   NOT NULL,
    package_lpa  DECIMAL(5, 2),
    job_url      VARCHAR(500),
    applied_on   DATE,
    next_step    VARCHAR(120),
    next_step_on DATE,
    notes        VARCHAR(1000),
    created_at   TIMESTAMP(6)  NOT NULL,
    updated_at   TIMESTAMP(6)  NOT NULL,
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_applications_package CHECK (package_lpa IS NULL OR package_lpa >= 0)
);
CREATE INDEX idx_applications_user_stage ON applications (user_id, stage);
CREATE INDEX idx_applications_next_step ON applications (user_id, next_step_on);

CREATE TABLE stage_changes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT       NOT NULL,
    from_stage     VARCHAR(20),
    to_stage       VARCHAR(20)  NOT NULL,
    changed_at     TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_stage_changes_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE
);
CREATE INDEX idx_stage_changes_application ON stage_changes (application_id, changed_at);

CREATE TABLE problems (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    title          VARCHAR(150)  NOT NULL,
    url            VARCHAR(500),
    platform       VARCHAR(20)   NOT NULL,
    difficulty     VARCHAR(10)   NOT NULL,
    topic          VARCHAR(20)   NOT NULL,
    notes          VARCHAR(1000),
    solved_on      DATE          NOT NULL,
    repetitions    INT           NOT NULL DEFAULT 0,
    interval_days  INT           NOT NULL DEFAULT 0,
    ease_x100      INT           NOT NULL DEFAULT 250,
    next_review_on DATE          NOT NULL,
    last_quality   INT,
    created_at     TIMESTAMP(6)  NOT NULL,
    CONSTRAINT fk_problems_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_problems_ease CHECK (ease_x100 >= 130)
);
CREATE INDEX idx_problems_due ON problems (user_id, next_review_on);
CREATE INDEX idx_problems_solved ON problems (user_id, solved_on);

CREATE TABLE reviews (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id     BIGINT       NOT NULL,
    quality        INT          NOT NULL,
    reviewed_on    DATE         NOT NULL,
    interval_after INT          NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_reviews_problem FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE,
    CONSTRAINT ck_reviews_quality CHECK (quality BETWEEN 0 AND 5)
);
CREATE INDEX idx_reviews_problem_day ON reviews (problem_id, reviewed_on);
