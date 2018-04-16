package com.github.cwdtom.poseidon.coder;

import com.github.cwdtom.poseidon.entity.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 编码器
 *
 * @author chenweidong
 * @since 2.2.0
 */
public class Encoder extends MessageToByteEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message, ByteBuf byteBuf) {
        byteBuf.writeInt(Message.MAGIC_NUM);
        byteBuf.writeInt(message.getLength());
        byteBuf.writeInt(message.getLevel());
        byteBuf.writeBytes(message.getData());
    }
}
