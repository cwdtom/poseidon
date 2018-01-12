# Poseidon


![Version](https://img.shields.io/badge/version-1.0.0-green.svg)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://opensource.org/licenses/MIT)

## Overview
- 基于spring-boot，汇聚不同项目的日志文件
- master主机会记录自己的和所有salve机的日志

## Configuration
- application中添加新配置
    ```properties
    # 以下两个配置只需存在一个，存在port表示本项目为master项目，存在master.host表示本项目为salve项目
    # socket端口地址
    poseidon.port=10001
    # 记录主机地址
    poseidon.master.host=127.0.0.1:10002
    ```
    
## Usage
- 启用poseidon，添加注解@EnablePoseidon
    ```java
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
    ```