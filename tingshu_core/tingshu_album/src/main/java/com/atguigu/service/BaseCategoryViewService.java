package com.atguigu.service;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.vo.CategoryVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * VIEW 服务类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
public interface BaseCategoryViewService extends IService<BaseCategoryView> {

    List<CategoryVo> getAllCategoryList();
}
