package com.yang.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yang.blog.entity.Article;
import com.yang.blog.entity.Tag;
import com.yang.blog.mapper.ArticleMapper;
import com.yang.blog.mapper.TagMapper;
import com.yang.blog.search.entity.EsArticle;
import com.yang.blog.search.entity.EsCategory;
import com.yang.blog.search.repository.EsArticleRepository;
import com.yang.blog.service.IArticleService;
import com.yang.blog.service.ITagService;
import com.yang.blog.util.ResponseData;
import com.yang.blog.util.VerificationJudgementUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService {

    @Autowired
    private ITagService tagService;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private EsArticleRepository esArticleRepository;


    /**
     * 新增
     *
     * @param article
     * @param bindingResult
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseData<Object> add(Article article, BindingResult bindingResult) {
        ArrayList<String> errorList = VerificationJudgementUtils.hasErrror(bindingResult);
        if (!errorList.isEmpty()) {
            return ResponseData.fail(2, "error", errorList);
        }

        try {
            //把tag转化为list
            String tag = article.getTag();
            String[] tagArr = tag.split(",");
            List<String> tagList = new ArrayList<>(tagArr.length);
            Collections.addAll(tagList, tagArr);

            QueryWrapper<Tag> query = new QueryWrapper<>();
            int tagSize = tagList.size();
            if (tagSize == 1) {
                query.eq("name", tagList.get(0));
            } else {
                for (int i = 0; i < tagSize; i++) {
                    if (i == (tagSize - 1)) {
                        query.eq("name", tagList.get(i));
                    } else {
                        query.eq("name", tagList.get(i)).or();
                    }
                }
            }
            //获得数据库查询出来的名字
            List<Object> dbTagByName = tagService.listMaps(query.select("name")).stream().map(map -> map.get("name")).collect(toList());
            //交集
            List<String> intersection = tagList.stream().filter(dbTagByName::contains).collect(toList());
            // 差集
            List<String> diff = tagList.stream().filter(item -> !dbTagByName.contains(item)).collect(toList());

            //交集的+1
            intersection.forEach(s -> tagMapper.setIncByNumber(s));

            //差集的新增
            List<Tag> addTagList = new ArrayList<>();
            diff.forEach(s -> {
                Tag tag1 = new Tag();
                tag1.setNumber(1);
                tag1.setName(s);
                addTagList.add(tag1);
            });
            tagService.saveBatch(addTagList);
            //新增文章
            save(article);

            //获得主键，和创建时间
            //插入elasticsearch里
            EsArticle esArticle = new EsArticle();
            EsCategory esCategory = new EsCategory();
            esCategory.setId(article.getCategoryId());
            esCategory.setTitle(article.getCategoryTitle());

            esArticle.setId(Long.valueOf(article.getId()));
            esArticle.setTitle(article.getTitle());
            esArticle.setCategory(esCategory);
            esArticle.setImage(article.getImage());
            esArticle.setTag(tagList);
            esArticle.setCreateTime(article.getCreateTime());
            esArticleRepository.save(esArticle);
            return ResponseData.success();
        } catch (Exception e) {
            //手动回滚，处理try失效问题
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseData.fail(e.getMessage());
        }

    }
}