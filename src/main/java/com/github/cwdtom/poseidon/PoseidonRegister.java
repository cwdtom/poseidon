package com.github.cwdtom.poseidon;

import ch.qos.logback.classic.LoggerContext;
import com.github.cwdtom.poseidon.filter.PoseidonFilter;
import com.github.cwdtom.poseidon.socket.PoseidonSend;
import com.github.cwdtom.poseidon.socket.PoseidonSocket;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册poseidon服务
 *
 * @author chenweidong
 * @since 1.0.0
 */
@Slf4j
public class PoseidonRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 3,
            5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(3),
            new DefaultThreadFactory(3));
    private Environment environment;

    /**
     * 配置默认线程工厂
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private final int MAX_THREAD;
        private final AtomicInteger count = new AtomicInteger(0);

        DefaultThreadFactory(int maxThread) {
            this.MAX_THREAD = maxThread;
        }

        @Override
        public Thread newThread(Runnable r) {
            if(this.count.incrementAndGet() > MAX_THREAD) {
                this.count.decrementAndGet();
                return null;
            }
            return new Thread(r);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        // 添加拦截器
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.addTurboFilter(new PoseidonFilter());
        // 判断是否是master项目
        String port = environment.getProperty("poseidon.port");
        String host = environment.getProperty("poseidon.master.host");
        String ri = environment.getProperty("poseidon.reconnect-interval");
        if (port == null && host != null) {
            // 向master注册自己
            String[] tmp = host.split(":");
            // 间隔时间秒->毫秒，为空时初始化默认10秒
            Long reconnectInterval = ri == null ? 10 * 1000 : Long.parseLong(ri) * 1000;
            PoseidonRegister.threadPoolExecutor.execute(
                    new PoseidonSend(tmp[0], Integer.parseInt(tmp[1]), reconnectInterval));
        } else if (port != null && host == null) {
            // 开放指定端口接收日志
            PoseidonRegister.threadPoolExecutor.execute(new PoseidonSocket(Integer.parseInt(port)));
        } else {
            log.warn("poseidon config is invalid, already exit.");
        }
    }
}
