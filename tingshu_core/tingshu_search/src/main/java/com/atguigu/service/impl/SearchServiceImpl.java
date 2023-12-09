package com.atguigu.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.AlbumFeignClient;
import com.atguigu.CategoryFeignClient;
import com.atguigu.UserFeignClient;
import com.atguigu.entity.*;
import com.atguigu.repository.AlbumRepository;
import com.atguigu.service.SearchService;
import com.atguigu.vo.UserInfoVo;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private CategoryFeignClient categoryFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public void onSaleAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        //根据albumId查询albumInfo信息 已写
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfoById(albumId).getData();
        BeanUtils.copyProperties(albumInfo, albumInfoIndex);
        //根据albumId查询专辑属性值 未写
        List<AlbumAttributeValue> albumPropertyValueList = albumFeignClient.getAlbumPropertyValue(albumId);
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {
            List<AttributeValueIndex> valueIndexList = albumPropertyValueList.stream().map(albumPropertyValue -> {
                AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                BeanUtils.copyProperties(albumPropertyValue, attributeValueIndex);
                return attributeValueIndex;
            }).collect(Collectors.toList());
            albumInfoIndex.setAttributeValueIndexList(valueIndexList);
        }
        //根据三级分类id查询专辑的分类信息 未写
        BaseCategoryView categoryView = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
        albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        //根据用户id查询用户信息
        UserInfoVo userInfo = userFeignClient.getUserById(albumInfo.getUserId()).getData();
        albumInfoIndex.setAnnouncerName(userInfo.getNickname());
        //还需要查询统计表 不查用模拟数据
        int num1 = new Random().nextInt(1000);
        int num2 = new Random().nextInt(100);
        int num3 = new Random().nextInt(50);
        int num4 = new Random().nextInt(300);
        albumInfoIndex.setPlayStatNum(num1);
        albumInfoIndex.setSubscribeStatNum(num2);
        albumInfoIndex.setBuyStatNum(num3);
        albumInfoIndex.setCommentStatNum(num4);
        //计算公式：未实现
        double hotScore = num1 * 0.2 + num2 * 0.3 + num3 * 0.4 + num4 * 0.1;
        albumInfoIndex.setHotScore(hotScore);
        albumRepository.save(albumInfoIndex);
    }

    @Override
    public void offSaleAlbum(Long albumId) {
        albumRepository.deleteById(albumId);
    }

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @SneakyThrows
    @Override
    public List<Map<Object, Object>> getChannelData(Long category1Id) {
        //1.获取三级分类信息
        List<BaseCategory3> category3List = categoryFeignClient.getCategory3ListByCategory1Id(category1Id).getData();
        //2.把三级分类id转换为List<FieldValue>
        List<FieldValue> category3FieldValueList = category3List.stream().map(BaseCategory3::getId)
                //FieldValue.of的作用是把id包装成_kind和_value类型
                .map(x -> FieldValue.of(x)).collect(Collectors.toList());
        //3.搜索ES语句 传递一个searchRequest
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s
                        .index("albuminfo")
                        .query(q -> q
                                .terms(t -> t
                                        .field("category3Id")
                                        .terms(new TermsQueryField.Builder().value(category3FieldValueList).build())
                                )
                        )
                        .aggregations("category3IdAgg", a -> a.terms(t -> t.field("category3Id"))
                                .aggregations("topSixHotScoreAgg", xa -> xa.topHits(t -> t.size(6)
                                        .sort(xs -> xs.field(f -> f.field("hotScore").order(SortOrder.Desc)))))),
                AlbumInfoIndex.class);
        //4.建立三级分类id和三级分类对象的映射
        Map<Long, BaseCategory3> category3Map = category3List.stream().collect(Collectors
                .toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
        //4.解析数据放到对象当中
        Aggregate category3IdAgg = response.aggregations().get("category3IdAgg");
        List<Map<Object, Object>> topAlbumInfoIndexMapList = category3IdAgg.lterms().buckets().array().stream().map(bucket -> {
            Long category3Id = bucket.key();
            Aggregate topSixHotScoreAgg = bucket.aggregations().get("topSixHotScoreAgg");
            List<AlbumInfoIndex> topAlbumInfoIndexList = topSixHotScoreAgg.topHits().hits().hits().stream().map(hit ->
                    JSONObject.parseObject(hit.source().toString(), AlbumInfoIndex.class)
            ).collect(Collectors.toList());
            Map<Object, Object> retMap = new HashMap<>();
            retMap.put("baseCategory3", category3Map.get(category3Id));
            retMap.put("list", topAlbumInfoIndexList);
            return retMap;
        }).collect(Collectors.toList());
        return topAlbumInfoIndexMapList;
    }
}
