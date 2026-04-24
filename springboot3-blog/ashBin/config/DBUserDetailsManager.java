package com.culciful.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.UserInfo;
import com.culciful.utils.UserIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/2/1 23:15
 * description:
 */

// @Component
public class DBUserDetailsManager implements UserDetailsManager , UserDetailsPasswordService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        return null;
    }

    @Override
    public void createUser(UserDetails user) {
        /* todo email没存 */
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(user.getUsername());
        userInfo.setPassword(user.getPassword());
        userInfo.setIsDeleted(0);
        userInfoMapper.insert(userInfo);
    }

    public void createUser(UserInfo userInfo) {
        userInfo.setUserId(UserIdGenerator.generate().intValue());
        userInfo.setIsDeleted(0);
        userInfoMapper.insert(userInfo);
    }

    @Override
    public void updateUser(UserDetails user) {

    }

    @Override
    public void deleteUser(String username) {

    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }

    @Override
    public boolean userExists(String username) {
        return false;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(UserInfo::getUsername, username)
                .or()
                .eq(UserInfo::getEmail, username);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if(userInfo == null) {
            throw new UsernameNotFoundException(username);
        } else {
            // 权限列表
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            return new User(
                    userInfo.getUsername(),
                    userInfo.getPassword(),
                    userInfo.getIsDeleted() != 0,
                    true,
                    true,
                    true,
                    authorities
            );
        }
    }
}
