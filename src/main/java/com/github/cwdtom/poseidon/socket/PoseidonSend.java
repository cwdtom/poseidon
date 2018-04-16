package com.github.cwdtom.poseidon.socket;

import com.github.cwdtom.poseidon.coder.Encoder;
import com.github.cwdtom.poseidon.entity.Message;
import com.github.cwdtom.poseidon.filter.PoseidonFilter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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
    /**
     * ip
     */
    private String ip;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 重连时间间隔
     */
    private Long reconnectInterval;

    public PoseidonSend(String ip, Integer port, Long reconnectInterval) {
        this.ip = ip;
        this.port = port;
        this.reconnectInterval = reconnectInterval;
    }

    @Override
    public void run() {
        this.start();
    }

    /**
     * 启动socket连接
     */
    private void start() {
        // cpu核数
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
                log.info("poseidon is connected with " + this.ip + ":" + this.port + ".");
                log.info("poseidon reconnect interval is " + this.reconnectInterval + "ms.");
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.warn("poseidon sync exception.\n", e);
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
        } catch (InterruptedException e) {
            log.error("poseidon thread wake up fail, already exit.");
            return;
        }
        // 重连
        log.info("poseidon start reconnect.");
        this.start();
    }

    /**
     * 处理发送
     */
    private class SendHandler extends ChannelInboundHandlerAdapter {
        /**
         * 空闲次数
         */
        private int idleCount = 0;
        /**
         * 心跳发送，空闲次数间隔
         */
        private final int heartbeatInterval = 20;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (PoseidonFilter.queue.isEmpty()) {
                this.idleCount++;
                if (this.idleCount > this.heartbeatInterval) {
                    ctx.writeAndFlush(Message.buildHeartbeat());
                    this.idleCount = 0;
                }
            } else {
                this.idleCount = 0;
                while (!PoseidonFilter.queue.isEmpty()) {
                    ctx.writeAndFlush(PoseidonFilter.queue.poll());
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.warn("poseidon caught exception.", cause);
            ctx.close();
        }
    }
}
