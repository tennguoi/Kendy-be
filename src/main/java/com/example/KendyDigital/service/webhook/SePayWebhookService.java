package com.example.KendyDigital.service.webhook;

import com.example.KendyDigital.dto.webhook.request.SePayWebhookPayload;
import com.example.KendyDigital.repository.*;

public interface SePayWebhookService {
    void process(SePayWebhookPayload payload, String rawPayload);
}
