package com.culciful.service;

import com.culciful.param.EmailExistParam;
import com.culciful.pojo.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.culciful.common.api.R;

/**
* @author culciful_zy
* @description Service for user_info table operations.
* @createDate 2024-01-31 21:38:07
*/
public interface UserInfoService extends IService<UserInfo> {

    // void saveUserDetails(UserInfo userInfo);

    /**
     * Check whether email already exists.
     */
    R checkEmailExist(EmailExistParam emailExistParam);
}
