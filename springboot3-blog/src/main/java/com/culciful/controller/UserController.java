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
import com.culciful.dto.RegisterRequest;
import com.culciful.dto.UserUpdateRequest;
import com.culciful.pojo.UserInfo;
import com.culciful.pojo.UserFollow;
import com.culciful.pojo.UserPackage;
import com.culciful.pojo.FileAsset;
import com.culciful.service.EmailVerificationCodeService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
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
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * register
     */
    @PostMapping(value = "register", consumes = MediaType.TEXT_PLAIN_VALUE)
    public R<Void> registerEncrypted(@EncryptedBody @Valid RegisterRequest req) {
        return userService.register(req);
    }

    /**
     * Check whether email already exists.
     */
    @PostMapping("checkEmailExist")
    public R<Map<String, Boolean>> checkEmailExist(@RequestBody @Valid EmailExistParam emailExistParam) {
        return userService.checkEmailExist(emailExistParam);
    }

    /**
     * Public profile query by id.
     */
    @GetMapping("getUserInfo")
    public R<Map<String, Object>> getUserPublicProfile(@RequestParam("id") String idParam) {
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

    @PostMapping(value = "updateUserInfo", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<Void> updateMyProfile(@RequestBody @Valid UserUpdateRequest request) {
        return doUpdateMyProfile(request);
    }

    @PostMapping(value = "updateUserInfo", consumes = MediaType.TEXT_PLAIN_VALUE)
    public R<Void> updateMyProfileEncrypted(@EncryptedBody @Valid UserUpdateRequest request) {
        return doUpdateMyProfile(request);
    }

    @PostMapping(value = "checkPassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<Void> checkPassword(@RequestBody @Valid PasswordCheckRequest request) {
        return doCheckPassword(request);
    }

    @PostMapping(value = "checkPassword", consumes = MediaType.TEXT_PLAIN_VALUE)
    public R<Void> checkPasswordEncrypted(@EncryptedBody @Valid PasswordCheckRequest request) {
        return doCheckPassword(request);
    }

    @GetMapping("getStat")
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

    @PostMapping("getFollowings")
    public R<Map<String, Object>> searchMyFollowings(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        return R.ok(followPageResult(selfId, true, request));
    }

    @PostMapping("getFollowers")
    public R<Map<String, Object>> searchMyFollowers(@RequestBody @Valid PageSearchRequest request) {
        Long selfId = currentUserId();
        if (selfId == null) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        return R.ok(followPageResult(selfId, false, request));
    }

    @PostMapping("sendEmailCode")
    public R<Void> sendEmailCode(@RequestBody @Valid EmailCodeRequest request) {
        String reason = emailVerificationCodeService.sendCode(request.email(), sceneOrDefault(request.scene()));
        if (reason == null) {
            return R.ok(null);
        }
        return switch (reason) {
            case "EMAIL_USED" -> R.fail(ResultCodeEnum.EMAIL_USED);
            case "EMAIL_NOT_FOUND" -> R.fail(ResultCodeEnum.USERNAME_ERROR);
            case "RATE_LIMIT" -> R.fail(ResultCodeEnum.BUSINESS_ERROR);
            default -> R.fail(ResultCodeEnum.BUSINESS_ERROR);
        };
    }

    @PostMapping("checkEmailCode")
    public R<Void> checkEmailCode(@RequestBody @Valid EmailCodeRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.verificationCode())) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        return emailVerificationCodeService.verifyCode(request.email(), request.verificationCode(), sceneOrDefault(request.scene()))
                ? R.ok(null)
                : R.fail(ResultCodeEnum.PARAM_ERROR);
    }

    @PostMapping("uploadAvatar")
    public R<Void> uploadAvatar(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) String image) throws Exception {
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
        boolean followed = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, selfId)
                .eq(UserFollow::getFollowingId, targetId)) > 0;
        return R.ok(Map.of("data", followed));
    }

    @PostMapping("switchFollow")
    public R<Void> setFollowState(@RequestBody @Valid FollowStateRequest request) {
        Long selfId = currentUserId();
        Long targetId = parseId(request.id());
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

    private R<Void> doUpdateMyProfile(UserUpdateRequest request) {
        Long selfId = currentUserId();
        if (request == null || (isBlank(request.username()) && isBlank(request.email()) && isBlank(request.password()))) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        if (selfId == null) {
            return resetPasswordByEmail(request);
        }
        UserInfo user = new UserInfo();
        user.setId(selfId);
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
        }
        if (!isBlank(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userInfoMapper.updateById(user);
        return R.ok(null);
    }

    private R<Void> resetPasswordByEmail(UserUpdateRequest request) {
        if (isBlank(request.email()) || isBlank(request.password()) || isBlank(request.verificationCode())) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
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
        user.setPassword(passwordEncoder.encode(request.password()));
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
        m.put("createTime", user.getCreatedAt() != null ? user.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond() : 0L);
        return m;
    }

    private Map<String, Object> followPageResult(Long selfId, boolean followingPage, PageSearchRequest request) {
        Object keyword = request.safeFilter().get("keyword");
        String kw = keyword == null ? "" : keyword.toString().trim();
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(followingPage ? UserFollow::getFollowerId : UserFollow::getFollowingId, selfId)
                .orderByDesc(UserFollow::getCreatedAt);
        if (!kw.isEmpty()) {
            List<Long> matchedUserIds = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                            .eq(UserInfo::getIsDeleted, false)
                            .and(w -> w.like(UserInfo::getUsername, kw).or().like(UserInfo::getEmail, kw)))
                    .stream()
                    .map(UserInfo::getId)
                    .toList();
            if (matchedUserIds.isEmpty()) {
                return Map.of("list", List.of(), "total", 0);
            }
            wrapper.in(followingPage ? UserFollow::getFollowingId : UserFollow::getFollowerId, matchedUserIds);
        }
        Page<UserFollow> page = userFollowMapper.selectPage(
                new Page<>(request.safeCurrentPage(), request.safePageSize()),
                wrapper
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserFollow relation : page.getRecords()) {
            Long userId = followingPage ? relation.getFollowingId() : relation.getFollowerId();
            UserInfo user = loadActiveUser(userId);
            if (user == null) {
                continue;
            }
            Map<String, Object> m = toPublicProfile(user);
            // 不对外暴露他人邮箱
            boolean iFollowThem = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, selfId)
                    .eq(UserFollow::getFollowingId, userId)) > 0;
            boolean theyFollowMe = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, userId)
                    .eq(UserFollow::getFollowingId, selfId)) > 0;
            m.put("followed", iFollowThem);
            m.put("mutual", iFollowThem && theyFollowMe);
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

    private FileAsset resolveAvatarAsset(MultipartFile file, String image, Long ownerId) throws Exception {
        if (!isBlank(image)) {
            return saveBase64Upload(image, ownerId, "avatar");
        }
        if (file != null && !file.isEmpty()) {
            String original = file.getOriginalFilename();
            if (original == null || original.isBlank()) {
                String text = new String(file.getBytes());
                if (text.startsWith("data:")) {
                    return saveBase64Upload(text, ownerId, "avatar");
                }
            }
            return saveUpload(file, ownerId, "avatar");
        }
        throw new IllegalArgumentException("file is required");
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

    private FileAsset saveBase64Upload(String dataUrl, Long ownerId, String type) throws Exception {
        if (isBlank(dataUrl)) {
            throw new IllegalArgumentException("file is required");
        }
        String mimeType = "image/png";
        String payload = dataUrl;
        if (dataUrl.startsWith("data:")) {
            int semicolon = dataUrl.indexOf(';');
            int comma = dataUrl.indexOf(',');
            if (semicolon > 5) {
                mimeType = dataUrl.substring(5, semicolon);
            }
            if (comma > -1) {
                payload = dataUrl.substring(comma + 1);
            }
        }
        byte[] bytes = Base64.getDecoder().decode(payload);
        String ext = switch (mimeType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
        long id = snowflakeIdGenerator.nextId();
        Path dir = Path.of("uploads", type);
        Files.createDirectories(dir);
        Path target = dir.resolve(id + ext).normalize();
        Files.write(target, bytes);
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setOwnerUserId(ownerId);
        asset.setAssetType(type);
        asset.setProvider("local");
        asset.setBucket(null);
        asset.setStorageKey(target.toString().replace('\\', '/'));
        asset.setPublicUrl("/uploads/" + type + "/" + target.getFileName());
        asset.setMimeType(mimeType);
        asset.setSizeBytes((long) bytes.length);
        asset.setContentHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        asset.setStatus(1);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        fileAssetMapper.insert(asset);
        return asset;
    }

    private String sceneOrDefault(String scene) {
        return isBlank(scene) ? EmailVerificationCodeService.SCENE_REGISTER : scene;
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
