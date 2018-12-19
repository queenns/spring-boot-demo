package cn.com.xcar;

import cn.com.xcar.source.DefineSpringApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author lxj
 * @Date 2018-10-22 15:08
 **/
@ComponentScan(basePackages = "cn.com.xcar")
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {

        // SpringApplication.run(DemoApplication.class, args);

        DefineSpringApplication.run(DemoApplication.class, args);

    }

}
