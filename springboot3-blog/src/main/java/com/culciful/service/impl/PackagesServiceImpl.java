package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.UserPackage;
import com.culciful.service.PackagesService;
import com.culciful.mapper.UserPackageMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【packages(集合表)】的数据库操作Service实现
* @createDate 2024-01-31 13:49:22
*/
@Service
public class PackagesServiceImpl extends ServiceImpl<UserPackageMapper, UserPackage>
    implements PackagesService{

}




