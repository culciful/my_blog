package com.culciful.mapper;

import com.culciful.pojo.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Article table Mapper interface
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

}
