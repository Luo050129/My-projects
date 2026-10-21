package com.luoxianjun.community.module.user.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.user.dto.request.LoginRequest;
import com.luoxianjun.community.module.user.dto.request.RegisterRequest;
import com.luoxianjun.community.module.user.dto.response.LoginResponse;
import com.luoxianjun.community.module.user.dto.response.UserVO;

/**
 * 用户与鉴权服务。
 */
public interface UserService {

    /** 注册：校验唯一性 + BCrypt 加密 + 默认角色 USER */
    void register(RegisterRequest request);

    /** 登录：校验密码 + 签发 JWT */
    LoginResponse login(LoginRequest request);

    /** 当前登录用户资料 */
    UserVO currentUser();

    /** 分页查询用户（运营后台场景） */
    PageResult<UserVO> pageUsers(long current, long size);
}
