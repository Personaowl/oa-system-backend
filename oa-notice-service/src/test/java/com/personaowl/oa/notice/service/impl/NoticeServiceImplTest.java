package com.personaowl.oa.notice.service.impl;

import com.personaowl.oa.notice.domain.dto.NoticeCreateRequest;
import com.personaowl.oa.notice.domain.dto.NoticeQueryRequest;
import com.personaowl.oa.notice.domain.dto.NoticeUpdateRequest;
import com.personaowl.oa.notice.domain.entity.Notice;
import com.personaowl.oa.notice.domain.entity.NoticeRead;
import com.personaowl.oa.notice.domain.enums.NoticeStatus;
import com.personaowl.oa.notice.mapper.NoticeMapper;
import com.personaowl.oa.notice.mapper.NoticeReadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {
    @Mock
    private NoticeMapper noticeMapper;

    @Mock
    private NoticeReadMapper noticeReadMapper;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private NoticeCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new NoticeCreateRequest();
        createRequest.setTitle("公告标题");
        createRequest.setContent("公告内容");
        createRequest.setStatus(NoticeStatus.DRAFT.name());
    }

    @Test
    void createNoticeShouldSetDraftByDefault() {
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> 1);

        Notice notice = noticeService.create(createRequest, 100L);

        assertEquals("公告标题", notice.getTitle());
        assertEquals("公告内容", notice.getContent());
        assertEquals(100L, notice.getPublisherId());
        assertEquals(NoticeStatus.DRAFT.name(), notice.getStatus());
        assertEquals(0L, notice.getViewCount());
        assertEquals(0, notice.getDeleted());
    }

    @Test
    void publishShouldThrowWhenNoticeMissing() {
        when(noticeMapper.selectById(1L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> noticeService.publish(1L, 100L));
    }

    @Test
    void updateShouldRejectOfflineNotice() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(NoticeStatus.OFFLINE.name());
        when(noticeMapper.selectById(1L)).thenReturn(notice);

        NoticeUpdateRequest request = new NoticeUpdateRequest();
        request.setTitle("新标题");
        request.setContent("新内容");

        assertThrows(RuntimeException.class, () -> noticeService.update(1L, request, 100L));
    }

    @Test
    void listPublishedShouldReturnOnlyPublishedNotices() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setTitle("已发布公告");
        notice.setStatus(NoticeStatus.PUBLISHED.name());
        notice.setDeleted(0);
        when(noticeMapper.selectList(any())).thenReturn(List.of(notice));
        when(noticeReadMapper.existsByNoticeIdAndUserId(1L, 200L)).thenReturn(false);

        var result = noticeService.listPublished(null, 200L);

        assertEquals(1, result.getRecords().size());
        assertEquals("已发布公告", result.getRecords().get(0).getTitle());
        assertFalse(result.getRecords().get(0).isRead());
    }

    @Test
    void unreadCountShouldDelegateToMapper() {
        when(noticeReadMapper.countUnreadByUser(300L)).thenReturn(5L);

        assertEquals(5L, noticeService.unreadCount(300L).getUnreadCount());
    }

    @Test
    void readShouldMarkAsReadAndIncreaseViewCount() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setTitle("公告");
        notice.setStatus(NoticeStatus.PUBLISHED.name());
        notice.setDeleted(0);
        notice.setViewCount(2L);
        when(noticeMapper.selectById(1L)).thenReturn(notice);
        when(noticeReadMapper.existsByNoticeIdAndUserId(1L, 400L)).thenReturn(false);
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);
        when(noticeReadMapper.insert(any(NoticeRead.class))).thenReturn(1);

        var vo = noticeService.read(1L, 400L);

        assertTrue(vo.isRead());
        assertEquals(3L, vo.getViewCount());
    }

    @Test
    void publicDetailShouldRejectOfflineNotice() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(NoticeStatus.OFFLINE.name());
        notice.setDeleted(0);
        when(noticeMapper.selectById(1L)).thenReturn(notice);

        assertThrows(RuntimeException.class, () -> noticeService.getById(1L, 500L, false));
    }
}
