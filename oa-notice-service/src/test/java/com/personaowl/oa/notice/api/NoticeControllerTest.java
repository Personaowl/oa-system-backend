package com.personaowl.oa.notice.api;

import com.personaowl.oa.common.web.CommonWebAutoConfiguration;
import com.personaowl.oa.notice.domain.dto.NoticeCreateRequest;
import com.personaowl.oa.notice.domain.dto.NoticeUpdateRequest;
import com.personaowl.oa.notice.domain.entity.Notice;
import com.personaowl.oa.notice.domain.enums.NoticeStatus;
import com.personaowl.oa.notice.domain.vo.NoticeDetailVO;
import com.personaowl.oa.notice.domain.vo.NoticeListItemVO;
import com.personaowl.oa.notice.domain.vo.NoticePageVO;
import com.personaowl.oa.notice.domain.vo.NoticeUnreadCountVO;
import com.personaowl.oa.notice.service.NoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
@Import(CommonWebAutoConfiguration.class)
class NoticeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoticeService noticeService;

    @Test
    void createShouldReturnSuccess() throws Exception {
        Notice notice = new Notice();
        notice.setId(2079489406225780737L);
        notice.setTitle("公告标题");
        notice.setStatus(NoticeStatus.DRAFT.name());
        when(noticeService.create(any(NoticeCreateRequest.class), eq(100L))).thenReturn(notice);

        mockMvc.perform(post("/api/v1/notices")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:create,notice:list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"公告标题","content":"公告内容","status":"DRAFT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value("2079489406225780737"))
                .andExpect(jsonPath("$.data.title").value("公告标题"));
    }

    @Test
    void createWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/notices")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"公告标题","content":"公告内容","status":"DRAFT"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void createWithoutUserIdShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/notices")
                        .header("X-Permissions", "notice:create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"公告标题","content":"公告内容","status":"DRAFT"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"));
    }

    @Test
    void listAdminShouldReturnPage() throws Exception {
        NoticeListItemVO item = new NoticeListItemVO();
        item.setId(1L);
        item.setTitle("公告标题");
        when(noticeService.listAdmin(any(), eq(100L))).thenReturn(new NoticePageVO<>(1L, List.of(item)));

        mockMvc.perform(get("/api/v1/notices")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("公告标题"));
    }

    @Test
    void listAdminWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notices")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void publicListShouldReturnPage() throws Exception {
        NoticeListItemVO item = new NoticeListItemVO();
        item.setId(1L);
        item.setTitle("公告标题");
        when(noticeService.listPublished(any(), eq(100L))).thenReturn(new NoticePageVO<>(1L, List.of(item)));

        mockMvc.perform(get("/api/v1/notices/public").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("公告标题"));
    }

    @Test
    void publicListWithoutUserIdShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notices/public"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"));
    }

    @Test
    void unreadCountShouldReturnValue() throws Exception {
        when(noticeService.unreadCount(100L)).thenReturn(new NoticeUnreadCountVO(3L));

        mockMvc.perform(get("/api/v1/notices/public/unread-count").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void readShouldReturnDetail() throws Exception {
        NoticeDetailVO vo = new NoticeDetailVO();
        vo.setId(1L);
        vo.setTitle("公告标题");
        vo.setStatus(NoticeStatus.PUBLISHED.name());
        vo.setRead(true);
        when(noticeService.read(1L, 100L)).thenReturn(vo);

        mockMvc.perform(post("/api/v1/notices/1/read").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    void updateShouldReturnSuccess() throws Exception {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setTitle("更新后");
        when(noticeService.update(any(Long.class), any(NoticeUpdateRequest.class), eq(100L))).thenReturn(notice);

        mockMvc.perform(put("/api/v1/notices/1")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"更新后","content":"内容"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("更新后"));
    }

    @Test
    void updateWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/notices/1")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"更新后","content":"内容"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void deleteShouldReturnSuccess() throws Exception {
        Notice notice = new Notice();
        notice.setId(1L);
        when(noticeService.delete(1L, 100L)).thenReturn(notice);

        mockMvc.perform(delete("/api/v1/notices/1")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void deleteWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/notices/1")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void publishShouldReturnSuccess() throws Exception {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(NoticeStatus.PUBLISHED.name());
        when(noticeService.publish(1L, 100L)).thenReturn(notice);

        mockMvc.perform(post("/api/v1/notices/1/publish")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void publishWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/notices/1/publish")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void offlineShouldReturnSuccess() throws Exception {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(NoticeStatus.OFFLINE.name());
        when(noticeService.offline(1L, 100L)).thenReturn(notice);

        mockMvc.perform(post("/api/v1/notices/1/offline")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:offline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));
    }

    @Test
    void offlineWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/notices/1/offline")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void detailShouldReturnSuccess() throws Exception {
        NoticeDetailVO vo = new NoticeDetailVO();
        vo.setId(1L);
        vo.setTitle("公告标题");
        when(noticeService.getById(1L, 100L, true)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/notices/1")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("公告标题"));
    }

    @Test
    void detailWithoutPermissionShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notices/1")
                        .header("X-User-Id", "100")
                        .header("X-Permissions", "notice:list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }
}
