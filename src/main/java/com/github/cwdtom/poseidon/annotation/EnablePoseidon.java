package com.github.cwdtom.poseidon.annotation;

import com.github.cwdtom.poseidon.PoseidonRegister;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用Hermes
 *
 * @author chenweidong
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({PoseidonRegister.class})
public @interface EnablePoseidon {
}
