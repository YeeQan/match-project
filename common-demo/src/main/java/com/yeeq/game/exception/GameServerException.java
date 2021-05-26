package com.yeeq.game.exception;

import com.yeeq.game.error.GameServerError;

/**
 * @author yeeq
 * @date 2021/5/2
 */
public class GameServerException extends RuntimeException {

    private Integer code;

    private String message;

    public GameServerException(GameServerError error) {
        super(error.getErrorDesc());
        this.code = error.getErrorCode();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
