-- =====================================================
-- Interview TTS 数据库初始化脚本
-- =====================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_key` VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '密码哈希',
    `user_type` VARCHAR(20) DEFAULT 'USER' COMMENT '用户类型: USER/VIP',
    `vip_expire_date` DATETIME DEFAULT NULL COMMENT 'VIP过期时间',
    `monthly_char_limit` INT DEFAULT 5000 COMMENT '当月字数限额',
    `monthly_char_used` INT DEFAULT 0 COMMENT '当月已用字数',
    `last_char_reset_date` DATE DEFAULT NULL COMMENT '上次字数重置日期',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '状态: NORMAL/BANNED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_key` (`user_key`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文本记录表
CREATE TABLE IF NOT EXISTS `text_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `raw_text` TEXT NOT NULL COMMENT '原始文本',
    `ssml_text` TEXT DEFAULT NULL COMMENT 'SSML处理后文本',
    `voice_name` VARCHAR(100) NOT NULL COMMENT '音色名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文本记录表';

-- 音频文件表
CREATE TABLE IF NOT EXISTS `audio_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `text_record_id` BIGINT NOT NULL COMMENT '文本记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '音频名称',
    `r2_url` VARCHAR(255) NOT NULL COMMENT '音频URL',
    `temp_audio_url` VARCHAR(500) DEFAULT NULL COMMENT '火山引擎临时音频URL',
    `download_failed` TINYINT(1) DEFAULT NULL COMMENT '下载是否失败标记',
    `duration` INT DEFAULT 0 COMMENT '时长(秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_text_record_id` (`text_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频文件表';

-- 用户收藏表
CREATE TABLE IF NOT EXISTS `user_collect` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `audio_file_id` BIGINT NOT NULL COMMENT '音频文件ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_audio` (`user_id`, `audio_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 音频缓存表
CREATE TABLE IF NOT EXISTS `audio_cache` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `raw_text_md5` VARCHAR(32) NOT NULL COMMENT '原文MD5',
    `voice_name` VARCHAR(100) NOT NULL COMMENT '音色名称',
    `prompt_hash` VARCHAR(32) NOT NULL COMMENT '提示词哈希',
    `r2_url` VARCHAR(255) NOT NULL COMMENT '音频URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_md5_voice_prompt` (`raw_text_md5`, `voice_name`, `prompt_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频缓存表';

-- 播客表
CREATE TABLE IF NOT EXISTS `podcast` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `source_text` TEXT DEFAULT NULL COMMENT '原文内容',
    `script_content` TEXT DEFAULT NULL COMMENT '脚本内容',
    `voice_a` VARCHAR(50) DEFAULT NULL COMMENT '主播A音色',
    `voice_b` VARCHAR(50) DEFAULT NULL COMMENT '主播B音色',
    `audio_url` VARCHAR(255) DEFAULT NULL COMMENT '音频URL',
    `duration` INT DEFAULT 0 COMMENT '时长(秒)',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播客表';

-- 克隆声音表
CREATE TABLE IF NOT EXISTS `cloned_voice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '声音名称',
    `voice_id` VARCHAR(100) DEFAULT NULL COMMENT '克隆音色ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `sample_count` INT DEFAULT 0 COMMENT '样本数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='克隆声音表';

-- 声音样本表
CREATE TABLE IF NOT EXISTS `voice_sample` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `cloned_voice_id` BIGINT NOT NULL COMMENT '克隆声音ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `audio_url` VARCHAR(255) NOT NULL COMMENT '音频URL',
    `duration` INT DEFAULT 0 COMMENT '时长(秒)',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_cloned_voice_id` (`cloned_voice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='声音样本表';

-- 翻译表
CREATE TABLE IF NOT EXISTS `translation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `source_text` TEXT DEFAULT NULL COMMENT '源文本',
    `translated_text` TEXT DEFAULT NULL COMMENT '翻译文本',
    `source_lang` VARCHAR(10) DEFAULT 'zh' COMMENT '源语言',
    `target_lang` VARCHAR(10) DEFAULT 'en' COMMENT '目标语言',
    `source_audio_url` VARCHAR(255) DEFAULT NULL COMMENT '源音频URL',
    `translated_audio_url` VARCHAR(255) DEFAULT NULL COMMENT '翻译音频URL',
    `translated_audio_duration` INT DEFAULT 0 COMMENT '翻译音频时长',
    `translation_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '翻译类型: TEXT/SPEECH',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译表';

-- 会话表
CREATE TABLE IF NOT EXISTS `session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_type` VARCHAR(50) DEFAULT NULL COMMENT '会话类型',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题',
    `context_text` TEXT DEFAULT NULL COMMENT '上下文文本',
    `audio_url` VARCHAR(255) DEFAULT NULL COMMENT '音频URL',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: USER/ASSISTANT/SYSTEM',
    `content` TEXT DEFAULT NULL COMMENT '消息内容',
    `audio_url` VARCHAR(255) DEFAULT NULL COMMENT '音频URL',
    `audio_duration` INT DEFAULT 0 COMMENT '音频时长',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 播放列表表
CREATE TABLE IF NOT EXISTS `playlist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '列表名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面URL',
    `audio_count` INT DEFAULT 0 COMMENT '音频数量',
    `total_duration` INT DEFAULT 0 COMMENT '总时长(秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表表';

-- 播放列表音频关联表
CREATE TABLE IF NOT EXISTS `playlist_audio` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `playlist_id` BIGINT NOT NULL COMMENT '播放列表ID',
    `audio_file_id` BIGINT NOT NULL COMMENT '音频文件ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `add_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_playlist_audio` (`playlist_id`, `audio_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表音频关联表';

-- 短信验证码表
CREATE TABLE IF NOT EXISTS `sms_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `type` VARCHAR(20) DEFAULT 'LOGIN' COMMENT '验证码类型: LOGIN/REGISTER',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码表';

-- VIP订单表
CREATE TABLE IF NOT EXISTS `vip_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `plan_type` VARCHAR(20) NOT NULL COMMENT '套餐类型: MONTHLY/YEARLY/LIFETIME',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/CANCELLED',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP订单表';
