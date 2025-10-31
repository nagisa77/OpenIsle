package com.openisle.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author smallclover
 * @since  2025-10-26
 * 缓存key
 */
@Configuration
public class RedisKeyGenerator {

    @Bean("last_reply_generator")
    public KeyGenerator lastReplyGenerator(){
        return (target, method, params) -> {
            String methodName = method.getName();
            return Arrays.stream(params)
                    // 忽略auth的方法参数
                    .filter(p -> !(p instanceof Authentication))
                    .map(p -> {

                        // 将null转化为none
                        if (p == null) return "none";
                        // 固定集合中元素的顺序
                        if (p instanceof Collection<?>) {
                            return ((Collection<?>) p).stream()
                                    .map(String::valueOf)
                                    .sorted()
                                    .collect(Collectors.joining(","));
                        }
                        return p.toString();
                    })
                    .collect(Collectors.joining(":", methodName + ":", ""));
        };
    }
}
