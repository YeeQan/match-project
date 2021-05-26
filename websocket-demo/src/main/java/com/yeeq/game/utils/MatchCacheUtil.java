package com.yeeq.game.utils;

import com.yeeq.game.websocket.ChatWebsocket;
import com.yeeq.game.redis.EnumRedisKey;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yeeq
 * @date 2021/4/16
 */
@Component
public class MatchCacheUtil {

    /**
     * 用户 userId 为 key，ChatWebsocket 为 value
     */
    private static final Map<String, ChatWebsocket> CLIENTS = new HashMap<>();

    /**
     * key 是标识存储用户在线状态的 EnumRedisKey，value 为 map 类型，其中用户在线状态为 key，用户 userId 为 value
     */
    @Resource
    private RedisTemplate<String, Map<String, Set<String>>> redisTemplate;

    /**
     * 添加客户端
     */
    public void addClient(String userId, ChatWebsocket websocket) {
        CLIENTS.put(userId, websocket);
    }

    /**
     * 移除客户端
     */
    public void removeClinet(String userId) {
        CLIENTS.remove(userId);
    }

    /**
     * 获取客户端
     */
    public ChatWebsocket getClient(String userId) {
        return CLIENTS.get(userId);
    }

    /**
     * 移除用户在线状态
     */
    public void removeUserOnlineStatus(String userId) {
        redisTemplate.opsForHash().delete(EnumRedisKey.USER_STATUS.getKey(), userId);
    }

    /**
     * 获取用户在线状态
     */
    public StatusEnum getUserOnlineStatus(String userId) {
        Object status = redisTemplate.opsForHash().get(EnumRedisKey.USER_STATUS.getKey(), userId);
        if (status == null) {
            return null;
        }
        return StatusEnum.getStatusEnum(status.toString());
    }

    /**
     * 设置用户为 IDLE 状态
     */
    public void setUserIDLE(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.IDLE.getValue());
    }

    /**
     * 设置用户为 IN_MATCH 状态
     */
    public void setUserInMatch(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.IN_MATCH.getValue());
    }

    /**
     * 随机获取处于匹配状态的用户（除了指定用户外）
     */
    public String getUserInMatchRandom(String userId) {
        Optional<Map.Entry<Object, Object>> any = redisTemplate.opsForHash().entries(EnumRedisKey.USER_STATUS.getKey())
                .entrySet().stream().filter(entry -> entry.getValue().equals(StatusEnum.IN_MATCH.getValue()) && !entry.getKey().equals(userId))
                .findAny();
        return any.map(entry -> entry.getKey().toString()).orElse(null);
    }

    /**
     * 设置用户为 IN_GAME 状态
     */
    public void setUserInGame(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.IN_GAME.getValue());
    }

    /**
     * 设置处于游戏中的用户在同一房间
     */
    public void setUserInRoom(String userId1, String userId2) {
        redisTemplate.opsForHash().put(EnumRedisKey.ROOM.getKey(), userId1, userId2);
        redisTemplate.opsForHash().put(EnumRedisKey.ROOM.getKey(), userId2, userId1);
    }

    /**
     * 从房间中移除用户
     */
    public void removeUserFromRoom(String userId) {
        redisTemplate.opsForHash().delete(EnumRedisKey.ROOM.getKey(), userId);
    }

    /**
     * 从房间中获取用户
     */
    public String getUserFromRoom(String userId) {
        return redisTemplate.opsForHash().get(EnumRedisKey.ROOM.getKey(), userId).toString();
    }

    /**
     * 设置处于游戏中的用户的对战信息
     */
    public void setUserMatchInfo(String userId, String userMatchInfo) {
        redisTemplate.opsForHash().put(EnumRedisKey.USER_MATCH_INFO.getKey(), userId, userMatchInfo);
    }

    /**
     * 移除处于游戏中的用户的对战信息
     */
    public void removeUserMatchInfo(String userId) {
        redisTemplate.opsForHash().delete(EnumRedisKey.USER_MATCH_INFO.getKey(), userId);
    }

    /**
     * 设置处于游戏中的用户的对战信息
     */
    public String getUserMatchInfo(String userId) {
        return redisTemplate.opsForHash().get(EnumRedisKey.USER_MATCH_INFO.getKey(), userId).toString();
    }

    /**
     * 设置用户为游戏结束状态
     */
    public synchronized void setUserGameover(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.GAME_OVER.getValue());
    }
}
