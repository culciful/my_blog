package com.culciful.mapper;

import com.culciful.pojo.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * User profile table Mapper interface
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /**
     * 查用户，不管 is_deleted。application.yaml 把 isDeleted 配成了 mybatis-plus 全局逻辑删除字段，
     * BaseMapper 生成的 selectById/selectList 等方法会自动加 is_deleted=0，查不到已注销用户；
     * 但「文章作者名」「访客视角个人主页」这些场景需要把已注销用户和真不存在的 id 区分开
     * （前者要显示"该用户已注销"，后者才是真的 404）。手写 SQL 不受 MP 那层自动注入影响。
     */
    @Select("SELECT * FROM user_info WHERE id = #{id}")
    UserInfo selectByIdIncludingDeleted(@Param("id") Long id);
}
