package com.foosbot.model;

public class Player {
    private long id;
    private String slackUserId;
    private String displayName;

    public Player() {}

    public Player(long id, String slackUserId, String displayName) {
        this.id = id;
        this.slackUserId = slackUserId;
        this.displayName = displayName;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSlackUserId() { return slackUserId; }
    public void setSlackUserId(String slackUserId) { this.slackUserId = slackUserId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
