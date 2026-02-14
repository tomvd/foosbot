package com.foosbot.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("slack")
public class SlackConfig {

    private String botToken;
    private String appToken;
    private String signingSecret;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }
}
