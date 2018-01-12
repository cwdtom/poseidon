package com.github.cwdtom.poseidon;

import ch.qos.logback.classic.LoggerContext;
import com.github.cwdtom.poseidon.filter.PoseidonFilter;
import com.github.cwdtom.poseidon.socket.PoseidonSend;
import com.github.cwdtom.poseidon.socket.PoseidonSocket;
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
public class PoseidonRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    public static ThreadPoolExecutor threadPoolExecutor;
    private Environment environment;

    /**
     * 配置默认线程工厂
     */
    static class DefaultThreadFactory implements ThreadFactory {
        private final int MAX_THREAD;
        private final AtomicInteger count = new AtomicInteger(0);

        DefaultThreadFactory(int maxThread) {
            MAX_THREAD = maxThread;
        }

        @Override
        public Thread newThread(Runnable r) {
            int incrementAndGet = count.incrementAndGet();
            if(incrementAndGet > MAX_THREAD) {
                count.decrementAndGet();
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
        loggerContext.addTurboFilter(new PoseidonFilter(environment.getProperty("spring.application.name")));
        // 判断是否是master项目
        String port = environment.getProperty("poseidon.port");
        if (port == null) {
            // 向master注册自己
            String host = environment.getProperty("poseidon.master.host");
            String[] tmp = host.split(":");
            Thread thread = new Thread(new PoseidonSend(tmp[0], Integer.parseInt(tmp[1])));
            thread.start();
        } else {
            // 初始化线程池
            PoseidonRegister.threadPoolExecutor = new ThreadPoolExecutor(50, 100,
                    5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
                    new DefaultThreadFactory(100));
            // 开放指定端口接收日志
            PoseidonRegister.threadPoolExecutor.execute(new PoseidonSocket(Integer.parseInt(port)));
        }
    }
}
