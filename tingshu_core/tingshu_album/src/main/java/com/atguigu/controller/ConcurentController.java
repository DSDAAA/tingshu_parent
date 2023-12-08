package com.atguigu.controller;

import com.atguigu.service.BaseCategory1Service;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
@RestController
@Tag(name = "并发管理接口")
@RequestMapping("/api/album")
public class ConcurentController {
    @Autowired
    private BaseCategory1Service baseCategory1Service;

}
