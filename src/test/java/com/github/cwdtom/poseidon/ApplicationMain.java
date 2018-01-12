package com.github.cwdtom.poseidon;

import com.github.cwdtom.poseidon.annotation.EnablePoseidon;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试启动类
 *
 * @author chenweidong
 * @since 1.0.0
 */
@SpringBootApplication
@EnablePoseidon
public class ApplicationMain {
    /**
     * main方法
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ApplicationMain.class, args);
    }
}