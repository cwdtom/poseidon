package com.github.cwdtom.poseidon.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Marker;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 * slf4j日志拦截器
 *
 * @author chenweidong
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PoseidonFilter extends TurboFilter {
    public static TransferQueue<JSONObject> queue = new LinkedTransferQueue<>();

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (level.toInt() < Level.DEBUG_INT || format == null) {
            return FilterReply.NEUTRAL;
        }
        // 推送至队列中
        JSONObject obj = new JSONObject();
        obj.put("level", level.levelInt);
        obj.put("message", format);
        obj.put("class", logger.getName());
        PoseidonFilter.queue.offer(obj);
        return FilterReply.ACCEPT;
    }
}
