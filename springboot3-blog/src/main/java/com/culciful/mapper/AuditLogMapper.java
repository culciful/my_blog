package com.culciful.mapper;

import com.culciful.pojo.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Security audit log table Mapper interface
 * </p>
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

}
