package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.user.config.AvatarStorageProperties;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvatarStorageServiceTest {
    @TempDir
    Path tempDir;

    private SysUserMapper userMapper;
    private AvatarStorageService service;
    private SysUser user;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        AvatarStorageProperties properties = new AvatarStorageProperties();
        properties.setDirectory(tempDir.toString());
        properties.setMaxBytes(1024 * 1024);
        service = new AvatarStorageService(userMapper, properties);
        user = new SysUser();
        user.setId(1L);
        user.setStatus(1);
        user.setDeleted(0);
        when(userMapper.findEnabledById(1L)).thenReturn(user);
    }

    @Test
    void storesPngUsingGeneratedSafeFileName() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        when(userMapper.updateAvatarFileName(eq(1L), org.mockito.ArgumentMatchers.anyString())).thenReturn(1);

        service.store(1L, new MockMultipartFile("file", "avatar.exe", "application/octet-stream", png));

        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updateAvatarFileName(eq(1L), fileName.capture());
        assertThat(fileName.getValue()).endsWith(".png").doesNotContain("..");
        assertThat(Files.readAllBytes(tempDir.resolve(fileName.getValue()))).isEqualTo(png);
    }

    @Test
    void rejectsFileWhoseActualContentIsNotAnAllowedImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> service.store(1L, file)).isInstanceOf(BusinessException.class);
    }

    @Test
    void removeClearsDatabaseAndDeletesExistingFile() throws Exception {
        Path avatar = tempDir.resolve("old.jpg");
        Files.write(avatar, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        user.setAvatarFileName("old.jpg");
        when(userMapper.updateAvatarFileName(1L, null)).thenReturn(1);

        service.remove(1L);

        verify(userMapper).updateAvatarFileName(1L, null);
        assertThat(avatar).doesNotExist();
    }
}
