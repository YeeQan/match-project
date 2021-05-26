package com.yeeq.game.entity;

import lombok.Data;

/**
 * websocket 响应消息类
 *
 * @author yeeq
 */
@Data
public class MessageReply<T> {

    private Integer code;

    private String desc;

    private ChatMessage<T> chatMessage;
}
