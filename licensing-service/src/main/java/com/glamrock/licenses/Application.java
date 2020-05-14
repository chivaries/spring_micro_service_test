package com.glamrock.licenses;

import com.glamrock.licenses.config.ServiceConfig;
import com.glamrock.licenses.events.models.OrganizationChangeModel;
import com.glamrock.licenses.utils.UserContextInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@SpringBootApplication
// discovery client 사용시
@EnableDiscoveryClient
// 스프링 부트 액추에이터 로 /refresh 엔드 포인트를 사용해 애플리케이션 구성정보를 다시 읽어올 수 있음
@RefreshScope
// feign client 사용시
@EnableFeignClients
@EnableCircuitBreaker
@EnableBinding(Sink.class)
// spring cloud 및 spring security 에 해당 서비스가 접근 보호 자원임을 알림
@EnableResourceServer
public class Application {
    @Autowired
    private ServiceConfig serviceConfig;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    // ribbon 지원 RestTemplate 사용
    @LoadBalanced
    @Bean
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        List interceptors = restTemplate.getInterceptors();

        if (interceptors == null) {
            restTemplate.setInterceptors(Collections.singletonList(new UserContextInterceptor()));
        } else {
            interceptors.add(new UserContextInterceptor());
            restTemplate.setInterceptors(interceptors);
        }
        return restTemplate;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(serviceConfig.getRedisServer());
        jedisConnectionFactory.setPort(serviceConfig.getRedisPort());
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }

    /*
        @Bean
        public OAuth2RestTemplate restTemplate(UserInfoRestTemplateFactory factory) {
            return factory.getUserInfoRestTemplate();
        }
    */
    @StreamListener(Sink.INPUT)
    public void loggerSink(OrganizationChangeModel orgChange) {
        logger.debug("Received an event for organization id {}", orgChange.getOrganizationId());
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
