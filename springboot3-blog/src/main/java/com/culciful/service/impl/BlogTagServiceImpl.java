package com.culciful.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.culciful.pojo.BlogTag;
import com.culciful.service.BlogTagService;
import com.culciful.mapper.BlogTagMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description Service implementation for database operations on the blog_tag table.
* @createDate 2024-01-31 21:38:07
*/
@Service
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag>
    implements BlogTagService{

}




