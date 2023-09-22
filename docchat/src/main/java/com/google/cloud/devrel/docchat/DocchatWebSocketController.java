package com.google.cloud.devrel.docchat;

import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;

@ServerWebSocket("/chat")
public class DocchatWebSocketController {

    private final WebSocketBroadcaster broadcaster;
    private final LLMQueryService queryService;

    public DocchatWebSocketController(WebSocketBroadcaster broadcaster, LLMQueryService queryService) {
        this.broadcaster = broadcaster;
        this.queryService = queryService;
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
        broadcaster.broadcastSync("Open");
        System.out.println("Open: " + session.getId());
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        broadcaster.broadcastSync("Close");
        System.out.println("Close: " + session.getId());
    }

    @OnMessage
    public void onMessage(WsChatMessage message, WebSocketSession session) {
        System.out.println("Query: " + message);
        session.sendSync(
            "<div hx-swap-oob='beforeend:#chat'>" +
            "<div class='right'><p>" + message.message() + "</p></div>" +
            "<div class='left'><div>" + queryService.execute(message.message()) + "</div></div>" +
            "</div>" +
            "<textarea hx-swap-oob='outerHTML' name='message' id='message-area'></textarea>"
        );
    }
}