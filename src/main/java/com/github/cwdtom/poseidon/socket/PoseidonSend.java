package com.github.cwdtom.poseidon.socket;

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
    private Long reconnectInterval;
    private Long initReconnectInterval;

    public PoseidonSend(String ip, Integer port, Long reconnectInterval) {
        this.ip = ip;
        this.port = port;
        this.reconnectInterval = reconnectInterval;
        this.initReconnectInterval = reconnectInterval;
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
                p.addLast(new IdleStateHandler(0, 1, 0));
                p.addLast(new Encoder());
                p.addLast(new SendHandler());
            }
        });
        try {
            ChannelFuture channelFuture = bootstrap.connect(this.ip, this.port).sync();
            if (channelFuture.isSuccess()) {
                // 重连时间间隔初始化
                this.reconnectInterval = this.initReconnectInterval;
                log.info("poseidon is connected with " + this.ip + ":" + this.port);
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.warn("poseidon start fail", e);
        } finally {
            eventLoopGroup.shutdownGracefully();
            reconnect();
        }
    }

    /**
     * 重连
     */
    private void reconnect() {
        try {
            // 防止频繁重连，消耗资源
            Thread.sleep(this.reconnectInterval);
            // 每次重连后增加下次重连间隔
            this.reconnectInterval = this.reconnectInterval << 1;
        } catch (InterruptedException e) {
            // 结束日志输出
            log.error("poseidon reconnect fail, exit");
            return;
        }
        // 重连
        this.start();
    }

    /**
     * 编码器
     */
    private class Encoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext, Message message, ByteBuf byteBuf) {
            // 判断是否为心跳包
            if (message.getLevel() == 0) {
                byteBuf.writeInt(0);
                return;
            }
            byteBuf.writeInt(message.getLength());
            byteBuf.writeInt(message.getLevel());
            byteBuf.writeBytes(message.getData());
        }
    }

    /**
     * 处理发送
     */
    private class SendHandler extends ChannelInboundHandlerAdapter {
        /**
         * 空闲次数
         */
        private Integer idleCount = 0;
        /**
         * 心跳发送，空闲次数间隔
         */
        private final Integer heartbeatInterval = 20;
        /**
         * 心跳消息level
         */
        private final Integer heartbeatLevel = 0;

        /**
         * 处理心跳
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (PoseidonFilter.queue.isEmpty()) {
                this.idleCount++;
                if (this.idleCount > this.heartbeatInterval) {
                    ctx.writeAndFlush(new Message(this.heartbeatLevel));
                    this.idleCount = 0;
                }
                return;
            }
            this.idleCount = 0;
            while (!PoseidonFilter.queue.isEmpty()) {
                ctx.writeAndFlush(PoseidonFilter.queue.poll());
            }
        }
    }
}
