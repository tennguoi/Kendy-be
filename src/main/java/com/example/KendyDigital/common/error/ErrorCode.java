package com.example.KendyDigital.common.error;

public enum ErrorCode {
    // Common
    UNEXPECTED("error.unexpected"),
    INTERNAL("error.internal"),
    NOT_FOUND("error.notFound"),
    FORBIDDEN("error.forbidden"),
    UNAUTHORIZED("error.unauthorized"),
    BAD_REQUEST("error.badRequest"),
    CONFLICT("error.conflict"),
    TOO_MANY_REQUESTS("error.tooManyRequests"),
    MAINTENANCE("error.maintenance"),
    VALIDATION("error.validation"),

    // Auth
    AUTH_EMAIL_OR_PASSWORD("auth.error.emailOrPassword"),
    AUTH_EMAIL_EXISTS("auth.error.emailExists"),
    AUTH_INVALID_CREDENTIALS("auth.error.invalidCredentials"),
    AUTH_ACCOUNT_LOCKED("auth.error.accountLocked"),
    AUTH_ACCOUNT_DISABLED("auth.error.accountDisabled"),
    AUTH_TOKEN_EXPIRED("auth.error.tokenExpired"),
    AUTH_TOKEN_INVALID("auth.error.tokenInvalid"),
    AUTH_TOKEN_REVOKED("auth.error.tokenRevoked"),
    AUTH_REGISTRATION_FAILED("auth.error.registrationFailed"),
    AUTH_PASSWORD_TOO_WEAK("auth.error.passwordTooWeak"),
    AUTH_PASSWORD_MISMATCH("auth.error.passwordMismatch"),
    AUTH_OLD_PASSWORD_INCORRECT("auth.error.oldPasswordIncorrect"),
    AUTH_OAUTH_PROVIDER_NOT_SUPPORTED("auth.error.oauthProviderNotSupported"),
    AUTH_OAUTH_STATE_INVALID("auth.error.oauthStateInvalid"),
    AUTH_TWO_FACTOR_REQUIRED("auth.error.twoFactorRequired"),
    AUTH_TWO_FACTOR_INVALID("auth.error.twoFactorInvalid"),
    AUTH_TWO_FACTOR_EXPIRED("auth.error.twoFactorExpired"),
    AUTH_RESET_TOKEN_INVALID("auth.error.resetTokenInvalid"),
    AUTH_VERIFY_TOKEN_INVALID("auth.error.verifyTokenInvalid"),
    AUTH_EMAIL_NOT_VERIFIED("auth.error.emailNotVerified"),
    AUTH_SESSION_EXPIRED("auth.error.sessionExpired"),

    // Deposit
    DEPOSIT_NOT_FOUND("deposit.error.notFound"),
    DEPOSIT_NOT_PENDING("deposit.error.notPending"),
    DEPOSIT_ALREADY_COMPLETED("deposit.error.alreadyCompleted"),
    DEPOSIT_CANNOT_CANCEL("deposit.error.cannotCancel"),
    DEPOSIT_AMOUNT_TOO_SMALL("deposit.error.amountTooSmall"),
    DEPOSIT_AMOUNT_TOO_LARGE("deposit.error.amountTooLarge"),
    DEPOSIT_EXPIRED("deposit.error.expired"),
    DEPOSIT_INVALID_CODE("deposit.error.invalidCode"),
    DEPOSIT_DUPLICATE("deposit.error.duplicate"),

    // Order
    ORDER_NOT_FOUND("order.error.notFound"),
    ORDER_CANNOT_CANCEL("order.error.cannotCancel"),
    ORDER_ALREADY_PROCESSED("order.error.alreadyProcessed"),
    ORDER_INVALID_STATUS("order.error.invalidStatus"),
    ORDER_SERVICE_UNAVAILABLE("order.error.serviceUnavailable"),
    ORDER_INSUFFICIENT_BALANCE("order.error.insufficientBalance"),
    ORDER_SERVICE_REQUIRES_CONSULTATION("order.error.serviceRequiresConsultation"),
    ORDER_PRICE_NOT_VALID("order.error.priceNotValid"),
    ORDER_DUPLICATE("order.error.duplicate"),
    ORDER_SERVICE_ID_INVALID("order.error.serviceIdInvalid"),

    // Wallet
    WALLET_INSUFFICIENT("wallet.error.insufficient"),
    WALLET_NOT_FOUND("wallet.error.notFound"),
    WALLET_TRANSACTION_NOT_FOUND("wallet.error.transactionNotFound"),
    WALLET_INVALID_AMOUNT("wallet.error.invalidAmount"),
    WALLET_NEGATIVE_AMOUNT("wallet.error.negativeAmount"),

    // Ticket
    TICKET_NOT_FOUND("ticket.error.notFound"),
    TICKET_CLOSED("ticket.error.closed"),
    TICKET_CANNOT_CLOSE("ticket.error.cannotClose"),
    TICKET_CANNOT_REOPEN("ticket.error.cannotReopen"),
    TICKET_MESSAGE_EMPTY("ticket.error.messageEmpty"),
    TICKET_ATTACHMENT_TOO_LARGE("ticket.error.attachmentTooLarge"),
    TICKET_ATTACHMENT_TYPE_NOT_ALLOWED("ticket.error.attachmentTypeNotAllowed"),

    // Coupon
    COUPON_INVALID("coupon.error.invalid"),
    COUPON_EXPIRED("coupon.error.expired"),
    COUPON_USAGE_LIMIT("coupon.error.usageLimit"),
    COUPON_NOT_APPLICABLE("coupon.error.notApplicable"),
    COUPON_MIN_AMOUNT_NOT_MET("coupon.error.minAmountNotMet"),
    COUPON_ALREADY_USED("coupon.error.alreadyUsed"),

    // Service
    SERVICE_NOT_FOUND("service.error.notFound"),
    SERVICE_OUT_OF_STOCK("service.error.outOfStock"),
    SERVICE_INACTIVE("service.error.inactive"),
    SERVICE_INVALID_INPUT("service.error.invalidInput"),
    SERVICE_CTA_NOT_ALLOWED("service.error.ctaNotAllowed"),

    // Inventory
    INVENTORY_ACCOUNT_NOT_FOUND("inventory.error.accountNotFound"),
    INVENTORY_ACCOUNT_ALREADY_ASSIGNED("inventory.error.accountAlreadyAssigned"),
    INVENTORY_ACCOUNT_EXPIRED("inventory.error.accountExpired"),
    INVENTORY_BATCH_NOT_FOUND("inventory.error.batchNotFound"),
    INVENTORY_INVALID_CREDENTIAL_FORMAT("inventory.error.invalidCredentialFormat"),

    // File
    FILE_TOO_LARGE("file.error.tooLarge"),
    FILE_TYPE_NOT_ALLOWED("file.error.typeNotAllowed"),
    FILE_UPLOAD_FAILED("file.error.uploadFailed"),
    FILE_NOT_FOUND("file.error.notFound"),
    FILE_DELETE_FAILED("file.error.deleteFailed"),
    FILE_VIRUS_DETECTED("file.error.virusDetected"),

    // Data integrity
    DATA_UNIQUE_CONSTRAINT("data.error.uniqueConstraint"),
    DATA_FOREIGN_KEY_CONSTRAINT("data.error.foreignKeyConstraint"),
    DATA_INVALID("data.error.invalidData"),

    // Webhook
    WEBHOOK_INVALID_PAYLOAD("webhook.error.invalidPayload"),
    WEBHOOK_INVALID_SIGNATURE("webhook.error.invalidSignature"),
    WEBHOOK_TRANSACTION_NOT_FOUND("webhook.error.transactionNotFound"),

    // Admin
    ADMIN_NOT_FOUND("admin.error.notFound"),
    ADMIN_CANNOT_MODIFY_SELF("admin.error.cannotModifySelf"),
    ADMIN_ROLE_NOT_FOUND("admin.error.roleNotFound"),
    ADMIN_PERMISSION_DENIED("admin.error.permissionDenied"),

    // Warranty
    WARRANTY_NOT_FOUND("warranty.error.notFound"),
    WARRANTY_OUT_OF_RANGE("warranty.error.outOfRange"),
    WARRANTY_ORDER_NOT_ELIGIBLE("warranty.error.orderNotEligible"),
    WARRANTY_DUPLICATE("warranty.error.duplicate");

    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
