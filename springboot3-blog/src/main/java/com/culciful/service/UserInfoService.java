package com.culciful.service;

import com.culciful.param.EmailExistParam;
import com.culciful.pojo.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.culciful.utils.R;

/**
* @author culciful_zy
* @description 针对表【user_info(用户基本信息表)】的数据库操作Service
* @createDate 2024-01-31 21:38:07
*/
public interface UserInfoService extends IService<UserInfo> {

    // void saveUserDetails(UserInfo userInfo);

    /**
     * 检查邮箱是否已存在
     * @param emailExistParam
     * @return
     */
    R checkEmailExist(EmailExistParam emailExistParam);
}
