package com.luoxianjun.community.common.exception;

import lombok.Getter;

/**
 * 业务异常：由业务校验主动抛出，携带 {@link ErrorCode}。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(detail);
        this.code = errorCode.getCode();
    }
}
