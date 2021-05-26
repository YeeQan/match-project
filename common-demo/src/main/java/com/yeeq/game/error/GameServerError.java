package com.yeeq.game.error;

/**
 * @author yeeq
 * @date 2021/2/27
 */
public enum GameServerError implements IServerError {

    /**
     * 枚举型错误码
     */
    WEBSOCKET_ADD_USER_FAILED(4018, "用户进入匹配模式失败"),
    MESSAGE_TYPE_ERROR(4019, "websocket 消息类型错误"),
    ;

    private final Integer errorCode;
    private final String errorDesc;

    GameServerError(Integer errorCode, String errorDesc) {
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    @Override
    public Integer getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorDesc() {
        return errorDesc;
    }
}
