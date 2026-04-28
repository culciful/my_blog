package com.culciful.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.param.EmailExistParam;
import com.culciful.pojo.UserInfo;
import com.culciful.service.UserInfoService;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.common.api.R;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
// import org.springframework.security.core.userdetails.User;
// import org.springframework.security.core.userdetails.UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
* @author culciful_zy
* @description Service implementation for user_info table operations.
* @createDate 2024-01-31 21:38:07
*/
@Service
@Slf4j
@Transactional
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService{
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    // @Resource
    // private DBUserDetailsManager dbUserDetailsManager;
    // @Override
    // public void saveUserDetails(UserInfo userInfo) {
    //     UserDetails userDetails = User.withDefaultPasswordEncoder()
    //             .username(userInfo.getUsername()) // custom username
    //             .password(userInfo.getPassword()) // custom password
    //             .build();
    //     dbUserDetailsManager.createUser(userInfo);
    // }

    @Override
    public R checkEmailExist(EmailExistParam emailExistParam) {
        long requestTraceId = snowflakeIdGenerator.nextId();
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, emailExistParam.getEmail());
        Long count = userInfoMapper.selectCount(queryWrapper);
        log.info("UserInfoServiceImpl.checkEmailExist is over, traceId: {}, result: {}", requestTraceId, count);
        Map<String, Integer> data = new HashMap<>();
        data.put("isExisted", count > 0 ? 1 : 0);
        return R.ok(data);
    }
}




