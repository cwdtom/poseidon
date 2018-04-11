package com.github.cwdtom.poseidon.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.github.cwdtom.poseidon.entity.Message;
import org.slf4j.Marker;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * slf4j日志拦截器
 *
 * @author chenweidong
 * @since 1.0.0
 */
public class PoseidonFilter extends TurboFilter {
    /**
     * 日志队列
     */
    public static Queue<Message> queue = new ConcurrentLinkedQueue<>();

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (level.toInt() < Level.INFO_INT || format == null) {
            return FilterReply.NEUTRAL;
        }
        String msg = String.format("[%s] %s", logger.getName(), format);
        // 防止连接断开过程中日志占用过多内存
        Integer maxSize = 1000;
        if (PoseidonFilter.queue.size() < maxSize) {
            // 推送至队列中
            PoseidonFilter.queue.offer(new Message(Level.INFO_INT, msg));
        }
        return FilterReply.ACCEPT;
    }
}
