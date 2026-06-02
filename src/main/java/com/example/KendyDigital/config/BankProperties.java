package com.example.KendyDigital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bank")
public class BankProperties {
    private String name = "ACB";
    private String accountNumber = "0000000000";
    private String accountOwner = "KENDY DIGITAL";
    private int depositExpiryMinutes = 30;
    private String transferPrefix = "KD";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(String accountOwner) {
        this.accountOwner = accountOwner;
    }

    public int getDepositExpiryMinutes() {
        return depositExpiryMinutes;
    }

    public void setDepositExpiryMinutes(int depositExpiryMinutes) {
        this.depositExpiryMinutes = depositExpiryMinutes;
    }

    public String getTransferPrefix() {
        return transferPrefix;
    }

    public void setTransferPrefix(String transferPrefix) {
        this.transferPrefix = transferPrefix;
    }
}
