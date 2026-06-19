package com.aizuda.snail.ai.admin.service;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.features.resource.enums.ResourceBizTypeEnum;
import com.aizuda.snail.ai.persistence.admin.mapper.UserMapper;
import com.aizuda.snail.ai.persistence.resource.mapper.ResourceMapper;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.dto.AudienceDTO;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.admin.vo.AuthorizeRequestVO;
import com.aizuda.snail.ai.admin.vo.ChangePasswordRequestVO;
import com.aizuda.snail.ai.admin.vo.LoginRequestVO;
import com.aizuda.snail.ai.admin.vo.LoginResponseVO;
import com.aizuda.snail.ai.admin.vo.UserCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.UserInfoVO;
import com.aizuda.snail.ai.admin.vo.UserQueryVO;
import com.aizuda.snail.ai.admin.vo.UserUpdateRequestVO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 用户服务
 *
 * @author opensnail
 * @date 2025-07-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;
    
    private static final String PASSWORD_SALT = "snail_ai_2026";
    private static final String PASSWORD_HASH_PREFIX = "pbkdf2";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_SALT_BYTES = 16;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成随机字符串（用于JWT签名密钥）
     */
    private String generateCode(int length) {
        String chars = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPLKJHGFDSAZXCVBNM";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 加密密码 (PBKDF2WithHmacSHA256)
     */
    private String encryptPassword(String password) {
        byte[] salt = new byte[PBKDF2_SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS);
        return String.join("$",
                PASSWORD_HASH_PREFIX,
                String.valueOf(PBKDF2_ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash));
    }

    private String legacySha256Password(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((password + PASSWORD_SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new SnailAiCommonException("密码加密失败");
        }
    }

    /**
     * 验证密码
     */
    private boolean verifyPassword(String rawPassword, String encryptedPassword) {
        if (StrUtil.isBlank(rawPassword) || StrUtil.isBlank(encryptedPassword)) {
            return false;
        }
        if (encryptedPassword.startsWith(PASSWORD_HASH_PREFIX + "$")) {
            return verifyPbkdf2Password(rawPassword, encryptedPassword);
        }
        return legacySha256Password(rawPassword).equals(encryptedPassword);
    }

    private boolean isLegacyPasswordHash(String encryptedPassword) {
        return StrUtil.isNotBlank(encryptedPassword) && !encryptedPassword.startsWith(PASSWORD_HASH_PREFIX + "$");
    }

    private boolean verifyPbkdf2Password(String rawPassword, String encryptedPassword) {
        try {
            String[] parts = encryptedPassword.split("\\$");
            if (parts.length != 4 || !PASSWORD_HASH_PREFIX.equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            log.warn("Invalid password hash format");
            return false;
        }
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, PBKDF2_KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new SnailAiCommonException("密码加密失败", e);
        }
    }

    /**
     * 创建用户（管理员）
     */
    public void createUser(UserCreateRequestVO requestVO) {
        if (StrUtil.isBlank(requestVO.getUsername())) {
            throw new SnailAiCommonException("用户名不能为空");
        }
        
        if (StrUtil.isBlank(requestVO.getPassword())) {
            throw new SnailAiCommonException("密码不能为空");
        }

        if (requestVO.getPassword().length() < 6) {
            throw new SnailAiCommonException("密码长度不能少于6位");
        }

        if (requestVO.getRole() == null) {
            throw new SnailAiCommonException("角色不能为空");
        }

        if (!RoleEnum.getEnumTypeMap().containsKey(requestVO.getRole())) {
            throw new SnailAiCommonException("无效的角色");
        }

        // 检查用户名是否已存在
        UserPO existUser = userMapper.selectOne(
            new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, requestVO.getUsername())
        );
        
        if (existUser != null) {
            throw new SnailAiCommonException("用户名已存在");
        }

        // 如果提供了邮箱，检查邮箱是否已存在
        if (StrUtil.isNotBlank(requestVO.getEmail())) {
            UserPO existEmailUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>().eq(UserPO::getEmail, requestVO.getEmail())
            );
            
            if (existEmailUser != null) {
                throw new SnailAiCommonException("邮箱已存在");
            }
        }

        // 创建新用户
        UserPO userPO = new UserPO();
        userPO.setRole(requestVO.getRole());
        userPO.setUsername(requestVO.getUsername());
        // 如果提供了邮箱则使用邮箱，否则使用用户名
        if (StrUtil.isNotBlank(requestVO.getEmail())) {
            userPO.setEmail(requestVO.getEmail());
        }
        userPO.setPassword(encryptPassword(requestVO.getPassword()));
        userMapper.insert(userPO);
        
        log.info("管理员创建新用户成功: username={}, role={}", requestVO.getUsername(), requestVO.getRole());
    }

    /**
     * 用户注册
     */
    public void register(LoginRequestVO requestVO) {
        if (StrUtil.isBlank(requestVO.getUsername())) {
            throw new SnailAiCommonException("账号不能为空");
        }
        
        if (StrUtil.isBlank(requestVO.getPassword())) {
            throw new SnailAiCommonException("密码不能为空");
        }

        if (requestVO.getPassword().length() < 6) {
            throw new SnailAiCommonException("密码长度不能少于6位");
        }

        // 检查用户名是否已存在（邮箱 / 登录名 任一重复即不可注册）
        UserPO existUser = userMapper.selectOne(
            new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getEmail, requestVO.getUsername())
                .or()
                .eq(UserPO::getUsername, requestVO.getUsername())
        );
        
        if (existUser != null) {
            throw new SnailAiCommonException("账号已存在");
        }

        // 创建新用户（登录账号同时写入 username、email，与登录查询一致）
        UserPO userPO = new UserPO();
        userPO.setRole(RoleEnum.USER.getRoleId());
        userPO.setUsername(requestVO.getUsername());
        userPO.setEmail(requestVO.getUsername());
        userPO.setPassword(encryptPassword(requestVO.getPassword()));
        userMapper.insert(userPO);
        
        log.info("新用户注册成功: {}", requestVO.getUsername());
    }

    /**
     * 用户登录（账号密码）
     */
    public LoginResponseVO login(LoginRequestVO requestVO) {
        if (StrUtil.isBlank(requestVO.getUsername())) {
            throw new SnailAiCommonException("账号不能为空");
        }
        
        if (StrUtil.isBlank(requestVO.getPassword())) {
            throw new SnailAiCommonException("密码不能为空");
        }

        // 查询用户
        UserPO userPO = userMapper.selectOne(
            new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, requestVO.getUsername())
        );
        
        if (Objects.isNull(userPO)) {
            throw new SnailAiCommonException("账号或密码错误");
        }

        // 验证密码
        if (!verifyPassword(requestVO.getPassword(), userPO.getPassword())) {
            throw new SnailAiCommonException("账号或密码错误");
        }

        if (isLegacyPasswordHash(userPO.getPassword())) {
            userPO.setPassword(encryptPassword(requestVO.getPassword()));
            userMapper.updateById(userPO);
        }

        // 生成token（使用授权码作为JWT签名密钥）
        AudienceDTO audienceDTO = new AudienceDTO();
        audienceDTO.setUsername(userPO.getUsername());
        
        LoginResponseVO loginResponseVO = new LoginResponseVO();
        loginResponseVO.setUsername(userPO.getUsername());
        loginResponseVO.setNickname(userPO.getNickname());
        loginResponseVO.setToken(getToken(audienceDTO, userPO.getPassword()));


        log.info("用户登录成功: {}", requestVO.getUsername());
        return loginResponseVO;
    }

    /**
     * 生成Token（24小时有效）
     */
    private String getToken(AudienceDTO audienceDTO, String pwd) {
        return JWT.create()
                .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)))
                .withAudience(JsonUtil.toJsonString(audienceDTO))
                .sign(Algorithm.HMAC256(pwd));
    }

    /**
     * 获取当前用户信息
     */
    public UserInfoVO getUserInfo() {
        UserPO userPO = UserSessionUtils.currentUserSession();
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(userPO.getId());
        userInfoVO.setUsername(userPO.getUsername());
        userInfoVO.setNickname(userPO.getNickname());
        userInfoVO.setEmail(userPO.getEmail());
        userInfoVO.setRole(userPO.getRole());
        userInfoVO.setRoleName(resolveRoleName(userPO.getRole()));
        userInfoVO.setAvatarUrl(resolveAvatarUrl(userPO.getResourceId()));
        return userInfoVO;
    }

    /**
     * 获取用户分页列表（管理员）
     */
    public PageResult<List<UserInfoVO>> getPageUserList(UserQueryVO queryVO) {
        PageDTO<UserPO> userPageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
        String keyword = StrUtil.trim(queryVO.getEmail());

        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<UserPO>().orderByDesc(UserPO::getId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(UserPO::getEmail, keyword).or().like(UserPO::getUsername, keyword));
        }

        PageDTO<UserPO> pageDTO = userMapper.selectPage(userPageDTO, wrapper);

        PageResult<List<UserInfoVO>> pageResult = new PageResult<>();

        List<UserInfoVO> infoVOS = pageDTO.getRecords().stream().map(userPO -> {
            UserInfoVO userInfoVO = new UserInfoVO();
            userInfoVO.setId(userPO.getId());
            userInfoVO.setUsername(userPO.getUsername());
            userInfoVO.setNickname(userPO.getNickname());
            userInfoVO.setEmail(userPO.getEmail());
            userInfoVO.setRole(userPO.getRole());
            userInfoVO.setRoleName(resolveRoleName(userPO.getRole()));
            userInfoVO.setAvatarUrl(resolveAvatarUrl(userPO.getResourceId()));
            userInfoVO.setCreateDt(userPO.getCreateDt());
            userInfoVO.setUpdateDt(userPO.getUpdateDt());
            return userInfoVO;
        }).toList();

        pageResult.setData(infoVOS);
        pageResult.setTotal(pageDTO.getTotal());
        pageResult.setSize(pageDTO.getSize());
        pageResult.setPage(pageDTO.getCurrent());
        return pageResult;
    }

    /**
     * 授权用户（管理员）
     */
    public void authorizeUser(AuthorizeRequestVO requestVO) {
        String account = StrUtil.trim(requestVO.getEmail());
        UserPO userPO = userMapper.selectOne(
            new LambdaQueryWrapper<UserPO>().eq(UserPO::getEmail, account)
        );
        if (Objects.isNull(userPO)) {
            userPO = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, account)
            );
        }

        if (Objects.isNull(userPO)) {
            throw new SnailAiCommonException("用户不存在");
        }

        userMapper.updateById(userPO);
        
        log.info("授权用户成功: {}, 配额: {}, 天数: {}", requestVO.getEmail(), requestVO.getTotals(), requestVO.getDays());
    }

    /**
     * 更新用户信息（管理员）- 统一更新接口
     */
    public void updateUser(Long id, UserUpdateRequestVO requestVO) {
        // 1. 验证用户存在
        UserPO userPO = userMapper.selectById(id);
        if (Objects.isNull(userPO)) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        // 2. 验证角色有效性
        if (!RoleEnum.getEnumTypeMap().containsKey(requestVO.getRole())) {
            throw new SnailAiCommonException("无效的角色");
        }
        
        // 3. 如果更新邮箱，检查邮箱是否已被其他用户使用
        if (StrUtil.isNotBlank(requestVO.getEmail()) 
            && !requestVO.getEmail().equals(userPO.getEmail())) {
            UserPO existUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>()
                    .eq(UserPO::getEmail, requestVO.getEmail())
                    .ne(UserPO::getId, id)
            );
            if (Objects.nonNull(existUser)) {
                throw new SnailAiCommonException("邮箱已被使用");
            }
            userPO.setEmail(requestVO.getEmail());
        }
        
        // 4. 更新角色
        userPO.setRole(requestVO.getRole());
        
        // 5. 如果提供了密码，则更新密码
        if (StrUtil.isNotBlank(requestVO.getPassword())) {
            if (requestVO.getPassword().length() < 6) {
                throw new SnailAiCommonException("密码长度不能少于6位");
            }
            userPO.setPassword(encryptPassword(requestVO.getPassword()));
        }

        // 6. 如果提交了头像字段，则更新头像资源绑定
        if (requestVO.getResourceId() != null || Boolean.TRUE.equals(requestVO.getAvatarCleared())) {
            updateUserAvatar(userPO, requestVO);
        }
        
        // 7. 保存更新
        userMapper.updateById(userPO);
        
        log.info("更新用户信息成功: id={}, role={}, email={}", id, requestVO.getRole(), requestVO.getEmail());
    }

    private void updateUserAvatar(UserPO userPO, UserUpdateRequestVO requestVO) {
        if (Boolean.TRUE.equals(requestVO.getAvatarCleared())) {
            // MyBatis-Plus 默认策略会跳过 null 字段，需显式置空
            userMapper.update(null, new LambdaUpdateWrapper<UserPO>()
                    .eq(UserPO::getId, userPO.getId())
                    .set(UserPO::getResourceId, null));
            // 同步内存对象，避免后续 updateById 覆盖
            userPO.setResourceId(null);
            return;
        }

        if (requestVO.getResourceId() != null) {
            ResourcePO resource = resourceMapper.selectById(requestVO.getResourceId());
            if (resource == null) {
                throw new SnailAiCommonException("头像资源不存在");
            }
            if (!ResourceBizTypeEnum.AVATAR.getValue().equals(resource.getBizType())) {
                throw new SnailAiCommonException("请选择头像资源");
            }
            userPO.setResourceId(resource.getId());
        }
    }

    /**
     * 更新用户角色（管理员）
     */
    public void updateUserRole(Long id, Integer role) {
        if (!RoleEnum.getEnumTypeMap().containsKey(role)) {
            throw new SnailAiCommonException("无效的角色");
        }

        UserPO userPO = userMapper.selectById(id);
        if (Objects.isNull(userPO)) {
            throw new SnailAiCommonException("用户不存在");
        }

        userPO.setRole(role);
        userMapper.updateById(userPO);
        
        log.info("更新用户角色成功: id={}, role={}", id, role);
    }

    /**
     * 用户自助修改密码（需验证旧密码）
     */
    public void changePassword(ChangePasswordRequestVO request) {
        UserPO currentUser = UserSessionUtils.currentUserSession();
        if (!verifyPassword(request.getOldPassword(), currentUser.getPassword())) {
            throw new SnailAiCommonException("旧密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new SnailAiCommonException("新密码不能与旧密码相同");
        }
        UserPO update = new UserPO();
        update.setId(currentUser.getId());
        update.setPassword(encryptPassword(request.getNewPassword()));
        userMapper.updateById(update);
        log.info("用户修改密码成功: id={}, username={}", currentUser.getId(), currentUser.getUsername());
    }

    /**
     * 重置用户密码（管理员）
     */
    public void resetUserPassword(Long id, String newPassword) {
        if (StrUtil.isBlank(newPassword)) {
            throw new SnailAiCommonException("密码不能为空");
        }

        if (newPassword.length() < 6) {
            throw new SnailAiCommonException("密码长度不能少于6位");
        }

        UserPO userPO = userMapper.selectById(id);
        if (Objects.isNull(userPO)) {
            throw new SnailAiCommonException("用户不存在");
        }

        userPO.setPassword(encryptPassword(newPassword));
        userMapper.updateById(userPO);
        
        log.info("重置用户密码成功: id={}, username={}", id, userPO.getUsername());
    }

    /**
     * 删除用户（管理员）
     */
    public void deleteUser(Long id) {
        UserPO userPO = userMapper.selectById(id);
        if (Objects.isNull(userPO)) {
            throw new SnailAiCommonException("用户不存在");
        }

        // 不允许删除管理员
        if (RoleEnum.isAdmin(userPO.getRole())) {
            throw new SnailAiCommonException("不能删除管理员账号");
        }

        userMapper.deleteById(id);
        
        log.info("删除用户成功: id={}, email={}", id, userPO.getEmail());
    }

    private String resolveAvatarUrl(Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        ResourcePO resource = resourceMapper.selectById(resourceId);
        return resource != null ? resource.getAccessUrl() : null;
    }

    private static String resolveRoleName(Integer roleId) {
        if (roleId == null) {
            return "-";
        }
        RoleEnum e = RoleEnum.getEnumTypeMap().get(roleId);
        if (e == null) {
            return "-";
        }
        return switch (e) {
            case ADMIN -> "管理员";
            case USER -> "普通用户";
        };
    }
}
