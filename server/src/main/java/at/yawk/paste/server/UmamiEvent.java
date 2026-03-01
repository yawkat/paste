package at.yawk.paste.server;

/**
 * Umami event payload for {@code POST /api/send}.
 */
record UmamiEvent(String type, Payload payload) {
    record Payload(
            String website,
            String url,
            String hostname,
            String language,
            String referrer
    ) {}
}
