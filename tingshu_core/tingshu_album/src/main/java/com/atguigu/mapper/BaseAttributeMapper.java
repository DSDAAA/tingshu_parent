package com.atguigu.mapper;

import com.atguigu.entity.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 属性表 Mapper 接口
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {

    List<BaseAttribute> getPropertyByCategoryId(Long category1Id);
}
