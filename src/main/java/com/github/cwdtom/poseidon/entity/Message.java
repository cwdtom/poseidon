package com.github.cwdtom.poseidon.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 消息实体，自定义socket协议
 *
 * @author chenweidong
 * @since 1.1.0
 */
@Data
@AllArgsConstructor
public class Message {
    /**
     * 协议魔数
     */
    public static final int MAGIC_NUM = 0xB03D;
    /**
     * 数据长度
     */
    private Integer length;
    /**
     * 日志等级
     */
    private Integer level;
    /**
     * 数据
     */
    private byte[] data;

    public Message(Integer level, String msg) {
        byte[] data = msg.getBytes();
        this.length = data.length;
        this.level = level;
        this.data = data;
    }

    /**
     * 获取心跳包message
     *
     * @return 心跳包message
     */
    public static Message buildHeartbeat() {
        return new Message(0, "");
    }
}
