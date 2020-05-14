package com.glamrock.licenses.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceConfig {

    // @Value 로 프로퍼티 직접 읽기
    @Value("${example.property}")
    private String exampleProperty;

    @Value("${redis.server}")
    private String redisServer="";

    @Value("${redis.port}")
    private String redisPort="";

    public String getExampleProperty() {
        return exampleProperty;
    }

    public String getRedisServer(){
        return redisServer;
    }

    public Integer getRedisPort(){
        return new Integer( redisPort ).intValue();
    }
}
