package com.aizuda.snail.ai.admin.interceptor;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.persistence.admin.mapper.UserMapper;
import com.aizuda.snail.ai.admin.dto.AudienceDTO;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 系统登录认证拦截器
 *
 * @author: byteblogs
 * @date:2023-04-26 12:52
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {
    public static final String AUTHENTICATION = "Snail-Ai-Auth";
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Method method = handlerMethod.getMethod();
        // 检查是否有LoginRequired注解，没有则跳过认证
        if (!method.isAnnotationPresent(LoginRequired.class)) {
            return true;
        }

        LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
        if (!loginRequired.required()) {
            return true;
        }

        // 从请求头中取出token
        String token = request.getHeader(AUTHENTICATION);
        if (StrUtil.isBlank(token)) {
            throw new SnailAiAuthenticationException("未登录或登录已过期");
        }

        // 验证token并获取用户信息
        UserPO userPO = verifyTokenAndGetUser(token);

        // 检查角色权限
        RoleEnum requiredRole = loginRequired.role();
        if (requiredRole == RoleEnum.ADMIN && !RoleEnum.isAdmin(userPO.getRole())) {
            throw new SnailAiAuthenticationException("权限不足");
        }

        // 将用户信息存储到ThreadLocal
        UserSessionUtils.setUserSession(userPO);
        
        return true;
    }

    /**
     * 验证token并获取用户信息
     */
    private UserPO verifyTokenAndGetUser(String token) {
        try {
            // 解码token获取用户信息
            DecodedJWT jwt = JWT.decode(token);
            List<String> audience = jwt.getAudience();
            
            if (audience == null || audience.isEmpty()) {
                throw new SnailAiAuthenticationException("Token格式错误");
            }

            AudienceDTO audienceDTO = JsonUtil.parseObject(audience.get(0), AudienceDTO.class);
            if (audienceDTO == null || StrUtil.isBlank(audienceDTO.getUsername())) {
                throw new SnailAiAuthenticationException("Token格式错误");
            }

            // 查询用户
            UserPO userPO = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, audienceDTO.getUsername())
            );

            if (Objects.isNull(userPO)) {
                throw new SnailAiAuthenticationException("用户不存在");
            }

            // 验证token签名（使用授权码）
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(userPO.getPassword())).build();
            verifier.verify(token);

            return userPO;

        } catch (TokenExpiredException e) {
            log.warn("Token已过期: {}", e.getMessage());
            throw new SnailAiAuthenticationException("登录已过期，请重新登录");
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            throw new SnailAiAuthenticationException("Token验证失败");
        } catch (Exception e) {
            log.error("Token解析异常", e);
            throw new SnailAiAuthenticationException("认证失败");
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 可以在这里做一些后置处理
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal，防止内存泄漏
        UserSessionUtils.clearUserSession();
    }
}
