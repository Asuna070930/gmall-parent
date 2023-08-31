package com.atguigu.gmall.weball.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

    @GetMapping("/login.html")
    public String toLogin(HttpServletRequest request){
        //获取参数
        String originUrl = request.getParameter("originUrl");
        //设置数据
        request.setAttribute("originUrl",originUrl);

        return "login";
    }
}
