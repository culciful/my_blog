package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.Blog;
import com.culciful.service.BlogsService;
import com.culciful.mapper.BlogMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【blogs(博文表)】的数据库操作Service实现
* @createDate 2024-01-31 13:49:22
*/
@Service
public class BlogsServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogsService{

}




