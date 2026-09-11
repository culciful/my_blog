package com.culciful.service;

import com.culciful.common.enums.AuditAction;
import com.culciful.mapper.AuditLogMapper;
import com.culciful.pojo.AuditLog;
import com.culciful.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录 / 改密 / 注销这类安全敏感操作的审计留痕。双写：落一行到 audit_log（可查询），
 * 同时打一条 SLF4J INFO 日志（能被日志采集/告警系统捡到，不用等着查库）。
 *
 * <p>写审计本身失败（比如数据库抖了一下）不能反过来把登录/改密/注销这个主流程搞挂——
 * 抓住异常改成打 ERROR 日志，不往上抛。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * @param userId 操作用户 ID；识别不到（比如登录失败、用户名/邮箱都对不上）就传 null
     * @param detail 附加信息，比如登录失败时提交的用户名/邮箱；不涉及密码等敏感值
     */
    public void record(AuditAction action, Long userId, String ip, String detail) {
        log.info("audit: action={} userId={} ip={} detail={}", action, userId, ip, detail);
        try {
            AuditLog row = new AuditLog();
            row.setId(snowflakeIdGenerator.nextId());
            row.setUserId(userId);
            row.setAction(action.name());
            row.setIp(ip);
            row.setDetail(detail);
            row.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(row);
        } catch (Exception e) {
            log.error("failed to persist audit log row: action={} userId={}", action, userId, e);
        }
    }
}
