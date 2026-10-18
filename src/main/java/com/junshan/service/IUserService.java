package com.junshan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.junshan.dto.LoginFormDTO;
import com.junshan.dto.Result;
import com.junshan.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 小君
 * @since 2025-11
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();
}
