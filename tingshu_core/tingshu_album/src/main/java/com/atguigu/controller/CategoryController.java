package com.atguigu.controller;

import com.atguigu.login.TingShuLogin;
import com.atguigu.result.RetVal;
import com.atguigu.util.AuthContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "分类管理")
@RequestMapping("/api/album/category")
public class CategoryController {
    @TingShuLogin(required = true)
    @Operation(summary = "获取全部分类信息")
    @GetMapping("getAllCategoryList")
    public RetVal getAllCategoryList() {
        Long userId = AuthContextHolder.getUserId();
        System.out.println(userId);
        return RetVal.ok();
    }
}
