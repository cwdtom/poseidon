package com.github.cwdtom.poseidon.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.github.cwdtom.poseidon.entity.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 监听注册端口
 *
 * @author chenweidong
 * @since 1.1.0
 */
@Slf4j
public class PoseidonSocket implements Runnable {
    private Integer port;

    public PoseidonSocket(Integer port) {
        this.port = port;
    }

    @Override
    public void run() {
        this.bind();
    }

    private void bind() {
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
                p.addLast(new Decode());
                p.addLast(new HandlerMessage());
            }
        });
        try {
            ChannelFuture channelFuture = bootstrap.bind(this.port).sync();
            if (channelFuture.isSuccess()) {
                log.info("poseidon is started in " + this.port);
            }
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("netty listen fail", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    /**
     * 解码器
     */
    private class Decode extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
            // 读取4字节int类型头
            int length = byteBuf.readInt();
            // 判断数据包是否到齐
            if (byteBuf.readableBytes() < length) {
                // 读取位置归0
                byteBuf.readerIndex(0);
                return;
            }
            byte[] body = new byte[length];
            byteBuf.readBytes(body);
            list.add(new Message(length, body));
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
            JSONArray arr;
            String inStr = new String(message.getData(), "utf-8");
            try {
                arr = JSON.parseArray(inStr);
            } catch (JSONException ignored) {
                log.info(inStr);
                return;
            }
            int len = arr.size();
            for (int i = 0; i < len; i++) {
                JSONObject obj = arr.getJSONObject(i);
                Logger logger = LoggerFactory.getLogger(obj.getString("class"));
                String logStr = String.format("[%s] - %s", ip, obj.getString("message"));
                switch (obj.getInteger("level")) {
                    case 10000:
                        logger.debug(logStr);
                        break;
                    case 20000:
                        logger.info(logStr);
                        break;
                    case 30000:
                        logger.warn(logStr);
                        break;
                    case 40000:
                        logger.error(logStr);
                        break;
                    default:
                        logger.info(logStr);
                }
            }
        }
    }
}
