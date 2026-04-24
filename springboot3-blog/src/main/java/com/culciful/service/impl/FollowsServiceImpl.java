package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.UserFollow;
import com.culciful.service.FollowsService;
import com.culciful.mapper.UserFollowMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【follows(用户关注关系表)】的数据库操作Service实现
* @createDate 2024-01-31 13:49:22
*/
@Service
public class FollowsServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow>
    implements FollowsService{

}




