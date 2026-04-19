ALTER TABLE xlinks_router.customer_tokens ADD daily_quota decimal(12,2) NULL COMMENT 'Daily usage quota, NULL means unlimited';
ALTER TABLE xlinks_router.customer_tokens CHANGE daily_quota daily_quota decimal(12,2) NULL COMMENT 'Daily usage quota, NULL means unlimited' AFTER allowed_models;
ALTER TABLE xlinks_router.customer_tokens ADD used_quota decimal(12,2) NULL;
ALTER TABLE xlinks_router.customer_tokens CHANGE used_quota used_quota decimal(12,2) NULL AFTER daily_quota;
ALTER TABLE xlinks_router.customer_tokens ADD total_quota decimal(12,2) NULL;
ALTER TABLE xlinks_router.customer_tokens CHANGE total_quota total_quota decimal(12,2) NULL AFTER used_quota;
ALTER TABLE xlinks_router.customer_tokens ADD total_used_quota decimal(12,2) NULL;
ALTER TABLE xlinks_router.customer_tokens CHANGE total_used_quota total_used_quota decimal(12,2) NULL AFTER total_quota;
