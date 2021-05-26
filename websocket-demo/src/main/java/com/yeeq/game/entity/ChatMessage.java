package com.yeeq.game.entity;


import com.yeeq.game.utils.MessageTypeEnum;
import lombok.Data;

import java.util.Set;


/**
 * websocket 通信消息类
 *
 * @author yeeq
 */
@Data
public class ChatMessage<T> {

    /**
     * 消息类型
     */
    private MessageTypeEnum type;

    /**
     * 消息发送者
     */
    private String sender;

    /**
     * 消息接收者
     */
    private Set<String> receivers;

    private T data;
}

