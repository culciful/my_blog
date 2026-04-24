package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.BlogTagRelation;
import com.culciful.service.BlogTagRelationService;
import com.culciful.mapper.BlogTagRelationMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【blog_tag_relation(博文标签表)】的数据库操作Service实现
* @createDate 2024-01-31 21:38:07
*/
@Service
public class BlogTagRelationServiceImpl extends ServiceImpl<BlogTagRelationMapper, BlogTagRelation>
    implements BlogTagRelationService{

}




