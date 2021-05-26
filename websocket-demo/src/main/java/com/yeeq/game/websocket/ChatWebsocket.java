package com.yeeq.game.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yeeq.game.constant.CommonField;
import com.yeeq.game.entity.*;
import com.yeeq.game.error.GameServerError;
import com.yeeq.game.exception.GameServerException;
import com.yeeq.game.service.QuestionSev;
import com.yeeq.game.utils.MatchCacheUtil;
import com.yeeq.game.utils.MessageCode;
import com.yeeq.game.utils.MessageTypeEnum;
import com.yeeq.game.utils.StatusEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yeeq
 * @date 2021/4/9
 */
@Component
@Slf4j
@ServerEndpoint(value = "/game/match/{userId}")
public class ChatWebsocket {

    private Session session;

    private String userId;

    static QuestionSev questionSev;
    static MatchCacheUtil matchCacheUtil;

    static Lock lock = new ReentrantLock();

    static Condition matchCond = lock.newCondition();

    @Autowired
    public void setMatchCacheUtil(MatchCacheUtil matchCacheUtil) {
        ChatWebsocket.matchCacheUtil = matchCacheUtil;
    }

    @Autowired
    public void setQuestionSev(QuestionSev questionSev) {
        ChatWebsocket.questionSev = questionSev;
    }

    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session) {

        log.info("ChatWebsocket open 有新连接加入 userId: {}", userId);

        this.userId = userId;
        this.session = session;
        matchCacheUtil.addClient(userId, this);

        log.info("ChatWebsocket open 连接建立完成 userId: {}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {

        log.error("ChatWebsocket onError 发生了错误 userId: {}, errorMessage: {}", userId, error.getMessage());

        matchCacheUtil.removeClinet(userId);
        matchCacheUtil.removeUserOnlineStatus(userId);
        matchCacheUtil.removeUserFromRoom(userId);
        matchCacheUtil.removeUserMatchInfo(userId);

        log.info("ChatWebsocket onError 连接断开完成 userId: {}", userId);
    }

    @OnClose
    public void onClose()
    {
        log.info("ChatWebsocket onClose 连接断开 userId: {}", userId);

        matchCacheUtil.removeClinet(userId);
        matchCacheUtil.removeUserOnlineStatus(userId);
        matchCacheUtil.removeUserFromRoom(userId);
        matchCacheUtil.removeUserMatchInfo(userId);

        log.info("ChatWebsocket onClose 连接断开完成 userId: {}", userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {

        log.info("ChatWebsocket onMessage userId: {}, 来自客户端的消息 message: {}", userId, message);

        JSONObject jsonObject = JSON.parseObject(message);
        MessageTypeEnum type = jsonObject.getObject("type", MessageTypeEnum.class);

        log.info("ChatWebsocket onMessage userId: {}, 来自客户端的消息类型 type: {}", userId, type);

        if (type == MessageTypeEnum.ADD_USER) {
            addUser(jsonObject);
        } else if (type == MessageTypeEnum.MATCH_USER) {
            matchUser(jsonObject);
        } else if (type == MessageTypeEnum.CANCEL_MATCH) {
            cancelMatch(jsonObject);
        } else if (type == MessageTypeEnum.PLAY_GAME) {
            toPlay(jsonObject);
        } else if (type == MessageTypeEnum.GAME_OVER) {
            gameover(jsonObject);
        } else {
            throw new GameServerException(GameServerError.WEBSOCKET_ADD_USER_FAILED);
        }

        log.info("ChatWebsocket onMessage userId: {} 消息接收结束", userId);
    }

    /**
     * 群发消息
     */
    private void sendMessageAll(MessageReply<?> messageReply) {

        log.info("ChatWebsocket sendMessageAll 消息群发开始 userId: {}, messageReply: {}", userId, JSON.toJSONString(messageReply));

        Set<String> receivers = messageReply.getChatMessage().getReceivers();
        for (String receiver : receivers) {
            ChatWebsocket client = matchCacheUtil.getClient(receiver);
            client.session.getAsyncRemote().sendText(JSON.toJSONString(messageReply));
        }

        log.info("ChatWebsocket sendMessageAll 消息群发结束 userId: {}", userId);
    }

    /**
     * 用户加入游戏
     */
    private void addUser(JSONObject jsonObject) {

        log.info("ChatWebsocket addUser 用户加入游戏开始 message: {}, userId: {}", jsonObject.toJSONString(), userId);

        MessageReply<Object> messageReply = new MessageReply<>();
        ChatMessage<Object> result = new ChatMessage<>();
        result.setType(MessageTypeEnum.ADD_USER);
        result.setSender(userId);

        /*
         * 获取用户的在线状态
         * 如果缓存中没有保存用户状态，表示用户新加入，则设置为在线状态
         * 否则直接返回
         */
        StatusEnum status = matchCacheUtil.getUserOnlineStatus(userId);
        if (status != null) {
            /*
             * 游戏结束状态，重新设置为在线状态
             * 否则返回错误提示信息
             */
            if (status.compareTo(StatusEnum.GAME_OVER) == 0) {
                messageReply.setCode(MessageCode.SUCCESS.getCode());
                messageReply.setDesc(MessageCode.SUCCESS.getDesc());
                matchCacheUtil.setUserIDLE(userId);
            } else {
                messageReply.setCode(MessageCode.USER_IS_ONLINE.getCode());
                messageReply.setDesc(MessageCode.USER_IS_ONLINE.getDesc());
            }
        } else {
            messageReply.setCode(MessageCode.SUCCESS.getCode());
            messageReply.setDesc(MessageCode.SUCCESS.getDesc());
            matchCacheUtil.setUserIDLE(userId);
        }

        Set<String> receivers = new HashSet<>();
        receivers.add(userId);
        result.setReceivers(receivers);
        messageReply.setChatMessage(result);

        sendMessageAll(messageReply);

        log.info("ChatWebsocket addUser 用户加入游戏结束 message: {}, userId: {}", jsonObject.toJSONString(), userId);

    }

    /**
     * 用户随机匹配对手
     */
    @SneakyThrows
    private void matchUser(JSONObject jsonObject) {

        log.info("ChatWebsocket matchUser 用户随机匹配对手开始 message: {}, userId: {}", jsonObject.toJSONString(), userId);

        MessageReply<GameMatchInfo> messageReply = new MessageReply<>();
        ChatMessage<GameMatchInfo> result = new ChatMessage<>();
        result.setSender(userId);
        result.setType(MessageTypeEnum.MATCH_USER);

        lock.lock();
        try {
            // 设置用户状态为匹配中
            matchCacheUtil.setUserInMatch(userId);
            matchCond.signal();
        } finally {
            lock.unlock();
        }

        // 创建一个异步线程任务，负责匹配其他同样处于匹配状态的其他用户
        Thread matchThread = new Thread(() -> {
            boolean flag = true;
            String receiver = null;
            while (flag) {
                // 获取除自己以外的其他待匹配用户
                lock.lock();
                try {
                    // 当前用户不处于待匹配状态
                    if (matchCacheUtil.getUserOnlineStatus(userId).compareTo(StatusEnum.IN_GAME) == 0
                            || matchCacheUtil.getUserOnlineStatus(userId).compareTo(StatusEnum.GAME_OVER) == 0) {
                        log.info("ChatWebsocket matchUser 当前用户 {} 已退出匹配", userId);
                        return;
                    }
                    // 当前用户取消匹配状态
                    if (matchCacheUtil.getUserOnlineStatus(userId).compareTo(StatusEnum.IDLE) == 0) {
                        // 当前用户取消匹配
                        messageReply.setCode(MessageCode.CANCEL_MATCH_ERROR.getCode());
                        messageReply.setDesc(MessageCode.CANCEL_MATCH_ERROR.getDesc());
                        Set<String> set = new HashSet<>();
                        set.add(userId);
                        result.setReceivers(set);
                        result.setType(MessageTypeEnum.CANCEL_MATCH);
                        messageReply.setChatMessage(result);
                        log.info("ChatWebsocket matchUser 当前用户 {} 已退出匹配", userId);
                        sendMessageAll(messageReply);
                        return;
                    }
                    receiver = matchCacheUtil.getUserInMatchRandom(userId);
                    if (receiver != null) {
                        // 对手不处于待匹配状态
                        if (matchCacheUtil.getUserOnlineStatus(receiver).compareTo(StatusEnum.IN_MATCH) != 0) {
                            log.info("ChatWebsocket matchUser 当前用户 {}, 匹配对手 {} 已退出匹配状态", userId, receiver);
                        } else {
                            matchCacheUtil.setUserInGame(userId);
                            matchCacheUtil.setUserInGame(receiver);
                            matchCacheUtil.setUserInRoom(userId, receiver);
                            flag = false;
                        }
                    } else {
                        // 如果当前没有待匹配用户，进入等待队列
                        try {
                            log.info("ChatWebsocket matchUser 当前用户 {} 无对手可匹配", userId);
                            matchCond.await();
                        } catch (InterruptedException e) {
                            log.error("ChatWebsocket matchUser 匹配线程 {} 发生异常: {}",
                                    Thread.currentThread().getName(), e.getMessage());
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            UserMatchInfo senderInfo = new UserMatchInfo();
            UserMatchInfo receiverInfo = new UserMatchInfo();
            senderInfo.setUserId(userId);
            senderInfo.setScore(0);
            receiverInfo.setUserId(receiver);
            receiverInfo.setScore(0);

            matchCacheUtil.setUserMatchInfo(userId, JSON.toJSONString(senderInfo));
            matchCacheUtil.setUserMatchInfo(receiver, JSON.toJSONString(receiverInfo));

            GameMatchInfo gameMatchInfo = new GameMatchInfo();
            List<Question> questions = questionSev.getAllQuestion();
            gameMatchInfo.setQuestions(questions);
            gameMatchInfo.setSelfInfo(senderInfo);
            gameMatchInfo.setOpponentInfo(receiverInfo);

            messageReply.setCode(MessageCode.SUCCESS.getCode());
            messageReply.setDesc(MessageCode.SUCCESS.getDesc());

            result.setData(gameMatchInfo);
            Set<String> set = new HashSet<>();
            set.add(userId);
            result.setReceivers(set);
            result.setType(MessageTypeEnum.MATCH_USER);
            messageReply.setChatMessage(result);
            sendMessageAll(messageReply);

            gameMatchInfo.setSelfInfo(receiverInfo);
            gameMatchInfo.setOpponentInfo(senderInfo);

            result.setData(gameMatchInfo);
            set.clear();
            set.add(receiver);
            result.setReceivers(set);
            messageReply.setChatMessage(result);

            sendMessageAll(messageReply);

            log.info("ChatWebsocket matchUser 用户随机匹配对手结束 messageReply: {}", JSON.toJSONString(messageReply));

        }, CommonField.MATCH_TASK_NAME_PREFIX + userId);
        matchThread.start();
    }

    /**
     * 取消匹配
     */
    private void cancelMatch(JSONObject jsonObject) {

        log.info("ChatWebsocket cancelMatch 用户取消匹配开始 userId: {}, message: {}", userId, jsonObject.toJSONString());

        lock.lock();
        try {
            matchCacheUtil.setUserIDLE(userId);
        } finally {
            lock.unlock();
        }

        log.info("ChatWebsocket cancelMatch 用户取消匹配结束 userId: {}", userId);
    }

    /**
     * 游戏中
     */
    @SneakyThrows
    public void toPlay(JSONObject jsonObject) {

        log.info("ChatWebsocket toPlay 用户更新对局信息开始 userId: {}, message: {}", userId, jsonObject.toJSONString());

        MessageReply<UserMatchInfo> messageReply = new MessageReply<>();

        ChatMessage<UserMatchInfo> result = new ChatMessage<>();
        result.setSender(userId);
        String receiver = matchCacheUtil.getUserFromRoom(userId);
        Set<String> set = new HashSet<>();
        set.add(receiver);
        result.setReceivers(set);
        result.setType(MessageTypeEnum.PLAY_GAME);

        Integer newScore = jsonObject.getInteger("data");
        UserMatchInfo userMatchInfo = new UserMatchInfo();
        userMatchInfo.setUserId(userId);
        userMatchInfo.setScore(newScore);

        matchCacheUtil.setUserMatchInfo(userId, JSON.toJSONString(userMatchInfo));

        result.setData(userMatchInfo);
        messageReply.setCode(MessageCode.SUCCESS.getCode());
        messageReply.setDesc(MessageCode.SUCCESS.getDesc());
        messageReply.setChatMessage(result);

        sendMessageAll(messageReply);

        log.info("ChatWebsocket toPlay 用户更新对局信息结束 userId: {}, userMatchInfo: {}", userId, JSON.toJSONString(userMatchInfo));
    }

    /**
     * 游戏结束
     */
    public void gameover(JSONObject jsonObject) {

        log.info("ChatWebsocket gameover 用户对局结束 userId: {}, message: {}", userId, jsonObject.toJSONString());

        MessageReply<UserMatchInfo> messageReply = new MessageReply<>();

        ChatMessage<UserMatchInfo> result = new ChatMessage<>();
        result.setSender(userId);
        String receiver = matchCacheUtil.getUserFromRoom(userId);
        result.setType(MessageTypeEnum.GAME_OVER);

        lock.lock();
        try {
            matchCacheUtil.setUserGameover(userId);
            if (matchCacheUtil.getUserOnlineStatus(receiver).compareTo(StatusEnum.GAME_OVER) == 0) {
                messageReply.setCode(MessageCode.SUCCESS.getCode());
                messageReply.setDesc(MessageCode.SUCCESS.getDesc());

                String userMatchInfo = matchCacheUtil.getUserMatchInfo(userId);
                result.setData(JSON.parseObject(userMatchInfo, UserMatchInfo.class));
                messageReply.setChatMessage(result);
                Set<String> set = new HashSet<>();
                set.add(receiver);
                result.setReceivers(set);
                sendMessageAll(messageReply);

                String receiverMatchInfo = matchCacheUtil.getUserMatchInfo(receiver);
                result.setData(JSON.parseObject(receiverMatchInfo, UserMatchInfo.class));
                messageReply.setChatMessage(result);
                set.clear();
                set.add(userId);
                result.setReceivers(set);
                sendMessageAll(messageReply);

                matchCacheUtil.removeUserMatchInfo(userId);
                matchCacheUtil.removeUserFromRoom(userId);
            }
        }  finally {
            lock.unlock();
        }

        log.info("ChatWebsocket gameover 对局 [{} - {}] 结束", userId, receiver);
    }
}
