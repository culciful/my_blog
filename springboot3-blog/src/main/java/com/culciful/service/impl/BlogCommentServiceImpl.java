package com.culciful.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.culciful.pojo.BlogComment;
import com.culciful.service.BlogCommentService;
import com.culciful.mapper.BlogCommentMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description Service implementation for database operations on the blog_comment table.
* @createDate 2024-01-31 21:38:07
*/
@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment>
    implements BlogCommentService{

}




