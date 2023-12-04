package com.atguigu.vo;

import lombok.Data;

import java.util.List;
@Data
public class CategoryVo {
    private Long categoryid;
    private String categoryName;
    private List<CategoryVo> categoryChild;
}
