package com.yang.blog.controller.backend;

import com.yang.blog.entity.Article;
import com.yang.blog.service.IArticleService;
import com.yang.blog.util.QueryCondition;
import com.yang.blog.util.ResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author yangyi
 * @date 2020/6/17 17:01
 * @description：
 */
@Controller
@RequestMapping("admin/content/article")
public class ArticleController {

    @Autowired
    private IArticleService articleService;

    /**
     * 排序分页查询
     *
     * @param queryCondition
     * @return
     */
    @ResponseBody
    @PostMapping("index")
    public Map<String, Object> index(QueryCondition queryCondition) {
        return articleService.queryPage(queryCondition);
    }

    /**
     * 新增
     *
     * @param article
     * @param bindingResult
     * @return
     */
    @ResponseBody
    @PostMapping("add")
    public ResponseData<Object> save(
            @RequestBody
            @Valid Article article,
            BindingResult bindingResult
    ) {
        return articleService.add(article, bindingResult);
    }


    /**
     * 根据id查询数据
     *
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping(value = {"update", "details"})
    public ResponseData<Article> find(@RequestParam(value = "id") Long id) {
        return articleService.findById(id);
    }

    /**
     * id 删除
     *
     * @param ids
     * @return
     */
    @ResponseBody
    @DeleteMapping("delete")
    public ResponseData<Object> delete(@RequestBody List<Long> ids) {
        return articleService.del(ids);
    }


    @GetMapping("index")
    public String indexView() {
        return "backend/content/article/index";
    }

    @GetMapping("add")
    public String saveView() {
        return "backend/content/article/add";
    }

    @GetMapping("update")
    public String editView() {
        return "backend/content/article/edit";
    }

    @GetMapping("details")
    public String detailsView() {
        return "backend/content/article/details";
    }
}
