package com.github.cwdtom.poseidon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试
 *
 * @author chenweidong
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping(method = RequestMethod.GET)
public class Controller {

    /**
     * 获取注册中心列表
     */
    @RequestMapping("/")
    public Integer index() {
        log.info("info");
        log.warn("warn");
        log.error("error");
        return 200;
    }
}
