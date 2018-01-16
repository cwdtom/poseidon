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
}
