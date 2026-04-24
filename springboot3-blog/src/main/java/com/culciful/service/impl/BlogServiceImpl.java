package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.Blog;
import com.culciful.service.BlogService;
import com.culciful.mapper.BlogMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【blog(博文表)】的数据库操作Service实现
* @createDate 2024-01-31 21:38:07
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

}




