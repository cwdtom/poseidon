package com.github.cwdtom.poseidon.socket;

import ch.qos.logback.classic.Level;
import com.github.cwdtom.poseidon.coder.Decoder;
import com.github.cwdtom.poseidon.entity.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 监听注册端口
 *
 * @author chenweidong
 * @since 1.0.0
 */
@Slf4j
@AllArgsConstructor
public class PoseidonSocket implements Runnable {
    /**
     * 端口
     */
    private Integer port;

    @Override
    public void run() {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline p = socketChannel.pipeline();
                p.addLast(new Decoder());
                p.addLast(new HandlerMessage());
                p.addLast(new IdleStateHandler(60, 0, 0));
            }
        });
        try {
            ChannelFuture channelFuture = bootstrap.bind(this.port).sync();
            if (channelFuture.isSuccess()) {
                log.info("poseidon is started in " + this.port + ".");
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("netty listen fail.\n", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    /**
     * 处理消息
     */
    private class HandlerMessage extends SimpleChannelInboundHandler<Message> {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
            InetSocketAddress isa = (InetSocketAddress) channelHandlerContext.channel().remoteAddress();
            String ip = isa.getAddress().getHostAddress();
            String inStr = new String(message.getData(), "utf-8");
            String logStr = String.format("[%s] - %s", ip, inStr);
            switch (message.getLevel()) {
                case Level.INFO_INT:
                    log.info(logStr);
                    break;
                case Level.WARN_INT:
                    log.warn(logStr);
                    break;
                case Level.ERROR_INT:
                    log.error(logStr);
                    break;
                default:
                    log.info(logStr);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            log.warn(ctx.channel().remoteAddress().toString() + " is offline.");
            ctx.channel().close();
        }
    }
}
