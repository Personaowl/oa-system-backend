package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.user.config.AvatarStorageProperties;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private final SysUserMapper userMapper;
    private final AvatarStorageProperties properties;

    public AvatarStorageService(SysUserMapper userMapper, AvatarStorageProperties properties) {
        this.userMapper = userMapper;
        this.properties = properties;
    }

    public void store(Long userId, MultipartFile file) {
        SysUser user = requireUser(userId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择头像文件");
        }
        if (file.getSize() > properties.getMaxBytes()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "头像文件不能超过 1 MB");
        }
        String extension = detectExtension(file);
        Path root = storageRoot();
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = resolveSafely(root, newFileName);
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            String previous = user.getAvatarFileName();
            if (userMapper.updateAvatarFileName(userId, newFileName) != 1) {
                Files.deleteIfExists(target);
                throw new IllegalStateException("保存头像信息失败");
            }
            deleteFileQuietly(root, previous);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像文件保存失败");
        }
    }

    public AvatarContent load(Long userId) {
        SysUser user = requireUser(userId);
        String fileName = user.getAvatarFileName();
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "当前用户未设置头像");
        }
        Path file = resolveSafely(storageRoot(), fileName);
        try {
            if (!Files.isRegularFile(file)) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "头像文件不存在");
            }
            return new AvatarContent(Files.readAllBytes(file), mediaType(fileName));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像文件读取失败");
        }
    }

    public void remove(Long userId) {
        SysUser user = requireUser(userId);
        String previous = user.getAvatarFileName();
        if (previous == null || previous.isBlank()) return;
        if (userMapper.updateAvatarFileName(userId, null) != 1) {
            throw new IllegalStateException("清除头像信息失败");
        }
        deleteFileQuietly(storageRoot(), previous);
    }

    private SysUser requireUser(Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        SysUser user = userMapper.findEnabledById(userId);
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被停用");
        return user;
    }

    private Path storageRoot() {
        return Path.of(properties.getDirectory()).toAbsolutePath().normalize();
    }

    private Path resolveSafely(Path root, String fileName) {
        Path result = root.resolve(fileName).normalize();
        if (!result.startsWith(root)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "非法头像文件路径");
        return result;
    }

    private String detectExtension(MultipartFile file) {
        byte[] header = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(header);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "无法读取头像文件");
        }
        if (length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) return ".png";
        if (length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) return ".jpg";
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return ".webp";
        throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "仅支持 PNG、JPEG 或 WebP 图片");
    }

    private String mediaType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void deleteFileQuietly(Path root, String fileName) {
        if (fileName == null || fileName.isBlank()) return;
        try {
            Files.deleteIfExists(resolveSafely(root, fileName));
        } catch (IOException ignored) {
            // 数据库状态已经更新，残留文件可由后续运维清理。
        }
    }

    public record AvatarContent(byte[] bytes, String mediaType) {}
}
