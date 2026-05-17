package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("file_asset")
public class FileAsset {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("asset_type")
    private String assetType;

    @TableField("provider")
    private String provider;

    @TableField("bucket")
    private String bucket;

    @TableField("storage_key")
    private String storageKey;

    @TableField("public_url")
    private String publicUrl;

    @TableField("mime_type")
    private String mimeType;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("content_hash")
    private String contentHash;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
