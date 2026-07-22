package com.personaowl.oa.notice.mapper;

import com.personaowl.oa.notice.domain.entity.Notice;
import com.personaowl.oa.notice.domain.entity.NoticeRead;
import com.personaowl.oa.notice.domain.enums.NoticeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class NoticeMapperIntegrationTest {
    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private NoticeReadMapper noticeReadMapper;

    @Test
    void shouldInsertAndQueryNoticeAndReadState() {
        long noticeId = System.currentTimeMillis();
        long userId = 9988L;
        long unreadBefore = noticeReadMapper.countUnreadByUser(userId);

        Notice notice = new Notice();
        notice.setId(noticeId);
        notice.setTitle("集成测试公告");
        notice.setSummary("摘要");
        notice.setContent("内容");
        notice.setPublisherId(100L);
        notice.setStatus(NoticeStatus.PUBLISHED.name());
        notice.setTopFlag(false);
        notice.setPublishedAt(LocalDateTime.now());
        notice.setViewCount(0L);
        notice.setCreatedBy(100L);
        notice.setUpdatedBy(100L);
        notice.setDeleted(0);
        notice.setVersion(0);
        noticeMapper.insert(notice);

        Notice found = noticeMapper.selectById(noticeId);
        assertNotNull(found);
        assertEquals("集成测试公告", found.getTitle());

        NoticeRead read = new NoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadAt(LocalDateTime.now());
        noticeReadMapper.insert(read);

        assertTrue(noticeReadMapper.existsByNoticeIdAndUserId(noticeId, userId));
        assertEquals(unreadBefore, noticeReadMapper.countUnreadByUser(userId));
    }
}
