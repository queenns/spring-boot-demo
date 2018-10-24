package cn.com.xcar.config;

import cn.com.xcar.bean.DefineHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author lxj
 * @Date 2018-10-23 10:48
 **/
@Configuration
public class HandlerConfig {

    @Bean
    public DefineHandler defineHandler(){

        return new DefineHandler();

    }

}
