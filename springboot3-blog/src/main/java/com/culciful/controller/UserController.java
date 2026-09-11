package com.culciful.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.mapper.UserFollowMapper;
import com.culciful.mapper.UserPackageMapper;
import com.culciful.mapper.FileAssetMapper;
import com.culciful.dto.EmailExistParam;
import com.culciful.dto.EmailCodeRequest;
import com.culciful.dto.FollowStateRequest;
import com.culciful.dto.PackageRefRequest;
import com.culciful.dto.PackageRequest;
import com.culciful.dto.PageSearchRequest;
import com.culciful.dto.PasswordCheckRequest;
import com.culciful.dto.PasswordResetRequest;
import com.culciful.dto.PasswordUpdateRequest;
import com.culciful.dto.RegisterRequest;
import com.culciful.dto.UserUpdateRequest;
import com.culciful.pojo.UserInfo;
import com.culciful.pojo.UserFollow;
import com.culciful.pojo.UserPackage;
import com.culciful.pojo.FileAsset;
import com.culciful.security.JwtCookieService;
import com.culciful.service.EmailVerificationCodeService;
import com.culciful.service.ImageStorageService;
import com.culciful.service.UserService;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.utils.RequestUtils;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/1/31 15:25
 * description:
 */

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {
    private final UserInfoMapper userInfoMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserPackageMapper userPackageMapper;
    private final FileAssetMapper fileAssetMapper;
    private final UserService userService;
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final ImageStorageService imageStorageService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final JwtCookieService jwtCookieService;

    /**
     * register
     */
    @PostMapping("register")
    public R<Void> register(@RequestBody @Valid RegisterRequest request) {
        return userService.register(request);
    }

    /**
     * Check whether email already exists.
     */
    @PostMapping("checkEmailExist")
    public R<Map<String, Boolean>> checkEmailExist(@RequestBody @Valid EmailExistParam emailExistParam) {
        return userService.checkEmailExist(emailExistParam);
    }

    /**
     * Public profile query by id. 已注销的账号也能查到（isDeleted=true），前端拿这个区分
     * 「该用户已注销」（内容管理页仍可浏览其历史文章、可取消已有关注、不能新建关注）
     * 和「id 压根不存在」（USERNAME_ERROR，整页找不到）。
     */
    @GetMapping("getUserInfo")
    public R<Map<String, Object>> getUserPublicProfile(@RequestParam("id") String idParam) {
        Long targetId = parseId(idParam);
        if (targetId == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = userInfoMapper.selectByIdIncludingDeleted(targetId);
        if (user == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        return R.ok(toPublicProfile(user));
    }

    /**
     * Current logged-in user profile, requires JWT.
     */
    @GetMapping("getMyProfile")
    public R<Map<String, Object>> getMyProfile() {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        UserInfo user = loadActiveUser(selfId);
        if (user == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        Map<String, Object> m = toPublicProfile(user);
        m.put("email", user.getEmail());
        return R.ok(m);
    }

    @PostMapping("updateUserInfo")
    public R<Void> updateMyProfile(@RequestBody @Valid UserUpdateRequest request) {
        return doUpdateMyProfile(request);
    }

    @PostMapping("checkPassword")
    public R<Void> checkPassword(@RequestBody @Valid PasswordCheckRequest request) {
        return doCheckPassword(request);
    }

    /** 已登录改密码：校验当前密码，改完 token_version+1（其他设备踢下线，本设备也需重登） */
    @PostMapping("updatePassword")
    public R<Void> updatePassword(@RequestBody @Valid PasswordUpdateRequest request) {
        return doUpdatePassword(request);
    }

    /** 匿名忘记密码：邮箱验证码 + 新密码，改完 token_version+1 */
    @PostMapping("resetPassword")
    public R<Void> resetPassword(@RequestBody @Valid PasswordResetRequest request) {
        return doResetPassword(request);
    }

    /**
     * 注销账号：校验当前密码，软删 + 释放 email/username（deleted_token 设成自己的雪花 id，
     * 天然全局唯一，(email/username, 0) 这个「活跃」槽位空出来给以后重新注册用）+ 强制登出
     * （token_version+1，本设备与其它设备的 JWT 一起失效）+ 清 cookie。
     * 不级联删除已发布的文章/评论 —— 账号注销 ≠ 内容删除，文章仍按原作者信息展示；
     * 访问该用户的个人主页会 404（getUserInfo/getMyProfile 都过滤 is_deleted）。
     */
    @PostMapping("deleteAccount")
    public R<Void> deleteAccount(@RequestBody @Valid PasswordCheckRequest request, HttpServletResponse response) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        UserInfo user = loadActiveUser(selfId);
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return R.fail(ResultCodeEnum.PASSWORD_ERROR);
        }
        // 不能用 updateById(entity) 传 isDeleted=true —— application.yaml 把 isDeleted 配成了
        // mybatis-plus 的全局逻辑删除字段（logic-delete-field），MP 生成 updateById 的 SQL 模板会
        // 直接把逻辑删除字段从 SET 子句里摘掉（它认为「删除」只能走 deleteById()/delete()），
        // 结果就是 deleted_token/updated_at 真的落了库、is_deleted 却静默没生效，注销「看起来成功」
        // 实际上账号还活着（实测踩到：deleted_token 已经等于自己的 id，is_deleted 还是 0）。
        // 跟 deletePackage 一样改用 LambdaUpdateWrapper.set(...)，走的是显式 SQL 拼接，不受这条限制。
        userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .set(UserInfo::getIsDeleted, true)
                .set(UserInfo::getDeletedToken, selfId)
                .set(UserInfo::getUpdatedAt, LocalDateTime.now())
                .eq(UserInfo::getId, selfId));
        bumpTokenVersion(selfId);
        jwtCookieService.clearTokenCookie(response);
        return R.ok(null);
    }

    @GetMapping("getStat")
    public R<Map<String, Object>> getMyStats() {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        Integer ownArticleCount = userInfoMapper.selectById(selfId).getArticleCount();
        long articleCount = ownArticleCount == null ? 0 : ownArticleCount;
        long followingCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId));
        long followerCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, selfId));
        return R.ok(Map.of("articleCount", articleCount, "followingCount", followingCount, "followerCount", followerCount));
    }

    @PostMapping("getFollowings")
    public R<Map<String, Object>> searchMyFollowings(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        return R.ok(followPageResult(selfId, /* isFollowingList */ true, request));
    }

    @PostMapping("getFollowers")
    public R<Map<String, Object>> searchMyFollowers(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        return R.ok(followPageResult(selfId, /* isFollowingList */ false, request));
    }

    @PostMapping("sendEmailCode")
    public R<Void> sendEmailCode(@RequestBody @Valid EmailCodeRequest request, HttpServletRequest httpRequest) {
        String reason = emailVerificationCodeService.sendCode(
                request.email(), requireValidScene(request.scene()), RequestUtils.clientIp(httpRequest));
        if (reason == null) {
            return R.ok(null);
        }
        return switch (reason) {
            case "EMAIL_USED" -> R.fail(ResultCodeEnum.EMAIL_USED);
            case "EMAIL_NOT_FOUND" -> R.fail(ResultCodeEnum.USERNAME_ERROR);
            case "RATE_LIMIT" -> R.fail(ResultCodeEnum.RATE_LIMITED);
            default -> R.fail(ResultCodeEnum.BUSINESS_ERROR);
        };
    }

    @PostMapping("checkEmailCode")
    public R<Void> checkEmailCode(@RequestBody @Valid EmailCodeRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.verificationCode())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        return emailVerificationCodeService.verifyCode(request.email(), request.verificationCode(), requireValidScene(request.scene()))
                ? R.ok(null)
                : R.fail(ResultCodeEnum.PARAM_ERROR);
    }

    @PostMapping("uploadAvatar")
    public R<Void> uploadAvatar(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) String image) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        FileAsset asset = resolveAvatarAsset(file, image, selfId);
        UserInfo user = new UserInfo();
        user.setId(selfId);
        user.setAvatarAssetId(asset.getId());
        user.setUpdatedAt(LocalDateTime.now());
        userInfoMapper.updateById(user);
        return R.ok(null);
    }

    @GetMapping("checkHasFollow")
    public R<Map<String, Boolean>> getFollowState(@RequestParam("id") String idParam) {
        Long selfId = currentUserId();
        Long targetId = parseId(idParam);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (targetId == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        boolean isFollowing = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId)
                .eq(UserFollow::getFollowingId, targetId)) > 0;
        return R.ok(Map.of("isFollowing", isFollowing));
    }

    @PostMapping("switchFollow")
    public R<Void> setFollowState(@RequestBody @Valid FollowStateRequest request) {
        Long selfId = currentUserId();
        Long targetId = parseId(request.id());
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (targetId == null || selfId.equals(targetId) || request == null || request.shouldFollow() == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        // 注销的账号不能被关注：取消关注（history cleanup）仍放行，新建关注要挡
        if (request.shouldFollow() && loadActiveUser(targetId) == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        boolean isFollowing = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId)
                .eq(UserFollow::getFollowingId, targetId)) > 0;
        if (request.shouldFollow() && !isFollowing) {
            UserFollow follow = new UserFollow();
            follow.setId(snowflakeIdGenerator.nextId());
            follow.setFollowerId(selfId);
            follow.setFollowingId(targetId);
            follow.setCreatedAt(LocalDateTime.now());
            follow.setUpdatedAt(LocalDateTime.now());
            userFollowMapper.insert(follow);
        } else if (!request.shouldFollow() && isFollowing) {
            userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, selfId)
                    .eq(UserFollow::getFollowingId, targetId));
        }
        return R.ok(null);
    }

    @GetMapping("getPackages")
    public R<Map<String, Object>> getPackages(@RequestParam("id") String idParam) {
        Long userId = parseId(idParam);
        if (userId == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        List<Map<String, Object>> list = userPackageMapper.selectList(new LambdaQueryWrapper<UserPackage>()
                        .eq(UserPackage::getUserId, userId)
                        .eq(UserPackage::getIsDeleted, false)
                        .orderByAsc(UserPackage::getCreatedAt))
                .stream()
                .map(p -> Map.<String, Object>of("pid", p.getId(), "pname", p.getPackName()))
                .toList();
        return R.ok(Map.of("list", list));
    }

    @PostMapping("addPackage")
    public R<Map<String, Long>> addPackage(@RequestBody @Valid PackageRequest request) {
        Long selfId = currentUserId();
        Long userId = parseId(request.id());
        if (!isSelf(selfId, userId) || request == null || isBlank(request.pname())) {
            return R.fail(selfId == null ? ResultCodeEnum.NOT_LOGIN : ResultCodeEnum.PARAM_ERROR);
        }
        UserPackage p = new UserPackage();
        p.setId(snowflakeIdGenerator.nextId());
        p.setUserId(userId);
        p.setPackName(request.pname());
        p.setIsDeleted(false);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        userPackageMapper.insert(p);
        return R.ok(Map.of("pid", p.getId()));
    }

    @PostMapping("editPackage")
    public R<Void> editPackage(@RequestBody @Valid PackageRequest request) {
        Long selfId = currentUserId();
        Long userId = parseId(request.id());
        Long pid = parseId(request.pid());
        if (!isSelf(selfId, userId) || pid == null || request == null || isBlank(request.pname())) {
            return R.fail(selfId == null ? ResultCodeEnum.NOT_LOGIN : ResultCodeEnum.PARAM_ERROR);
        }
        UserPackage p = new UserPackage();
        p.setId(pid);
        p.setPackName(request.pname());
        p.setUpdatedAt(LocalDateTime.now());
        userPackageMapper.update(p, new LambdaUpdateWrapper<UserPackage>()
                .eq(UserPackage::getId, pid)
                .eq(UserPackage::getUserId, userId)
                .eq(UserPackage::getIsDeleted, false));
        return R.ok(null);
    }

    @PostMapping("deletePackage")
    public R<Void> deletePackage(@RequestBody @Valid PackageRefRequest request) {
        Long selfId = currentUserId();
        Long userId = parseId(request.id());
        Long pid = parseId(request.pid());
        if (!isSelf(selfId, userId) || pid == null) {
            return R.fail(selfId == null ? ResultCodeEnum.NOT_LOGIN : ResultCodeEnum.PARAM_ERROR);
        }
        userPackageMapper.update(null, new LambdaUpdateWrapper<UserPackage>()
                .set(UserPackage::getIsDeleted, true)
                .set(UserPackage::getUpdatedAt, LocalDateTime.now())
                .eq(UserPackage::getId, pid)
                .eq(UserPackage::getUserId, userId));
        return R.ok(null);
    }

    /** 已登录改资料：仅用户名 / 邮箱（改邮箱需验证码，且 token_version+1）。改密码走 updatePassword。 */
    private R<Void> doUpdateMyProfile(UserUpdateRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (request == null || (isBlank(request.username()) && isBlank(request.email()))) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = new UserInfo();
        user.setId(selfId);
        boolean isEmailChanged = false;
        if (!isBlank(request.username())) {
            user.setUsername(request.username());
        }
        if (!isBlank(request.email())) {
            if (isBlank(request.verificationCode())
                    || !emailVerificationCodeService.consumeCode(
                    request.email(),
                    request.verificationCode(),
                    EmailVerificationCodeService.SCENE_UPDATE_EMAIL)) {
                return R.fail(ResultCodeEnum.PARAM_ERROR);
            }
            user.setEmail(request.email());
            isEmailChanged = true;
        }
        user.setUpdatedAt(LocalDateTime.now());
        userInfoMapper.updateById(user);
        if (isEmailChanged) {
            bumpTokenVersion(selfId);
        }
        return R.ok(null);
    }

    private R<Void> doUpdatePassword(PasswordUpdateRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        UserInfo user = loadActiveUser(selfId);
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return R.fail(ResultCodeEnum.PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            return R.fail(ResultCodeEnum.PASSWORD_NOT_CHANGED);
        }
        applyNewPassword(selfId, request.newPassword());
        return R.ok(null);
    }

    private R<Void> doResetPassword(PasswordResetRequest request) {
        if (!emailVerificationCodeService.consumeCode(
                request.email(),
                request.verificationCode(),
                EmailVerificationCodeService.SCENE_RESET_PASSWORD)) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getEmail, request.email())
                .eq(UserInfo::getIsDeleted, false)
                .last("LIMIT 1"));
        if (user == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        applyNewPassword(user.getId(), request.newPassword());
        return R.ok(null);
    }

    /** 落新密码 + token_version+1（改密后所有旧 token 失效，需重新登录） */
    private void applyNewPassword(Long userId, String rawNewPassword) {
        UserInfo update = new UserInfo();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(rawNewPassword));
        update.setUpdatedAt(LocalDateTime.now());
        userInfoMapper.updateById(update);
        bumpTokenVersion(userId);
    }

    private void bumpTokenVersion(Long userId) {
        userInfoMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .setSql("token_version = token_version + 1")
                .eq(UserInfo::getId, userId));
    }

    private R<Void> doCheckPassword(PasswordCheckRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (request == null || isBlank(request.password())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = loadActiveUser(selfId);
        return user != null && passwordEncoder.matches(request.password(), user.getPassword())
                ? R.ok(null)
                : R.fail(ResultCodeEnum.PASSWORD_ERROR);
    }

    private UserInfo loadActiveUser(long id) {
        UserInfo user = userInfoMapper.selectById(id);
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
            return null;
        }
        return user;
    }

    private Map<String, Object> toPublicProfile(UserInfo user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("avatarUrl", avatarUrl(user.getAvatarAssetId()));
        m.put("createTime", user.getCreatedAt() != null ? user.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond() : 0L);
        m.put("isDeleted", Boolean.TRUE.equals(user.getIsDeleted()));
        return m;
    }

    private Map<String, Object> followPageResult(Long selfId, boolean isFollowingList, PageSearchRequest request) {
        String keywordTrimmed = request.safeKeyword();
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(isFollowingList ? UserFollow::getFollowerId : UserFollow::getFollowingId, selfId)
                .orderByDesc(UserFollow::getCreatedAt);
        if (!keywordTrimmed.isEmpty()) {
            List<Long> matchedUserIds = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                            .eq(UserInfo::getIsDeleted, false)
                            .and(w -> w.like(UserInfo::getUsername, keywordTrimmed).or().like(UserInfo::getEmail, keywordTrimmed)))
                    .stream()
                    .map(UserInfo::getId)
                    .toList();
            if (matchedUserIds.isEmpty()) {
                return Map.of("list", List.of(), "total", 0);
            }
            wrapper.in(isFollowingList ? UserFollow::getFollowingId : UserFollow::getFollowerId, matchedUserIds);
        }
        Page<UserFollow> page = userFollowMapper.selectPage(
                new Page<>(request.safeCurrentPage(), request.safePageSize()),
                wrapper
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserFollow relation : page.getRecords()) {
            Long userId = isFollowingList ? relation.getFollowingId() : relation.getFollowerId();
            UserInfo user = loadActiveUser(userId);
            if (user == null) {
                continue;
            }
            Map<String, Object> m = toPublicProfile(user);
            // 不对外暴露他人邮箱
            boolean isFollowedByMe = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, selfId)
                    .eq(UserFollow::getFollowingId, userId)) > 0;
            boolean isFollowingMe = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, userId)
                    .eq(UserFollow::getFollowingId, selfId)) > 0;
            m.put("isFollowing", isFollowedByMe);
            m.put("isMutual", isFollowedByMe && isFollowingMe);
            list.add(m);
        }
        return Map.of("list", list, "total", page.getTotal());
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return parseId(auth.getName());
    }

    private boolean isSelf(Long selfId, Long targetId) {
        return selfId != null && targetId != null && selfId.equals(targetId);
    }

    private String avatarUrl(Long avatarAssetId) {
        if (avatarAssetId == null) {
            return null;
        }
        FileAsset asset = fileAssetMapper.selectById(avatarAssetId);
        return asset == null ? null : asset.getPublicUrl();
    }

    private FileAsset resolveAvatarAsset(MultipartFile file, String image, Long ownerId) {
        if (!isBlank(image)) {
            return imageStorageService.storeDataUrl(image, ownerId, ImageStorageService.Kind.AVATAR);
        }
        if (file != null && !file.isEmpty()) {
            return imageStorageService.store(file, ownerId, ImageStorageService.Kind.AVATAR);
        }
        throw new IllegalArgumentException("file is required");
    }

    /** 空 → register；否则必须是三个已知场景之一，未知场景直接拒（防日志注入 / 借服务器给任意邮箱发信） */
    private String requireValidScene(String scene) {
        if (isBlank(scene)) {
            return EmailVerificationCodeService.SCENE_REGISTER;
        }
        String s = scene.trim();
        if (EmailVerificationCodeService.SCENE_REGISTER.equals(s)
                || EmailVerificationCodeService.SCENE_RESET_PASSWORD.equals(s)
                || EmailVerificationCodeService.SCENE_UPDATE_EMAIL.equals(s)) {
            return s;
        }
        throw new IllegalArgumentException("unknown scene: " + s);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Long parseId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
