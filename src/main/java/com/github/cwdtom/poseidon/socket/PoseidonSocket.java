package com.github.cwdtom.poseidon.socket;

import com.alibaba.fastjson.JSON;
import com.github.cwdtom.poseidon.PoseidonRegister;
import lombok.AllArgsConstructor;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 监听注册端口
 *
 * @author chenweidong
 * @since 1.0.0
 */
@AllArgsConstructor
public class PoseidonSocket implements Runnable {
    private Integer port;

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            while (true) {
                // 阻塞
                PoseidonRegister.threadPoolExecutor.execute(new HandlerSocket(serverSocket.accept()));
            }
        } catch (IOException e) {
            throw new RuntimeException("poseidon socket port listen fail");
        }
    }

    /**
     * 处理新连接
     */
    @AllArgsConstructor
    private class HandlerSocket implements Runnable {
        private Socket socket;

        @Override
        public void run() {
            try {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                while (true) {
                    // 阻塞
                    PoseidonRegister.threadPoolExecutor.execute(
                            new HandlerMessage(input.readUTF(), socket.getInetAddress().getHostAddress()));
                }
            } catch (IOException e) {
                try {
                    this.socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 处理消息
     */
    @AllArgsConstructor
    private class HandlerMessage implements Runnable {
        private String message;
        private String ip;

        @Override
        public void run() {
            // 处理数据
            JSONObject obj = JSON.parseObject(this.message);
            Logger logger = LoggerFactory.getLogger(obj.getString("class"));
            String message = String.format("%s - [%s] - [%s]",
                    obj.getString("message"), obj.getString("name"), this.ip);
            switch (obj.getString("level")) {
                case "INFO":
                    logger.info(message);
                    break;
                case "DEBUG":
                    logger.debug(message);
                    break;
                case "WARN":
                    logger.warn(message);
                    break;
                case "ERROR":
                    logger.error(message);
                    break;
                default:
                    logger.info(message);
            }
        }
    }
}
