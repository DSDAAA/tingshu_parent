package com.atguigu.mapper;

import com.atguigu.entity.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 属性表 Mapper 接口
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {

    List<BaseAttribute> getPropertyByCategory1Id(@Param("category1Id") Long category1Id);
}
