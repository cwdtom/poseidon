package com.github.cwdtom.poseidon.socket;

import ch.qos.logback.classic.Level;
import com.github.cwdtom.poseidon.entity.Message;
import com.github.cwdtom.poseidon.filter.PoseidonFilter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 发送日志消息
 *
 * @author chenweidong
 * @since 1.0.0
 */
@Slf4j
public class PoseidonSend implements Runnable {
    private String ip;
    private Integer port;

    public PoseidonSend(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        this.start();
    }

    /**
     * 启动socket连接
     */
    private void start() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.group(eventLoopGroup);
        bootstrap.remoteAddress(this.ip, this.port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline p = socketChannel.pipeline();
                p.addLast(new Encoder());
                p.addLast(new SendHandler());
                p.addLast(new IdleStateHandler(0, 15, 0));
                p.addLast(new HeartbeatHandler());
            }
        });
        ChannelFuture channelFuture = null;
        try {
            channelFuture = bootstrap.connect(this.ip, this.port).sync();
            if (channelFuture.isSuccess()) {
                log.info("poseidon is connected with " + this.ip + ":" + this.port);
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.warn("poseidon start fail", e);
        } finally {
            if (null != channelFuture) {
                if (channelFuture.channel() != null && channelFuture.channel().isOpen()) {
                    channelFuture.channel().close();
                }
            }
            // 重连
            start();
        }
    }

    /**
     * 编码器
     */
    private class Encoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, Message message, ByteBuf byteBuf) {
            byteBuf.writeInt(message.getLength());
            byteBuf.writeInt(message.getLevel());
            byteBuf.writeBytes(message.getData());
        }
    }

    /**
     * 处理发送
     */
    private class SendHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws InterruptedException {
            while (true) {
                // 阻塞
                ctx.writeAndFlush(PoseidonFilter.queue.take());
            }
        }
    }

    /**
     * 处理心跳
     */
    private class HeartbeatHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            ctx.writeAndFlush(new Message(Level.INFO_INT, "heartbeat"));
        }
    }
}
