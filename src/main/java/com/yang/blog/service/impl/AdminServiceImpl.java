package com.yang.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yang.blog.dto.QueryParam;
import com.yang.blog.dto.RoleDto;
import com.yang.blog.entity.Admin;
import com.yang.blog.entity.AdminRole;
import com.yang.blog.entity.Role;
import com.yang.blog.mapper.AdminMapper;
import com.yang.blog.service.IAdminRoleService;
import com.yang.blog.service.IAdminService;
import com.yang.blog.service.IRoleService;
import com.yang.blog.util.PasswordUtil;
import com.yang.blog.util.ResponseData;
import com.yang.blog.util.SearchUtil;
import com.yang.blog.util.VerificationJudgementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IAdminRoleService adminRoleService;

    /**
     * 分页查询
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> queryPage(QueryParam params) {
        QueryWrapper<Admin> queryWrapper = SearchUtil.parseWhereSql(new QueryWrapper<>(), params.getCondition());
        int count = count(queryWrapper);
        List<Map<String, Object>> rows = listMaps(
                queryWrapper.select("id", "username", "nick_name", "create_time")
                        .orderByDesc("id")
                        .last(params.getPageSql())

        );
        return ResponseData.list(count, rows);
    }

    /**
     * 添加数据
     *
     * @param admin
     * @param bindingResult
     * @return
     */
    @Override
    public ResponseData<Object> add(Admin admin, BindingResult bindingResult) {
        ArrayList<String> errorList = VerificationJudgementUtil.hasErrror(bindingResult);
        if (!errorList.isEmpty()) {
            return ResponseData.fail(2, "error", errorList);
        }
        try {
            //密码加密
            String salt = PasswordUtil.generateSalt(3, 3);
            admin.setPassword(PasswordUtil.encodePassword(admin.getUsername(), salt));
            admin.setSalt(salt);

            save(admin);
            return ResponseData.success();
        } catch (Exception e) {
            return ResponseData.fail(e.getMessage());
        }
    }

    /**
     * 修改数据
     *
     * @param admin
     * @param bindingResult
     * @return
     */
    @Override
    public ResponseData<Object> update1(Admin admin, BindingResult bindingResult) {
        ArrayList<String> errorList = VerificationJudgementUtil.hasErrror(bindingResult);
        if (!errorList.isEmpty()) {
            return ResponseData.fail(2, "error", errorList);
        }

        String userPwd = admin.getPassword();
        if (userPwd == null || userPwd.isEmpty()) {
            admin.setPassword(null);
        } else {
            //这里要加密的
            String salt = PasswordUtil.generateSalt(3, 3);
            admin.setPassword(PasswordUtil.encodePassword(admin.getUsername(), salt));
            admin.setSalt(salt);
        }
        try {
            updateById(admin);
            return ResponseData.success();
        } catch (Exception e) {
            return ResponseData.fail("数据修改失败！" + e.getMessage());
        }
    }

    /**
     * id查找
     *
     * @param id
     * @return
     */
    @Override
    public ResponseData<Map<String, Object>> find(Long id) {
        Map<String, Object> map = getMap(
                new QueryWrapper<Admin>().
                        eq("id", id).
                        select(
                                "id",
                                "username",
                                "nick_name",
                                "create_time"
                        )
        );
        return ResponseData.success(map);
    }

    /**
     * id查找角色和全部角色
     *
     * @param id
     * @return
     */
    @Override
    public ResponseData<Object> myRole(Long id) {
        List<Map<String, Object>> notOwnedRole;
        //根据id获得自己的角色
        List<RoleDto> myRole = adminMapper.roleFindByIdAdmin(id);

        if (myRole.isEmpty()) {
            notOwnedRole = roleService.all();
        } else {
            //通过stream流取出map中的id
            List<Integer> ids = myRole.stream().map(RoleDto::getId).collect(Collectors.toList());
            notOwnedRole = roleService.listMaps(
                    new QueryWrapper<Role>()
                            .notIn("id", ids)
                            .orderByDesc("id")
                            .select("id", "name", "code")
            );
        }

        HashMap<String, Object> rstMap = new HashMap<>();
        rstMap.put("my_role", myRole);
        rstMap.put("not_owned_role", notOwnedRole);
        return ResponseData.success(rstMap);
    }

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @Override
    public ResponseData<Object> del(List<Long> ids) {
        try {
            removeByIds(ids);
            return ResponseData.success();
        } catch (Exception e) {
            return ResponseData.fail(e.getMessage());
        }
    }

    /**
     * 分配角色
     *
     * @param roleId
     * @param id
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseData<Object> assignRole(String roleId, Long id) {
        try {
            ArrayList<Long> roleIdList = JSON.parseObject(roleId, new TypeReference<ArrayList<Long>>() {
            });
            //获得角色id，获得权限id,先删除角色所有的id，再添加进去
            adminRoleService.remove(new QueryWrapper<AdminRole>().eq("user_id", id));
            ArrayList<AdminRole> adminRoles = new ArrayList<>();
            roleIdList.forEach(rid -> {
                AdminRole adminRole = new AdminRole();
                adminRole.setRoleId(rid.intValue());
                adminRole.setUserId(id.intValue());
                adminRoles.add(adminRole);
            });
            adminRoleService.saveBatch(adminRoles);
            return ResponseData.success();
        } catch (Exception e) {
            //手动回滚，处理try失效问题
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseData.fail(e.getMessage());
        }
    }
}
