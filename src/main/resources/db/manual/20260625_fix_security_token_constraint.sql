-- Refresh old Hibernate-generated CHECK constraint so it accepts the
-- PASSWORD_RESET_CONFIRMED token type used after a reset code is verified.
ALTER TABLE user_security_tokens DROP CONSTRAINT IF EXISTS user_security_tokens_type_check;

ALTER TABLE user_security_tokens
    ADD CONSTRAINT user_security_tokens_type_check
    CHECK (
        type IN (
            'PASSWORD_RESET',
            'PASSWORD_RESET_CONFIRMED',
            'EMAIL_VERIFICATION',
            'EMAIL_2FA',
            'OAUTH_2FA_CHALLENGE'
        )
    );
