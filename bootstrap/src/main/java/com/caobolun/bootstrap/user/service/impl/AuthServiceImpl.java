package com.caobolun.bootstrap.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.caobolun.bootstrap.user.dto.resquest.LoginRequest;
import com.caobolun.bootstrap.user.dto.vo.LoginVO;
import com.caobolun.bootstrap.user.entity.UserDO;
import com.caobolun.bootstrap.user.mapper.UserMapper;
import com.caobolun.bootstrap.user.service.AuthService;
import com.caobolun.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_AVATAR_URL = "https://avatars.githubusercontent.com/u/583231?v=4";

    private final UserMapper userMapper;

    @Override
    public LoginVO login(LoginRequest requestParam) {
        String username = requestParam.getUsername();
        String password = requestParam.getPassword();
        if(StrUtil.isBlank(username) || StrUtil.isBlank(password)){
            throw new ClientException("用户名或密码不能为空");
        }
        UserDO userDO = findByUsername(username);
        if (userDO == null || !passwordMatches(password, userDO.getPassword())) {
            throw new ClientException("用户名或密码错误");
        }
        if (userDO.getId() == null) {
            throw new ClientException("用户信息异常");
        }
        String loginId = userDO.getId();
        StpUtil.login(loginId);
        String avatar = StrUtil.isBlank(userDO.getAvatar()) ? DEFAULT_AVATAR_URL : userDO.getAvatar();
        return new LoginVO(loginId, userDO.getRole(), StpUtil.getTokenValue(), avatar);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    private UserDO findByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return userMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getUsername, username)
                        .eq(UserDO::getDeleted, 0)
        );
    }

    private boolean passwordMatches(String input, String stored) {
        if (stored == null) {
            return input == null;
        }
        return stored.equals(input);
    }
}
