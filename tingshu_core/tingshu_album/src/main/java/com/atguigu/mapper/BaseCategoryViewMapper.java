package com.atguigu.mapper;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.vo.CategoryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * VIEW Mapper 接口
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    List<CategoryVo> getAllCategoryList(@Param("category1Id") Long category1Id);
}
