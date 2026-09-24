package com.culciful.mapper;

import com.culciful.pojo.BlogComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * Comment table Mapper interface
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

    /**
     * 取评论作者 id，忽略逻辑删除（父评论被删后，仍要能显示「回复 @xxx」）。
     */
    @Select("SELECT user_id FROM blog_comment WHERE id = #{id}")
    Long selectAuthorIdIgnoreDeleted(@Param("id") Long id);
}
