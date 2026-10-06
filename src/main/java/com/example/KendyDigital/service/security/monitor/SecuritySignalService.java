package com.example.KendyDigital.service.security.monitor;

public interface SecuritySignalService {
    /** Record a security signal: persist it, update risk scores, and run detection rules. */
    void record(SecuritySignal signal);

    boolean isEnabled();
}
