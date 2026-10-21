package com.luoxianjun.community.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.user.dto.request.LoginRequest;
import com.luoxianjun.community.module.user.dto.request.RegisterRequest;
import com.luoxianjun.community.module.user.dto.response.LoginResponse;
import com.luoxianjun.community.module.user.dto.response.UserVO;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.module.user.service.UserService;
import com.luoxianjun.community.security.JwtTokenProvider;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        Long exist = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (exist != null && exist > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册: {}", request.username());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (user == null) {
            throw new BizException(ErrorCode.PASSWORD_WRONG);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_WRONG);
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getRole());
        return new LoginResponse(user.getId(), user.getNickname(), user.getRole(), token);
    }

    @Override
    public UserVO currentUser() {
        Long uid = SecurityUtil.currentUserId();
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return UserVO.from(user);
    }

    @Override
    public PageResult<UserVO> pageUsers(long current, long size) {
        IPage<User> page = userMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        return PageResult.of(
                page.getRecords().stream().map(UserVO::from).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }
}
