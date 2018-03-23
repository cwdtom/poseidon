# Poseidon


![Version](https://img.shields.io/badge/version-2.1.1-green.svg)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://opensource.org/licenses/MIT)

## Overview
- 基于spring-boot，采用netty框架在分布式环境下汇聚不同主机的日志文件
- master主机会记录自己的和所有salve机的日志
- 暂时只支持slf4j
- 独立的日志中心(https://github.com/cwdtom/poseidon-center)

## Configuration
- application中添加新配置
    ```properties
    # 以下两个配置只需存在一个，存在port表示本项目为master项目，存在master.host表示本项目为salve项目
    # socket端口地址
    poseidon.port=10001
    # 记录主机地址
    poseidon.master.host=127.0.0.1:10002
    # 尝试重连时间间隔，单位秒
    poseidon.reconnect-interval=10
    ```
    
## Usage
- 添加依赖
    ```xml
    <dependency>
        <groupId>com.github.cwdtom</groupId>
        <artifactId>poseidon</artifactId>
        <version>2.1.1</version>
    </dependency>
    ```

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