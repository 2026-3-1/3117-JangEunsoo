package com.jes.devlearn.domain.notification.sender;

import com.jes.devlearn.domain.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Discord 채널 webhook 발송. DISCORD_WEBHOOK_URL 미설정 시 로그 fallback.
 * 발송 실패(네트워크/4xx-5xx)는 예외로 전파되어 outbox 재시도 대상이 된다.
 */
@Slf4j
@Component
public class DiscordWebhookSender implements NotificationSender {

    private final String webhookUrl;
    private final RestClient restClient;

    public DiscordWebhookSender(@Value("${app.notification.discord-webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.DISCORD;
    }

    @Override
    public void send(String title, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("[Discord:fallback-log] {} | {}", title, message);
            return;
        }
        String content = "**" + title + "**\n" + message;
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", truncate(content)))
                .retrieve()
                .toBodilessEntity();
    }

    private String truncate(String s) {
        // Discord content 한도 2000자
        return s.length() > 1900 ? s.substring(0, 1900) + "…" : s;
    }
}
