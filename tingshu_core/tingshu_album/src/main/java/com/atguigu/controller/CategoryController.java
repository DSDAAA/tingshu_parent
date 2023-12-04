package com.atguigu.controller;

import com.atguigu.entity.BaseAttribute;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.CategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @Autowired
    private BaseCategoryViewService baseCategoryViewService;
    @Autowired
    private BaseAttributeMapper propertyKeyMapper;

    @TingShuLogin(required = true)
    @Operation(summary = "获取全部分类信息")
    @GetMapping("getAllCategoryList")
    public RetVal getAllCategoryList() {
        List<CategoryVo> categoryVoList = baseCategoryViewService.getAllCategoryList();
        return RetVal.ok(categoryVoList);
    }

    @Operation(summary = "根据一级分类id查询分类属性信息")
    @GetMapping("getPropertyByCategory1Id/{category1Id}")
    public RetVal getPropertyByCategory1Id(@PathVariable Long category1Id) {
        List<BaseAttribute> categoryPropertyList = propertyKeyMapper.getPropertyByCategoryId(category1Id);
        return RetVal.ok(categoryPropertyList);
    }
}
