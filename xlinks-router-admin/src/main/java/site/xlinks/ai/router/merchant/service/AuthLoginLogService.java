package site.xlinks.ai.router.merchant.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.merchant.entity.AuthLoginLog;
import site.xlinks.ai.router.merchant.mapper.AuthLoginLogMapper;

/**
 * 登录日志服务。
 */
@Service
@RequiredArgsConstructor
public class AuthLoginLogService extends ServiceImpl<AuthLoginLogMapper, AuthLoginLog> {

    public void record(AuthLoginLog log) {
        this.save(log);
    }
}