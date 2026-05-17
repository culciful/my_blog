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
import com.culciful.dto.PackageRequest;
import com.culciful.dto.PageSearchRequest;
import com.culciful.dto.PasswordCheckRequest;
import com.culciful.dto.RegisterRequest;
import com.culciful.dto.UserUpdateRequest;
import com.culciful.pojo.UserInfo;
import com.culciful.pojo.UserFollow;
import com.culciful.pojo.UserPackage;
import com.culciful.pojo.FileAsset;
import com.culciful.service.UserService;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.security.crypto.EncryptedBody;
import com.culciful.utils.SnowflakeIdGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final Map<String, String> DEV_EMAIL_CODES = new ConcurrentHashMap<>();

    private final UserInfoMapper userInfoMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserPackageMapper userPackageMapper;
    private final FileAssetMapper fileAssetMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * register
     */
    @PostMapping(value = "users", consumes = MediaType.TEXT_PLAIN_VALUE)
    public R<Void> registerEncrypted(@EncryptedBody @Valid RegisterRequest req) {
        return userService.register(req);
    }

    /**
     * Check whether email already exists.
     */
    @PostMapping("users/email-existence")
    public R<Map<String, Integer>> checkEmailExist(@RequestBody @Valid EmailExistParam emailExistParam) {
        return userService.checkEmailExist(emailExistParam);
    }

    /**
     * Public profile query by id.
     */
    @GetMapping("users/{id:\\d+}")
    public R<Map<String, Object>> getUserPublicProfile(@PathVariable("id") String idParam) {
        Long targetId = parseId(idParam);
        if (targetId == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = loadActiveUser(targetId);
        if (user == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        return R.ok(toPublicProfile(user));
    }

    /**
     * Current logged-in user profile, requires JWT.
     */
    @GetMapping("users/me")
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

    @PatchMapping(value = "users/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<Void> updateMyProfile(@RequestBody @Valid UserUpdateRequest request) {
        return doUpdateMyProfile(request);
    }

    @PatchMapping(value = "users/me", consumes = MediaType.TEXT_PLAIN_VALUE)
    public R<Void> updateMyProfileEncrypted(@EncryptedBody @Valid UserUpdateRequest request) {
        return doUpdateMyProfile(request);
    }

    @PostMapping(value = "users/me/password-check", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<Void> checkPassword(@RequestBody @Valid PasswordCheckRequest request) {
        return doCheckPassword(request);
    }

    @PostMapping(value = "users/me/password-check", consumes = MediaType.TEXT_PLAIN_VALUE)
    public R<Void> checkPasswordEncrypted(@EncryptedBody @Valid PasswordCheckRequest request) {
        return doCheckPassword(request);
    }

    @GetMapping("users/me/stats")
    public R<Map<String, Object>> getMyStats() {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        long articleCount = userInfoMapper.selectById(selfId).getArticleCount() == null
                ? 0
                : userInfoMapper.selectById(selfId).getArticleCount();
        long following = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId));
        long follower = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, selfId));
        return R.ok(Map.of("articleCount", articleCount, "following", following, "follower", follower));
    }

    @PostMapping("users/me/followings/search")
    public R<Map<String, Object>> searchMyFollowings(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        Page<UserFollow> page = userFollowMapper.selectPage(new Page<>(request.safeCurrentPage(), request.safePageSize()),
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, selfId)
                        .orderByDesc(UserFollow::getCreatedAt));
        return R.ok(userPageResult(page, true, request.safeFilter().get("keyword"), selfId));
    }

    @PostMapping("users/me/followers/search")
    public R<Map<String, Object>> searchMyFollowers(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        Page<UserFollow> page = userFollowMapper.selectPage(new Page<>(request.safeCurrentPage(), request.safePageSize()),
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowingId, selfId)
                        .orderByDesc(UserFollow::getCreatedAt));
        return R.ok(userPageResult(page, false, request.safeFilter().get("keyword"), selfId));
    }

    @PostMapping("users/verification-codes")
    public R<Void> sendEmailCode(@RequestBody @Valid EmailCodeRequest request) {
        if (request == null || isBlank(request.email())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        DEV_EMAIL_CODES.put(request.email(), "123456");
        System.out.println("dev email verification code for " + request.email() + ": 123456");
        return R.ok(null);
    }

    @PostMapping("users/me/verification-check")
    public R<Void> checkEmailCode(@RequestBody @Valid EmailCodeRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.verificationCode())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        String code = DEV_EMAIL_CODES.get(request.email());
        return request.verificationCode().equals(code) ? R.ok(null) : R.fail(ResultCodeEnum.PARAM_ERROR);
    }

    @PostMapping("users/me/avatar")
    public R<Void> uploadAvatar(MultipartFile file) throws Exception {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        FileAsset asset = saveUpload(file, selfId, "avatar");
        UserInfo user = new UserInfo();
        user.setId(selfId);
        user.setAvatarAssetId(asset.getId());
        user.setUpdatedAt(LocalDateTime.now());
        userInfoMapper.updateById(user);
        return R.ok(null);
    }

    @GetMapping("users/{id:\\d+}/follow-state")
    public R<Map<String, Boolean>> getFollowState(@PathVariable("id") String idParam) {
        Long selfId = currentUserId();
        Long targetId = parseId(idParam);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (targetId == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        boolean followed = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId)
                .eq(UserFollow::getFollowingId, targetId)) > 0;
        return R.ok(Map.of("data", followed));
    }

    @PutMapping("users/{id:\\d+}/follow-state")
    public R<Void> setFollowState(@PathVariable("id") String idParam, @RequestBody @Valid FollowStateRequest request) {
        Long selfId = currentUserId();
        Long targetId = parseId(idParam);
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (targetId == null || selfId.equals(targetId) || request == null || request.value() == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        boolean exists = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId)
                .eq(UserFollow::getFollowingId, targetId)) > 0;
        if (request.value() && !exists) {
            UserFollow follow = new UserFollow();
            follow.setId(snowflakeIdGenerator.nextId());
            follow.setFollowerId(selfId);
            follow.setFollowingId(targetId);
            follow.setCreatedAt(LocalDateTime.now());
            follow.setUpdatedAt(LocalDateTime.now());
            userFollowMapper.insert(follow);
        } else if (!request.value() && exists) {
            userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, selfId)
                    .eq(UserFollow::getFollowingId, targetId));
        }
        return R.ok(null);
    }

    @GetMapping("users/{id:\\d+}/packages")
    public R<Map<String, Object>> getPackages(@PathVariable("id") String idParam) {
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

    @PostMapping("users/{id:\\d+}/packages")
    public R<Map<String, Long>> addPackage(@PathVariable("id") String idParam, @RequestBody @Valid PackageRequest request) {
        Long selfId = currentUserId();
        Long userId = parseId(idParam);
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

    @PatchMapping("users/{id:\\d+}/packages/{pid:\\d+}")
    public R<Void> editPackage(@PathVariable("id") String idParam, @PathVariable("pid") String pidParam,
                               @RequestBody @Valid PackageRequest request) {
        Long selfId = currentUserId();
        Long userId = parseId(idParam);
        Long pid = parseId(pidParam);
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

    @DeleteMapping("users/{id:\\d+}/packages/{pid:\\d+}")
    public R<Void> deletePackage(@PathVariable("id") String idParam, @PathVariable("pid") String pidParam) {
        Long selfId = currentUserId();
        Long userId = parseId(idParam);
        Long pid = parseId(pidParam);
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

    private R<Void> doUpdateMyProfile(UserUpdateRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        if (request == null || (isBlank(request.username()) && isBlank(request.email()) && isBlank(request.password()))) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = new UserInfo();
        user.setId(selfId);
        if (!isBlank(request.username())) {
            user.setUsername(request.username());
        }
        if (!isBlank(request.email())) {
            user.setEmail(request.email());
        }
        if (!isBlank(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userInfoMapper.updateById(user);
        return R.ok(null);
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
        m.put("createTime", user.getCreatedAt() != null ? user.getCreatedAt().toEpochSecond(ZoneOffset.UTC) : 0L);
        return m;
    }

    private Map<String, Object> userPageResult(Page<UserFollow> page, boolean followingPage, Object keyword, Long selfId) {
        String kw = keyword == null ? "" : keyword.toString().trim();
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserFollow relation : page.getRecords()) {
            Long userId = followingPage ? relation.getFollowingId() : relation.getFollowerId();
            UserInfo user = loadActiveUser(userId);
            if (user == null) {
                continue;
            }
            if (!kw.isEmpty() && !user.getUsername().contains(kw) && !user.getEmail().contains(kw)) {
                continue;
            }
            Map<String, Object> m = toPublicProfile(user);
            m.put("email", user.getEmail());
            m.put("mutual", userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, userId)
                    .eq(UserFollow::getFollowingId, selfId)) > 0);
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

    private FileAsset saveUpload(MultipartFile file, Long ownerId, String type) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        long id = snowflakeIdGenerator.nextId();
        Path dir = Path.of("uploads", type);
        Files.createDirectories(dir);
        Path target = dir.resolve(id + ext).normalize();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        byte[] bytes = Files.readAllBytes(target);
        String hash = HexFormat.of().formatHex(digest.digest(bytes));
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setOwnerUserId(ownerId);
        asset.setAssetType(type);
        asset.setProvider("local");
        asset.setBucket(null);
        asset.setStorageKey(target.toString().replace('\\', '/'));
        asset.setPublicUrl("/uploads/" + type + "/" + target.getFileName());
        asset.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        asset.setSizeBytes(file.getSize());
        asset.setContentHash(hash);
        asset.setStatus(1);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        fileAssetMapper.insert(asset);
        return asset;
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
