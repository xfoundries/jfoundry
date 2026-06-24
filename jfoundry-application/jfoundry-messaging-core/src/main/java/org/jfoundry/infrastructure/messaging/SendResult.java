package org.jfoundry.infrastructure.messaging;

/// 消息发送结果。
/// @param success      是否发送成功
/// @param errorMessage 失败时的错误信息；成功时为 null
public record SendResult(boolean success, String errorMessage) {

    public static SendResult ok() {
        return new SendResult(true, null);
    }

    public static SendResult fail(String errorMessage) {
        return new SendResult(false, errorMessage);
    }
}
