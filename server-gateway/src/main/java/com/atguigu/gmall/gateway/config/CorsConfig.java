package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){


        CorsConfiguration corsConfiguration=new CorsConfiguration();
        //设置允许的域
        corsConfiguration.addAllowedOrigin("*");
        //设置允许的头信息
        corsConfiguration.addAllowedHeader("*");
        //设置是否允许携带cookie
        corsConfiguration.setAllowCredentials(true);
        //设置允许的请求方式
        corsConfiguration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource configurationSource=new UrlBasedCorsConfigurationSource();

        configurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return  new CorsWebFilter(configurationSource);
    }



}
