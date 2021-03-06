package com.yang.blog.util;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yang.blog.dto.ConditionDto;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p> mybatis plus 通用搜索</p>
 *
 * @author : zhengqing
 * @description :
 * @date : 2019/10/12 15:42
 */
public class SearchUtil {

    public static <T> QueryWrapper<T> parseWhereSql(QueryWrapper<T> queryWrapper, String conditionJson) {

        if (!StringUtils.isEmpty(conditionJson)) {
            List<ConditionDto> conditionDtoList = JSON.parseArray(conditionJson, ConditionDto.class);
            if (!conditionDtoList.isEmpty()) {
                for (ConditionDto conditionDto : conditionDtoList) {
                    switch (conditionDto.getType()) {
                        case "eq":
                            if (!StringUtils.isEmpty(conditionDto.getValue())) {
                                queryWrapper.eq(conditionDto.getColumn(), conditionDto.getValue());
                            }
                            break;
                        case "ne":
                            if (!StringUtils.isEmpty(conditionDto.getValue())) {
                                queryWrapper.ne(conditionDto.getColumn(), conditionDto.getValue());
                            }
                            break;
                        case "like":
                            queryWrapper.like(conditionDto.getColumn(), conditionDto.getValue());
                            break;
                        case "leftlike":
                            queryWrapper.likeLeft(conditionDto.getColumn(), conditionDto.getValue());
                            break;
                        case "rightlike":
                            queryWrapper.likeRight(conditionDto.getColumn(), conditionDto.getValue());
                            break;
                        case "notlike":
                            queryWrapper.notLike(conditionDto.getColumn(), conditionDto.getValue());
                            break;
                        case "gt":
                            if (!StringUtils.isEmpty(conditionDto.getValue())) {
                                queryWrapper.gt(conditionDto.getColumn(), conditionDto.getValue());
                            }
                            break;
                        case "lt":
                            if (!StringUtils.isEmpty(conditionDto.getValue())) {
                                queryWrapper.lt(conditionDto.getColumn(), conditionDto.getValue());
                            }
                            break;
                        case "ge":
                            if (!StringUtils.isEmpty(conditionDto.getValue())) {
                                queryWrapper.ge(conditionDto.getColumn(), conditionDto.getValue());
                            }
                            break;
                        case "le":
                            if (!StringUtils.isEmpty(conditionDto.getValue())) {
                                queryWrapper.le(conditionDto.getColumn(), conditionDto.getValue());
                            }
                            break;
                    }
                }
            }
        }
        return queryWrapper;
    }
}
