package com.atguigu.service.impl;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.vo.CategoryVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {

    //@Override
    public List<CategoryVo> getAllCategoryList1() {
        //a.查询所有的分类信息
        List<BaseCategoryView> allCategoryView = list();
        //b.找到所有的一级分类
        Map<Long, List<BaseCategoryView>> category1Map = allCategoryView.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
//        ArrayList<CategoryVo> categoryVoArrayList = new ArrayList<>();
//        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {
//          }
        List<CategoryVo> categoryVoList = category1Map.entrySet().stream().map(category1Entry -> {
            Long category1Id = category1Entry.getKey();
            List<BaseCategoryView> category1List = category1Entry.getValue();
            CategoryVo category1Vo = new CategoryVo();
            category1Vo.setCategoryid(category1Id);
            category1Vo.setCategoryName(category1List.get(0).getCategory1Name());
            //c.找到所有的二级分类
            Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            List<CategoryVo> category1Children = category2Map.entrySet().stream().map(category2Entry -> {
                Long category2Id = category2Entry.getKey();
                List<BaseCategoryView> category2List = category2Entry.getValue();
                CategoryVo category2Vo = new CategoryVo();
                category2Vo.setCategoryid(category2Id);
                category2Vo.setCategoryName(category2List.get(0).getCategory2Name());
                //d.找到所有的三级分类
                Map<Long, List<BaseCategoryView>> category3Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                List<CategoryVo> category2Children = category3Map.entrySet().stream().map(category3Entry -> {
                    Long category3Id = category3Entry.getKey();
                    List<BaseCategoryView> category3List = category3Entry.getValue();
                    CategoryVo category3Vo = new CategoryVo();
                    category3Vo.setCategoryid(category3Id);
                    category3Vo.setCategoryName(category3List.get(0).getCategory3Name());
                    category3Vo.setCategoryChild(null);
                    return category3Vo;
                }).collect(Collectors.toList());
                category2Vo.setCategoryChild(category2Children);
                return category2Vo;
            }).collect(Collectors.toList());
            category1Vo.setCategoryChild(category1Children);
            return category1Vo;
        }).collect(Collectors.toList());

        return categoryVoList;
    }

    @Override
    public List<CategoryVo> getAllCategoryList() {
        return baseMapper.getAllCategoryList();
    }
}
