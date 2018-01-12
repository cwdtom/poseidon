package com.github.cwdtom.poseidon.socket;

import com.github.cwdtom.poseidon.filter.PoseidonFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * 发送日志消息
 *
 * @author chenweidong
 * @since 1.0.0
 */
@AllArgsConstructor
@Slf4j
public class PoseidonSend implements Runnable {
    private String ip;
    private Integer port;

    @Override
    public void run() {
        try {
            Socket socket = new Socket(this.ip, this.port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                // 阻塞
                out.writeUTF(PoseidonFilter.queue.take());
                out.flush();
            }
        } catch (IOException e) {
            log.error("poseidon send log fail", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("poseidon LinkedTransferQueue take fail", e);
        }
    }
}
