package cn.com.xcar.controller;

import cn.com.xcar.bean.DefineHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxj
 * @Date 2018-10-22 15:05
 **/
@RestController
@RequestMapping("api")
public class DemoController {

    @Autowired
    private DefineHandler defineHandler;

    @GetMapping("demo")
    public String demo(@RequestParam String param){

        defineHandler.handler();

        return param + System.currentTimeMillis();

    }

}
