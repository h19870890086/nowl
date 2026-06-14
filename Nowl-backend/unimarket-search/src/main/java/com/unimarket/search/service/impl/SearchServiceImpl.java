package com.unimarket.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.goods.dto.GoodsQueryDTO;
import com.unimarket.module.goods.entity.CollectionRecord;
import com.unimarket.module.goods.mapper.CollectionRecordMapper;
import com.unimarket.module.goods.entity.ItemCategory;
import com.unimarket.module.goods.mapper.ItemCategoryMapper;
import com.unimarket.module.goods.service.GoodsService;
import com.unimarket.module.goods.vo.GoodsVO;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.search.document.GoodsDocument;
import com.unimarket.search.dto.SearchRequestDTO;
import com.unimarket.search.service.SearchService;
import com.unimarket.search.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisCache redisCache;
    private final CollectionRecordMapper collectionRecordMapper;
    private final UserInfoMapper userInfoMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final GoodsService goodsService;

    private static final String SEARCH_HISTORY_KEY = "search:history:";
    private static final String HOT_WORD_KEY = "search:hot:";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int MAX_QUERY_SIZE = 100;
    private static final long HOT_WORD_EXPIRE_DAYS = 7;

    @Override
    public PageResult<SearchResultVO> search(SearchRequestDTO request, Long userId) {
        normalizePageQuery(request);

        // 主路径：ES 搜索
        try {
            NativeQuery query = buildSearchQuery(request);
            SearchHits<GoodsDocument> searchHits = elasticsearchOperations.search(query, GoodsDocument.class);

            if (searchHits.getTotalHits() > 0) {
                List<SearchResultVO> resultList = searchHits.getSearchHits().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
                fillCategoryInfo(resultList);
                fillSchoolCampusInfo(resultList);
                fillCollectedStatus(resultList, userId);
                return new PageResult<>(searchHits.getTotalHits(), resultList);
            }

            // ES 返回空结果：降级到 MySQL 查询，确保新发布的商品在 ES 未及时同步时仍可见
            log.info("ES 搜索返回空结果，降级到 MySQL 查询: keyword={}, schoolCode={}", request.getKeyword(), request.getSchoolCode());
        } catch (Exception e) {
            // ES 异常时降级到 MySQL，保证服务可用性
            log.error("ES 搜索异常，降级到 MySQL 查询: keyword={}, schoolCode={}", request.getKeyword(), request.getSchoolCode(), e);
        }

        // 降级路径：直接查 MySQL
        return fallbackToMysql(request, userId);
    }

    /**
     * ES 不可用或返回空时，回退到 MySQL 直接查询
     */
    private PageResult<SearchResultVO> fallbackToMysql(SearchRequestDTO request, Long userId) {
        GoodsQueryDTO goodsQuery = new GoodsQueryDTO();
        goodsQuery.setKeyword(request.getKeyword());
        goodsQuery.setCategoryId(request.getCategoryId());
        goodsQuery.setSchoolCode(request.getSchoolCode());
        goodsQuery.setCampusCode(request.getCampusCode());
        goodsQuery.setMinPrice(request.getMinPrice());
        goodsQuery.setMaxPrice(request.getMaxPrice());
        goodsQuery.setTradeStatus(request.getTradeStatus());
        goodsQuery.setSellerId(request.getSellerId());
        goodsQuery.setSortType(request.getSortType());
        goodsQuery.setPageNum(request.getPageNum());
        goodsQuery.setPageSize(request.getPageSize());

        PageResult<GoodsVO> goodsResult = goodsService.list(goodsQuery, userId);

        // 将 GoodsVO 转换为 SearchResultVO
        List<SearchResultVO> resultList = new ArrayList<>();
        if (goodsResult.getRecords() != null) {
            resultList = goodsResult.getRecords().stream().map(goods -> {
                SearchResultVO vo = new SearchResultVO();
                vo.setProductId(goods.getProductId());
                vo.setTitle(goods.getTitle());
                vo.setCategoryId(goods.getCategoryId());
                vo.setCategoryName(goods.getCategoryName());
                vo.setPrice(goods.getPrice());
                vo.setSellerId(goods.getSellerId());
                vo.setSellerName(goods.getSellerName());
                vo.setSellerAvatar(goods.getSellerAvatar());
                vo.setSchoolCode(goods.getSchoolCode());
                vo.setCampusCode(goods.getCampusCode());
                vo.setSchoolName(goods.getSchoolName());
                vo.setCampusName(goods.getCampusName());
                vo.setTradeStatus(goods.getTradeStatus());
                vo.setImage(goods.getImage());
                vo.setCollectCount(goods.getCollectCount());
                vo.setCreateTime(goods.getCreateTime());
                vo.setIsCollected(goods.getIsCollected());
                return vo;
            }).collect(Collectors.toList());
        }

        return new PageResult<>(goodsResult.getTotal(), resultList);
    }
    /**
     * 构建搜索查询
     */
    private NativeQuery buildSearchQuery(SearchRequestDTO request) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder();
        // 构建布尔查询
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        // 关键词搜索
        if (StrUtil.isNotBlank(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            boolean enableFuzziness = isAsciiAlphaNumeric(keyword) && keyword.length() <= 16;
            // 多字段匹配：标题权重最高，分类名次之，描述权重最低减少噪音
            List<String> searchFields = new ArrayList<>();
            searchFields.add("title^5");          // 标题匹配权重最高
            searchFields.add("categoryName^2");   // 分类名辅助匹配
            if (enableFuzziness) {
                searchFields.add("title.pinyin^2");
                searchFields.add("description");  // 英数搜索加描述字段
            }
            // 中文搜索不加description字段，避免常见词在无关商品描述中命中产生噪音
            // 商品标题+分类名已足够精准匹配中文搜索意图
            boolQuery.must(Query.of(q -> q
                .multiMatch(m -> {
                    m.query(keyword).fields(searchFields);
                    if (enableFuzziness) {
                        m.fuzziness("AUTO");
                    }
                    return m;
                })
            ));
        }
        // 过滤条件
        // 分类筛选：支持单个分类ID或多个分类ID
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            // 多个分类ID（一级分类下的所有子分类）
            List<FieldValue> categoryValues = request.getCategoryIds().stream()
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolQuery.filter(Query.of(q -> q
                .terms(t -> t.field("categoryId").terms(v -> v.value(categoryValues)))
            ));
        } else if (request.getCategoryId() != null) {
            // 单个分类ID（二级分类精确匹配）
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("categoryId").value(request.getCategoryId()))
            ));
        }
        // 学校筛选
        if (StrUtil.isNotBlank(request.getSchoolCode())) {
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("schoolCode").value(request.getSchoolCode()))
            ));
        }
        // 校区筛选
        if (StrUtil.isNotBlank(request.getCampusCode())) {
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("campusCode").value(request.getCampusCode()))
            ));
        }
        // 价格区间
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            boolQuery.filter(Query.of(q -> q
                .range(r -> {
                    r.field("price");
                    if (request.getMinPrice() != null) {
                        r.gte(co.elastic.clients.json.JsonData.of(request.getMinPrice()));
                    }
                    if (request.getMaxPrice() != null) {
                        r.lte(co.elastic.clients.json.JsonData.of(request.getMaxPrice()));
                    }
                    return r;
                })
            ));
        }
        // 交易状态（默认只查在售）
        Integer tradeStatus = request.getTradeStatus() != null ? request.getTradeStatus() : 0;
        boolQuery.filter(Query.of(q -> q
            .term(t -> t.field("tradeStatus").value(tradeStatus))
        ));
        // 审核状态（只查已通过）
        boolQuery.filter(Query.of(q -> q
            .terms(t -> t
                .field("reviewStatus")
                .terms(v -> v.value(Arrays.asList(
                    FieldValue.of(1),
                    FieldValue.of(2)
                )))
            )
        ));
        // 卖家筛选
        if (request.getSellerId() != null) {
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("sellerId").value(request.getSellerId()))
            ));
        }
        queryBuilder.withQuery(Query.of(q -> q.bool(boolQuery.build())));
        // 排序
        applySorting(queryBuilder, request.getSortType(), StrUtil.isNotBlank(request.getKeyword()));
        // 分页
        queryBuilder.withPageable(PageRequest.of(
            request.getPageNum() - 1,
            request.getPageSize()
        ));
        // 高亮
        if (StrUtil.isNotBlank(request.getKeyword())) {
            queryBuilder.withHighlightQuery(new HighlightQuery(
                new Highlight(
                    HighlightParameters.builder()
                        .withPreTags("<em>")
                        .withPostTags("</em>")
                        .build(),
                    List.of(new HighlightField("title"))
                ),
                GoodsDocument.class
            ));
        }
        return queryBuilder.build();
    }

    /**
     * 应用排序
     */
    private void applySorting(NativeQueryBuilder queryBuilder, Integer sortType, boolean hasKeyword) {
        if (sortType == null) sortType = 0;

        switch (sortType) {
            case 1: // 最新
                queryBuilder.withSort(Sort.by(
                    Sort.Order.desc("createTime"),
                    Sort.Order.desc("productId")
                ));
                break;
            case 2: // 价格升序
                queryBuilder.withSort(Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createTime")));
                break;
            case 3: // 价格降序
                queryBuilder.withSort(Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createTime")));
                break;
            case 4: // 热度
                queryBuilder.withSort(Sort.by(
                    Sort.Order.desc("hotScore"),
                    Sort.Order.desc("collectCount"),
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("createTime")
                ));
                break;
            default: // 综合排序
                if (hasKeyword) {
                    queryBuilder.withSort(Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc("hotScore"),
                        Sort.Order.desc("collectCount"),
                        Sort.Order.desc("viewCount"),
                        Sort.Order.desc("createTime")
                    ));
                } else {
                    queryBuilder.withSort(Sort.by(
                        Sort.Order.desc("hotScore"),
                        Sort.Order.desc("collectCount"),
                        Sort.Order.desc("viewCount"),
                        Sort.Order.desc("createTime")
                    ));
                }
        }
    }

    /**
     * 转换为VO
     */
    private SearchResultVO convertToVO(SearchHit<GoodsDocument> hit) {
        GoodsDocument doc = hit.getContent();
        SearchResultVO vo = new SearchResultVO();

        vo.setProductId(doc.getProductId());
        vo.setDescription(doc.getDescription());
        vo.setCategoryId(doc.getCategoryId());
        vo.setCategoryName(doc.getCategoryName());
        vo.setPrice(doc.getPrice());
        vo.setSellerId(doc.getSellerId());
        vo.setSellerName(doc.getSellerName());
        vo.setSellerAvatar(doc.getSellerAvatar());
        vo.setSchoolCode(doc.getSchoolCode());
        vo.setCampusCode(doc.getCampusCode());
        vo.setTradeStatus(doc.getTradeStatus());
        vo.setImage(doc.getImage());
        vo.setCollectCount(doc.getCollectCount());
        vo.setViewCount(doc.getViewCount());
        vo.setHotScore(doc.getHotScore());
        vo.setScore(hit.getScore());
        vo.setCreateTime(doc.getCreateTime());

        // 高亮标题
        List<String> highlightTitle = hit.getHighlightFields().get("title");
        if (highlightTitle != null && !highlightTitle.isEmpty()) {
            vo.setTitle(highlightTitle.get(0));
        } else {
            vo.setTitle(doc.getTitle());
        }

        return vo;
    }

    private void fillSchoolCampusInfo(List<SearchResultVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 只有当 ES 文档里缺少 school/campus 时，才回表查卖家信息兜底，避免每次搜索都打 MySQL。
        boolean needSellerFallback = items.stream().anyMatch(item ->
            StrUtil.isBlank(item.getSchoolCode()) || StrUtil.isBlank(item.getCampusCode())
        );
        Map<Long, UserInfo> sellerMap = new HashMap<>();
        if (needSellerFallback) {
            Set<Long> sellerIds = items.stream()
                .filter(item -> StrUtil.isBlank(item.getSchoolCode()) || StrUtil.isBlank(item.getCampusCode()))
                .map(SearchResultVO::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (!sellerIds.isEmpty()) {
                List<UserInfo> sellers = userInfoMapper.selectBatchIds(sellerIds);
                sellerMap = sellers.stream()
                    .collect(Collectors.toMap(UserInfo::getUserId, user -> user, (a, b) -> a));
            }
        }

        Set<String> schoolCodes = new HashSet<>();
        Set<String> campusCodes = new HashSet<>();
        for (SearchResultVO item : items) {
            UserInfo seller = sellerMap.get(item.getSellerId());
            String resolvedSchoolCode = resolveSchoolCode(item.getSchoolCode(), seller);
            String resolvedCampusCode = resolveCampusCode(item.getCampusCode(), seller);
            item.setSchoolCode(resolvedSchoolCode);
            item.setCampusCode(resolvedCampusCode);
            if (StrUtil.isNotBlank(resolvedSchoolCode) && StrUtil.isNotBlank(resolvedCampusCode)) {
                schoolCodes.add(resolvedSchoolCode);
                campusCodes.add(resolvedCampusCode);
            }
        }

        // 优先复用学校/校区缓存（SchoolServiceImpl 已缓存 campus:list:<schoolCode>），命中则无需查询 SchoolInfo 表。
        Map<String, SchoolInfo> schoolMap = new HashMap<>();
        Set<String> unresolvedKeys = new HashSet<>();
        if (!schoolCodes.isEmpty()) {
            for (String schoolCode : schoolCodes) {
                // 复用 unimarket-core 的缓存 key 规范
                List<SchoolInfo> cachedCampuses = redisCache.getCacheList("campus:list:" + schoolCode);
                if (cachedCampuses == null || cachedCampuses.isEmpty()) {
                    continue;
                }
                for (SchoolInfo campus : cachedCampuses) {
                    String key = buildSchoolCampusKey(campus.getSchoolCode(), campus.getCampusCode());
                    if (key != null) {
                        schoolMap.putIfAbsent(key, campus);
                    }
                }
            }
        }

        for (SearchResultVO item : items) {
            String key = buildSchoolCampusKey(item.getSchoolCode(), item.getCampusCode());
            if (key != null && !schoolMap.containsKey(key)) {
                unresolvedKeys.add(key);
            }
        }

        // 缓存未命中的 key 再回表查询一次兜底（只查缺失部分）
        if (!unresolvedKeys.isEmpty()) {
            Set<String> missSchoolCodes = unresolvedKeys.stream()
                .map(k -> k.split("\\|", 2)[0])
                .collect(Collectors.toSet());
            Set<String> missCampusCodes = unresolvedKeys.stream()
                .map(k -> k.split("\\|", 2)[1])
                .collect(Collectors.toSet());
            LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.in(SchoolInfo::getSchoolCode, missSchoolCodes)
                .in(SchoolInfo::getCampusCode, missCampusCodes)
                .eq(SchoolInfo::getStatus, 1);
            List<SchoolInfo> schools = schoolInfoMapper.selectList(schoolWrapper);
            for (SchoolInfo school : schools) {
                String key = buildSchoolCampusKey(school.getSchoolCode(), school.getCampusCode());
                if (key != null) {
                    schoolMap.putIfAbsent(key, school);
                }
            }
        }

        for (SearchResultVO item : items) {
            String schoolCode = item.getSchoolCode();
            String campusCode = item.getCampusCode();
            SchoolInfo schoolInfo = schoolMap.get(buildSchoolCampusKey(schoolCode, campusCode));
            if (schoolInfo != null) {
                item.setSchoolName(schoolInfo.getSchoolName());
                item.setCampusName(schoolInfo.getCampusName());
            } else {
                item.setSchoolName(schoolCode);
                item.setCampusName(campusCode);
            }
        }
    }

    private String resolveSchoolCode(String schoolCode, UserInfo seller) {
        if (StrUtil.isNotBlank(schoolCode)) {
            return schoolCode;
        }
        return seller == null ? null : seller.getSchoolCode();
    }

    private String resolveCampusCode(String campusCode, UserInfo seller) {
        if (StrUtil.isNotBlank(campusCode)) {
            return campusCode;
        }
        return seller == null ? null : seller.getCampusCode();
    }

    private String buildSchoolCampusKey(String schoolCode, String campusCode) {
        if (StrUtil.isBlank(schoolCode) || StrUtil.isBlank(campusCode)) {
            return null;
        }
        return schoolCode + "|" + campusCode;
    }

    private void fillCollectedStatus(List<SearchResultVO> items, Long userId) {
        if (userId == null || items == null || items.isEmpty()) {
            return;
        }
        List<Long> productIds = items.stream()
            .map(SearchResultVO::getProductId)
            .distinct()
            .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<CollectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionRecord::getUserId, userId)
            .in(CollectionRecord::getProductId, productIds);
        List<CollectionRecord> records = collectionRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return;
        }
        Set<Long> collectedIds = records.stream()
            .map(CollectionRecord::getProductId)
            .collect(Collectors.toSet());
        for (SearchResultVO item : items) {
            item.setIsCollected(collectedIds.contains(item.getProductId()));
        }
    }

    /**
     * 回填分类名称（ES文档中categoryName可能缺失，从MySQL批量补充）
     */
    private void fillCategoryInfo(List<SearchResultVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 找出categoryName为空的条目
        List<Integer> missingCategoryIds = items.stream()
            .filter(item -> StrUtil.isBlank(item.getCategoryName()))
            .map(SearchResultVO::getCategoryId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        if (missingCategoryIds.isEmpty()) {
            return;
        }

        List<ItemCategory> categories = itemCategoryMapper.selectBatchIds(missingCategoryIds);
        if (categories.isEmpty()) {
            return;
        }

        Map<Integer, String> categoryNameMap = categories.stream()
            .collect(Collectors.toMap(ItemCategory::getCategoryId, ItemCategory::getCategoryName, (a, b) -> a));

        for (SearchResultVO item : items) {
            if (StrUtil.isBlank(item.getCategoryName()) && item.getCategoryId() != null) {
                String name = categoryNameMap.get(item.getCategoryId());
                if (name != null) {
                    item.setCategoryName(name);
                }
            }
        }
    }

    @Override
    public List<String> suggest(String keyword, int size) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        int safeSize = clamp(size, 1, MAX_QUERY_SIZE);

        // 使用前缀查询实现简单的搜索建议
        NativeQuery query = NativeQuery.builder()
            .withQuery(Query.of(q -> q
                .prefix(p -> p
                    .field("title.keyword")
                    .value(keyword)
                )
            ))
            .withPageable(PageRequest.of(0, safeSize))
            .build();

        SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class);

        return hits.getSearchHits().stream()
            .map(hit -> hit.getContent().getTitle())
            .distinct()
            .limit(safeSize)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getHotWords(String schoolCode, int size) {
        int safeSize = clamp(size, 1, MAX_QUERY_SIZE);
        String key = StrUtil.isNotBlank(schoolCode)
            ? HOT_WORD_KEY + schoolCode
            : HOT_WORD_KEY + "all";

        Set<String> hotWords = stringRedisTemplate.opsForZSet()
            .reverseRange(key, 0, safeSize - 1);

        return hotWords != null ? new ArrayList<>(hotWords) : Collections.emptyList();
    }

    @Override
    public List<String> getSearchHistory(Long userId, int size) {
        int safeSize = clamp(size, 1, MAX_QUERY_SIZE);
        String key = SEARCH_HISTORY_KEY + userId;
        List<String> history = stringRedisTemplate.opsForList().range(key, 0, safeSize - 1);
        return history != null ? history : Collections.emptyList();
    }

    @Override
    public void recordSearchHistory(Long userId, String keyword) {
        if (userId == null || StrUtil.isBlank(keyword)) {
            return;
        }

        String key = SEARCH_HISTORY_KEY + userId;

        // 移除已存在的相同关键词
        stringRedisTemplate.opsForList().remove(key, 0, keyword);

        // 添加到列表头部
        stringRedisTemplate.opsForList().leftPush(key, keyword);

        // 保持列表长度
        stringRedisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);

        // 设置过期时间（30天）
        stringRedisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    @Override
    public void clearSearchHistory(Long userId) {
        String key = SEARCH_HISTORY_KEY + userId;
        stringRedisTemplate.delete(key);
    }

    @Override
    public void incrementHotWord(String keyword, String schoolCode) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }

        // 全局热搜
        String globalKey = HOT_WORD_KEY + "all";
        stringRedisTemplate.opsForZSet().incrementScore(globalKey, keyword, 1);
        stringRedisTemplate.expire(globalKey, HOT_WORD_EXPIRE_DAYS, TimeUnit.DAYS);

        // 学校热搜
        if (StrUtil.isNotBlank(schoolCode)) {
            String schoolKey = HOT_WORD_KEY + schoolCode;
            stringRedisTemplate.opsForZSet().incrementScore(schoolKey, keyword, 1);
            stringRedisTemplate.expire(schoolKey, HOT_WORD_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }

    private void normalizePageQuery(SearchRequestDTO request) {
        if (request == null) {
            return;
        }
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        request.setPageNum(clamp(pageNum, 1, Integer.MAX_VALUE));
        request.setPageSize(clamp(pageSize, 1, MAX_QUERY_SIZE));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    /**
     * 判断关键词是否为纯ASCII字母数字（用于决定是否启用fuzziness）
     */
    private boolean isAsciiAlphaNumeric(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        for (int i = 0; i < keyword.length(); i++) {
            char ch = keyword.charAt(i);
            if (ch >= 128) {
                return false;
            }
            if (!Character.isLetterOrDigit(ch)) {
                return false;
            }
        }
        return true;
    }
}
