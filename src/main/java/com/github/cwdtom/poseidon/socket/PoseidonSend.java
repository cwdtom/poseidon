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
                // 重连时间间隔初始化
                this.reconnectInterval = this.initReconnectInterval;
                log.info("poseidon is connected with " + this.ip + ":" + this.port);
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.warn("poseidon start fail", e);
        } finally {
            reconnect(channelFuture);
        }
    }

    /**
     * 重连
     *
     * @param channelFuture 连接
     */
    private void reconnect(ChannelFuture channelFuture) {
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
        if (null != channelFuture) {
            if (channelFuture.channel() != null && channelFuture.channel().isOpen()) {
                channelFuture.channel().close();
            }
        }
        // 重连
        start();
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
        public void channelActive(ChannelHandlerContext ctx) {
            Message tmp = null;
            try {
                while (true) {
                    tmp = PoseidonFilter.queue.take();
                    // 阻塞
                    ctx.writeAndFlush(tmp);
                }
            } catch (InterruptedException e) {
                // 发送失败时将失败message放回队列
                if (tmp != null) {
                    PoseidonFilter.queue.offer(tmp);
                }
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
