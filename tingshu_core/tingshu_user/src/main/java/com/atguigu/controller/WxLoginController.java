package com.atguigu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * vip服务配置表 前端控制器
 * </p>
 *
 * @author 强哥
 * @since 2023-11-28
 */
@RestController
@RequestMapping("/api/user/wxLogin")
public class WxLoginController {
    @GetMapping("wxLogin/{code}")
    public void wxLogin(@PathVariable String code) {

    }
}
