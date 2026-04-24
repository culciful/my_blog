package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.BlogTag;
import com.culciful.service.BlogTagService;
import com.culciful.mapper.BlogTagMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【blog_tag(标签表)】的数据库操作Service实现
* @createDate 2024-01-31 21:38:07
*/
@Service
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag>
    implements BlogTagService{

}




