package com.atguigu.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.AlbumFeignClient;
import com.atguigu.CategoryFeignClient;
import com.atguigu.UserFeignClient;
import com.atguigu.constant.RedisConstant;
import com.atguigu.entity.*;
import com.atguigu.query.AlbumIndexQuery;
import com.atguigu.repository.AlbumRepository;
import com.atguigu.repository.SuggestRepository;
import com.atguigu.service.SearchService;
import com.atguigu.util.PinYinUtils;
import com.atguigu.util.SleepUtils;
import com.atguigu.vo.AlbumInfoIndexVo;
import com.atguigu.vo.AlbumSearchResponseVo;
import com.atguigu.vo.AlbumStatVo;
import com.atguigu.vo.UserInfoVo;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private SuggestRepository suggestRepository;
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
        //专辑自动补全的内容
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        suggestIndex.setTitle(albumInfo.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{albumInfo.getAlbumTitle()}));
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfo.getAlbumTitle())}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfo.getAlbumTitle())}));
        suggestRepository.save(suggestIndex);
        //专辑主播名称自动补全
        if (!StringUtils.isEmpty(albumInfoIndex.getAnnouncerName())) {
            SuggestIndex announcerSuggestIndex = new SuggestIndex();
            announcerSuggestIndex.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            announcerSuggestIndex.setTitle(albumInfoIndex.getAnnouncerName());
            announcerSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAnnouncerName()}));
            announcerSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAnnouncerName())}));
            announcerSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAnnouncerName())}));
            suggestRepository.save(announcerSuggestIndex);
        }
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

    @SneakyThrows
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        //1.生成DSL语句
        SearchRequest request = buildQueryDsl(albumIndexQuery);
        //2.实现对DSL语句的调用
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(request, AlbumInfoIndex.class);
        //3.对结果进行解析
        AlbumSearchResponseVo responseVo = parseSearchResult(response);
        //4.设置其他参数
        responseVo.setPageNo(albumIndexQuery.getPageNo());
        responseVo.setPageSize(albumIndexQuery.getPageSize());
        //5.设置总页数
        long totalPages = (responseVo.getTotal() + responseVo.getPageSize() - 1) / responseVo.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }

    @SneakyThrows
    @Override
    public HashSet<String> autoCompleteSuggest(String keyword) {
        Suggester suggester = new Suggester.Builder()
                .suggesters("suggestionKeyword", s -> s
                        .prefix(keyword)
                        .completion(c -> c.field("keyword")))
                .suggesters("suggestionKeywordPinyin", s -> s
                        .prefix(keyword)
                        .completion(c -> c.field("keywordPinyin")))
                .suggesters("suggestionKeywordSequence", s -> s
                        .prefix(keyword)
                        .completion(c -> c.field("keywordSequence"))).build();
        System.out.println(suggester.toString());
        SearchResponse<SuggestIndex> suggestResponse = elasticsearchClient.search(s -> s
                .index("suggestinfo")
                .suggest(suggester), SuggestIndex.class);
        //2.解析自动补全结果
        HashSet<String> suggestSet = analysisResponse(suggestResponse);
        //3.如果上面自动补全的比较少
        if (suggestSet.size() < 5) {
            SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(s -> s.index("suggestinfo")
                            .size(10)
                            .query(q -> q.match(m -> m.field("title").query(keyword)))
                    , SuggestIndex.class);
            List<Hit<SuggestIndex>> suggestHitList = searchResponse.hits().hits();
            for (Hit<SuggestIndex> suggestHit : suggestHitList) {
                suggestSet.add(suggestHit.source().getTitle());
                int size = suggestSet.size();
                //自动补全不要超过10个
                if (size > 10) break;
            }
        }
        return suggestSet;
    }

    //    @Autowired
//    public ThreadPoolExecutor myPoolExecutor;
//    @Override
//    public Map<String, Object> getAlbumDetail(Long albumId) {
//        Map<String, Object> retMap = new HashMap<>();
//        System.out.println(Thread.currentThread().getName());
//        //这里我们先不对异常进行处理
//        CompletableFuture<AlbumInfo> albumFuture = CompletableFuture.supplyAsync(() -> {
//            //a.专辑基本信息 已写
//            AlbumInfo albumInfo = albumFeignClient.getAlbumInfoById(albumId).getData();
//            retMap.put("albumInfo", albumInfo);
//            System.out.println(Thread.currentThread().getName());
//            return albumInfo;
//        },myPoolExecutor);
//        CompletableFuture<Void> statFuture = CompletableFuture.runAsync(() -> {
//            //b.专辑统计信息 未写
//            AlbumStatVo albumStatInfo = albumFeignClient.getAlbumStatInfo(albumId);
//            retMap.put("albumStatVo", albumStatInfo);
//            System.out.println(Thread.currentThread().getName());
//        },myPoolExecutor);
//        CompletableFuture<Void> categoryViewFuture = albumFuture.thenAcceptAsync(albumInfo -> {
//            //c.专辑分类信息 已写
//            BaseCategoryView categoryView = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
//            retMap.put("baseCategoryView", categoryView);
//            System.out.println(Thread.currentThread().getName());
//        },myPoolExecutor);
//        CompletableFuture<Void> announcerFuture = albumFuture.thenAcceptAsync(albumInfo -> {
//            //d.用户基本信息 已写
//            UserInfoVo userInfoVo = userFeignClient.getUserById(albumInfo.getUserId()).getData();
//            retMap.put("announcer", userInfoVo);
//            System.out.println(Thread.currentThread().getName());
//        },myPoolExecutor);
//        CompletableFuture.allOf(albumFuture, statFuture, categoryViewFuture, announcerFuture).join();
//        return retMap;
//    }
    @Override
    public Map<String, Object> getAlbumDetail(Long albumId) {
        Map<String, Object> retMap = new HashMap<>();
        //a.专辑基本信息 已写
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfoById(albumId).getData();
        retMap.put("albumInfo", albumInfo);
        //b.专辑统计信息 未写
        AlbumStatVo albumStatInfo = albumFeignClient.getAlbumStatInfo(albumId);
        retMap.put("albumStatVo", albumStatInfo);
        //c.专辑分类信息 已写
        BaseCategoryView categoryView = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
        retMap.put("baseCategoryView", categoryView);
        //d.用户基本信息 已写
        UserInfoVo userInfoVo = userFeignClient.getUserById(albumInfo.getUserId()).getData();
        retMap.put("announcer", userInfoVo);
        return retMap;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @SneakyThrows
    @Override
    public void updateRanking() {
        List<BaseCategory1> category1List = categoryFeignClient.getCategory1();
        if (!CollectionUtils.isEmpty(category1List)) {
            for (BaseCategory1 category1 : category1List) {
                String[] rankingTypeList = new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
                for (String rankingType : rankingTypeList) {
                    SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s
                            .index("albuminfo")
                            .size(10)
                            .sort(t -> t.field(xs -> xs.field("playStatNum").order(SortOrder.Desc))), AlbumInfoIndex.class);
                    ArrayList<AlbumInfoIndex> albumInfoIndices = new ArrayList<>();
                    response.hits().hits().stream().forEach(hit -> {
                        AlbumInfoIndex albumInfoIndex = hit.source();
                        albumInfoIndices.add(albumInfoIndex);
                    });
                    //将排行榜西南西放到redis中
                    redisTemplate.boundHashOps(RedisConstant.RANKING_KEY_PREFIX + "").put(rankingType, albumInfoIndices);
                }
            }
        }

    }

//    @Override
//    public Map<String, Object> getAlbumDetail(Long albumId) {
//        Map<String, Object> retMap = new HashMap<>();
//        new Thread(()->{
//            //a.专辑基本信息 已写
//            AlbumInfo albumInfo = albumFeignClient.getAlbumInfoById(albumId).getData();
//            retMap.put("albumInfo",albumInfo);
//        }).start();
//        new Thread(()->{
//            //b.专辑统计信息 未写
//            AlbumStatVo albumStatInfo = albumFeignClient.getAlbumStatInfo(albumId);
//            retMap.put("albumStatVo",albumStatInfo);
//        }).start();
//        new Thread(()->{
//            //c.专辑分类信息 已写
//            BaseCategoryView categoryView = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
//            retMap.put("baseCategoryView",categoryView);
//        }).start();
//        new Thread(()->{
//            //d.用户基本信息 已写
//            UserInfoVo userInfoVo = userFeignClient.getUserById(albumInfo.getUserId()).getData();
//            retMap.put("announcer",userInfoVo);
//        }).start();
//        return retMap;
//    }

    private HashSet<String> analysisResponse(SearchResponse<SuggestIndex> suggestResponse) {
        HashSet<String> suggestSet = new HashSet<>();
        Map<String, List<Suggestion<SuggestIndex>>> suggestMap = suggestResponse.suggest();
        suggestMap.entrySet().stream().forEach(suggestEntry -> {
            List<Suggestion<SuggestIndex>> suggestValueList = suggestEntry.getValue();
            suggestValueList.stream().forEach(suggestValue -> {
                List<String> suggestTitleList = suggestValue.completion().options().stream()
                        .map(m -> m.source().getTitle()).collect(Collectors.toList());
                suggestSet.addAll(suggestTitleList);
            });
        });
        return suggestSet;

    }

    private AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> response) {
        AlbumSearchResponseVo responseVo = new AlbumSearchResponseVo();
        //获取总记录数
        responseVo.setTotal(response.hits().total().value());
        //获取专辑列表信息
        List<Hit<AlbumInfoIndex>> searchAlbumInfoHits = response.hits().hits();
        List<AlbumInfoIndexVo> albumInfoIndexVoList = new ArrayList<>();
        for (Hit<AlbumInfoIndex> searchAlbumInfoHit : searchAlbumInfoHits) {
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            BeanUtils.copyProperties(searchAlbumInfoHit.source(), albumInfoIndexVo);
            //设置高亮
            List<String> albumTitleHightList = searchAlbumInfoHit.highlight().get("albumTitle");
            if (albumTitleHightList != null) {
                albumInfoIndexVo.setAlbumTitle(albumTitleHightList.get(0));
            }
            albumInfoIndexVoList.add(albumInfoIndexVo);
        }
        responseVo.setList(albumInfoIndexVoList);
        return responseVo;
    }

    private SearchRequest buildQueryDsl(AlbumIndexQuery albumIndexQuery) {
        //2.构造一个bool
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        String keyword = albumIndexQuery.getKeyword();
        //3.构造should关键字查询
        if (!StringUtils.isEmpty(keyword)) {
            boolQuery.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            boolQuery.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));
            boolQuery.should(s -> s.match(m -> m.field("announcerName").query(keyword)));
        }
        //4.根据一级分类查询
        Long category1Id = albumIndexQuery.getCategory1Id();
        if (category1Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }
        //4.根据二级分类查询
        Long category2Id = albumIndexQuery.getCategory2Id();
        if (category2Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }
        //4.根据三级分类查询
        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null) {
            boolQuery.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }
        //5.根据分类属性嵌套过滤
        List<String> propertyList = albumIndexQuery.getAttributeList();
        if (!CollectionUtils.isEmpty(propertyList)) {
            for (String property : propertyList) {
                //property数据类似于这种格式-->18:38
                String[] propertySplit = property.split(":");
                if (propertySplit != null && propertySplit.length == 2) {
                    Query nestedQuery = NestedQuery.of(f -> f.path("attributeValueIndexList")
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(propertySplit[0])))
                                    .must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(propertySplit[1])))
                            )))._toQuery();
                    boolQuery.filter(nestedQuery);
                }
            }
        }
        //1.构建最外层的query
        Query query = boolQuery.build()._toQuery();
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        //6.构造分页与高亮
        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index("albuminfo")
                .query(query)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .highlight(h -> h.fields("albumTitle", p -> p
                        .preTags("<font color='red'>")
                        .postTags("</font>")));
        //7.构造排序 order=1:asc
        String order = albumIndexQuery.getOrder();
        String orderFiled = "";
        if (!StringUtils.isEmpty(order)) {
            String[] orderSplit = order.split(":");
            if (orderSplit != null && orderSplit.length == 2) {
                switch (orderSplit[0]) {
                    case "1":
                        orderFiled = "hotScore";
                        break;
                    case "2":
                        orderFiled = "playStatNum";
                        break;
                    case "3":
                        orderFiled = "createTime";
                        break;
                }
            }
            String sortType = orderSplit[1];
            String finalOrderFiled = orderFiled;
            searchRequest.sort(s -> s.field(f -> f.field(finalOrderFiled)
                    .order("asc".equals(sortType) ? SortOrder.Asc : SortOrder.Desc)));
        }
        SearchRequest request = searchRequest.build();
        System.out.println("拼接的DSL语句:" + request.toString());
        return request;
    }
}
