package com.culciful.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.culciful.pojo.BlogComment;
import com.culciful.service.CommentsService;
import com.culciful.mapper.BlogCommentMapper;
import org.springframework.stereotype.Service;

/**
* @author culciful_zy
* @description 针对表【comments(评论表)】的数据库操作Service实现
* @createDate 2024-01-31 13:49:22
*/
@Service
public class CommentsServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment>
    implements CommentsService{

}




