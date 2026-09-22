package com.culciful.service;

import com.culciful.config.UploadProperties;
import com.culciful.mapper.FileAssetMapper;
import com.culciful.pojo.FileAsset;
import com.culciful.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * 统一的图片上传落盘 + 入库。取代原先 ArticleController / UserController 里 3 份重复的
 * saveUpload / saveBase64Upload。
 *
 * <p>加固点：</p>
 * <ul>
 *   <li>大小上限（按用途区分，见 {@link UploadProperties}）</li>
 *   <li>不信任客户端的文件名 / Content-Type：格式由 ImageIO 从字节流嗅探</li>
 *   <li>只接受 jpeg / png / gif（JDK 原生可解码；webp 需额外库，暂不支持）</li>
 *   <li>必须能被 ImageIO 真正解码成栅格图，挡住 .svg / .html / 脚本 / 畸形文件</li>
 *   <li>限制解码后像素总数，挡「解压炸弹」</li>
 *   <li>png / jpeg 重新编码落盘，抹掉 EXIF 和 polyglot 载荷；gif 原样保留（动图）</li>
 *   <li>文件名用雪花 ID + 规范化扩展名，落盘路径校验不逃出根目录</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    public enum Kind {
        AVATAR("avatar"),
        ARTICLE_IMAGE("article_image"),
        COMMENT_IMAGE("comment_image");

        private final String assetType;

        Kind(String assetType) {
            this.assetType = assetType;
        }
    }

    /** 小写格式名白名单（ImageIO reader 报出来的 formatName） */
    private static final Set<String> ALLOWED = Set.of("jpeg", "jpg", "png", "gif");

    private final FileAssetMapper fileAssetMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final UploadProperties properties;

    public FileAsset store(MultipartFile file, Long ownerId, Kind kind) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        try {
            return persist(file.getBytes(), ownerId, kind);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read uploaded file", e);
        }
    }

    /** data:image/png;base64,xxxx 或裸 base64（头像裁剪结果） */
    public FileAsset storeDataUrl(String dataUrl, Long ownerId, Kind kind) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("file is required");
        }
        String payload = dataUrl;
        int comma = dataUrl.indexOf(',');
        if (dataUrl.startsWith("data:") && comma > -1) {
            payload = dataUrl.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid base64 image", e);
        }
        return persist(bytes, ownerId, kind);
    }

    /**
     * 删掉一份已落库的图（DB 行 + 磁盘文件）。目前唯一的用途是换头像时清理旧头像
     * （FileAsset 没有 is_deleted 概念，不受 MP 全局逻辑删除影响，{@code deleteById} 是真删）。
     * 磁盘删除失败只记日志不抛异常——DB 行已经删了，重要的一致性状态已经达到，孤儿文件
     * 最多是浪费点磁盘空间，不该让调用方因为这个失败（换头像这个操作本身应该已经成功了）。
     */
    public void delete(FileAsset asset) {
        if (asset == null) {
            return;
        }
        fileAssetMapper.deleteById(asset.getId());
        try {
            Files.deleteIfExists(Path.of(asset.getStorageKey()));
        } catch (IOException e) {
            log.warn("failed to delete upload file on disk: {}", asset.getStorageKey(), e);
        }
    }

    private FileAsset persist(byte[] bytes, Long ownerId, Kind kind) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("file is required");
        }
        long maxBytes = (kind == Kind.AVATAR ? properties.getAvatarMaxSize() : properties.getImageMaxSize()).toBytes();
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("image exceeds size limit: " + maxBytes + " bytes");
        }

        // 1. 只读 header：拿真实格式 + 声明的尺寸（不分配栅格）
        Header header = readHeader(bytes);
        if (header == null) {
            throw new IllegalArgumentException("not a decodable image");
        }
        String format = header.format();
        if (!ALLOWED.contains(format)) {
            throw new IllegalArgumentException("unsupported image type: " + format);
        }
        // 2. 解压炸弹防护：在真正解码分配像素之前挡掉
        if (header.pixels() > properties.getMaxPixels()) {
            throw new IllegalArgumentException("image dimensions too large");
        }

        // 3. 现在才真正解码
        BufferedImage image = decodePixels(bytes);
        if (image == null) {
            throw new IllegalArgumentException("cannot decode image");
        }
        if ((long) image.getWidth() * image.getHeight() > properties.getMaxPixels()) {
            throw new IllegalArgumentException("image dimensions too large");
        }

        String canonical = "jpg".equals(format) ? "jpeg" : format;
        String ext = "jpeg".equals(canonical) ? ".jpg" : "." + canonical;
        String mimeType = "image/" + canonical;
        byte[] content = "gif".equals(canonical) ? bytes : reEncode(image, canonical);

        long id = idGenerator.nextId();
        Path root = Path.of(properties.getDir()).toAbsolutePath().normalize();
        Path dir = root.resolve(kind.assetType);
        Path target = dir.resolve(id + ext).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("resolved path escapes upload root");
        }
        try {
            Files.createDirectories(dir);
            Files.write(target, content);
        } catch (IOException e) {
            throw new IllegalStateException("failed to write upload", e);
        }

        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setOwnerUserId(ownerId);
        asset.setAssetType(kind.assetType);
        asset.setProvider("local");
        asset.setBucket(null);
        asset.setStorageKey(properties.getDir() + "/" + kind.assetType + "/" + target.getFileName());
        asset.setPublicUrl("/" + properties.getDir() + "/" + kind.assetType + "/" + target.getFileName());
        asset.setMimeType(mimeType);
        asset.setSizeBytes((long) content.length);
        asset.setContentHash(sha256Hex(content));
        asset.setWidth(image.getWidth());
        asset.setHeight(image.getHeight());
        asset.setStatus(1);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        fileAssetMapper.insert(asset);
        return asset;
    }

    private record Header(String format, long pixels) {
    }

    /** 只读图像 header：真实格式 + 宽高（不解码像素，避免解压炸弹）。非法图片返回 null。 */
    private static Header readHeader(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                return new Header(format, pixels);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** 真正解码成栅格。仅在 header 尺寸校验通过后调用。 */
    private static BufferedImage decodePixels(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static byte[] reEncode(BufferedImage image, String format) {
        BufferedImage source = image;
        if ("jpeg".equals(format) && image.getColorModel().hasAlpha()) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(image, 0, 0, Color.WHITE, null);
            g.dispose();
            source = rgb;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(source, format, out)) {
                throw new IllegalStateException("no ImageIO writer for " + format);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to re-encode image", e);
        }
        return out.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
