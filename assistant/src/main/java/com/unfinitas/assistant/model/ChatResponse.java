package com.unfinitas.assistant.model;

public record ChatResponse(String reply, boolean success) {
    public static ChatResponse ok(final String reply) {
        return new ChatResponse(reply, true);
    }

    public static ChatResponse error(final String reply) {
        return new ChatResponse(reply, false);
    }
}
