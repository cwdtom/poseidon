package com.github.cwdtom.poseidon.coder;

import com.github.cwdtom.poseidon.entity.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 解码器
 *
 * @author chenweidong
 * @since 2.2.0
 */
public class Decoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        // 确认协议
        if (byteBuf.readInt() != Message.MAGIC_NUM) {
            byteBuf.discardReadBytes();
            return;
        }
        // 读取数据长度
        int length = byteBuf.readInt();
        // 读取日志类型
        int level = byteBuf.readInt();
        // 判断是否为心跳包
        if (level == 0) {
            byteBuf.discardReadBytes();
            return;
        }
        // 判断数据包是否到齐
        if (byteBuf.readableBytes() < length) {
            // 读取位置归0
            byteBuf.readerIndex(0);
            return;
        }
        byte[] body = new byte[length];
        byteBuf.readBytes(body);
        // 释放已读buffer
        byteBuf.discardReadBytes();
        list.add(new Message(length, level, body));
    }
}
