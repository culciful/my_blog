package com.culciful.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.param.EmailExistParam;
import com.culciful.pojo.UserInfo;
import com.culciful.service.UserInfoService;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.utils.R;
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
* @description 针对表【user_info(用户基本信息表)】的数据库操作Service实现
* @createDate 2024-01-31 21:38:07
*/
@Service
@Slf4j
@Transactional
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService{
    @Resource
    private UserInfoMapper userInfoMapper;
    // @Resource
    // private DBUserDetailsManager dbUserDetailsManager;
    // @Override
    // public void saveUserDetails(UserInfo userInfo) {
    //     UserDetails userDetails = User.withDefaultPasswordEncoder()
    //             .username(userInfo.getUsername()) //自定义用户名
    //             .password(userInfo.getPassword()) //自定义密码
    //             .build();
    //     dbUserDetailsManager.createUser(userInfo);
    // }

    @Override
    public R checkEmailExist(EmailExistParam emailExistParam) {
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getEmail, emailExistParam.getEmail());
        Long count = userInfoMapper.selectCount(queryWrapper);
        log.info("UserInfoServiceImpl.checkEmailExist is over, result: {}", count);
        Map<String, Integer> data = new HashMap<>();
        data.put("isExisted", count > 0 ? 1 : 0);
        return R.ok(data);
    }
}




