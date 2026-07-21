package com.personaowl.oa.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.notice.domain.dto.NoticeCreateRequest;
import com.personaowl.oa.notice.domain.dto.NoticeQueryRequest;
import com.personaowl.oa.notice.domain.dto.NoticeUpdateRequest;
import com.personaowl.oa.notice.domain.entity.Notice;
import com.personaowl.oa.notice.domain.entity.NoticeRead;
import com.personaowl.oa.notice.domain.enums.NoticeStatus;
import com.personaowl.oa.notice.domain.vo.NoticeDetailVO;
import com.personaowl.oa.notice.domain.vo.NoticeListItemVO;
import com.personaowl.oa.notice.domain.vo.NoticePageVO;
import com.personaowl.oa.notice.domain.vo.NoticeUnreadCountVO;
import com.personaowl.oa.notice.mapper.NoticeMapper;
import com.personaowl.oa.notice.mapper.NoticeReadMapper;
import com.personaowl.oa.notice.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {
    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper, NoticeReadMapper noticeReadMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
    }

    @Override
    @Transactional
    public Notice create(NoticeCreateRequest request, Long publisherId) {
        Notice notice = new Notice();
        notice.setTitle(request.getTitle());
        notice.setSummary(request.getSummary());
        notice.setContent(request.getContent());
        notice.setPublisherId(publisherId);
        notice.setCreatedBy(publisherId);
        notice.setUpdatedBy(publisherId);
        notice.setTopFlag(Boolean.TRUE.equals(request.getTopFlag()));
        notice.setStatus(resolveStatus(request.getStatus()));
        notice.setViewCount(0L);
        notice.setDeleted(0);
        notice.setVersion(0);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice;
    }

    @Override
    @Transactional
    public Notice update(Long id, NoticeUpdateRequest request, Long operatorId) {
        Notice notice = requireById(id);
        if (NoticeStatus.OFFLINE.name().equals(notice.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已下线公告不可修改");
        }
        if (NoticeStatus.PUBLISHED.name().equals(notice.getStatus()) && !NoticeStatus.PUBLISHED.name().equals(resolveStatus(request.getStatus()))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已发布公告只能保持发布状态或下线后再修改");
        }
        notice.setTitle(request.getTitle());
        notice.setSummary(request.getSummary());
        notice.setContent(request.getContent());
        notice.setUpdatedBy(operatorId);
        notice.setUpdatedAt(LocalDateTime.now());
        notice.setTopFlag(Boolean.TRUE.equals(request.getTopFlag()));
        notice.setStatus(resolveStatus(request.getStatus()));
        noticeMapper.updateById(notice);
        return notice;
    }

    @Override
    @Transactional
    public Notice delete(Long id, Long operatorId) {
        Notice notice = requireById(id);
        if (NoticeStatus.PUBLISHED.name().equals(notice.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已发布公告不可直接删除，请先下线");
        }
        notice.setDeleted(1);
        notice.setUpdatedBy(operatorId);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return notice;
    }

    @Override
    @Transactional
    public Notice publish(Long id, Long publisherId) {
        Notice notice = requireById(id);
        if (NoticeStatus.PUBLISHED.name().equals(notice.getStatus())) {
            return notice;
        }
        if (NoticeStatus.OFFLINE.name().equals(notice.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已下线公告不可直接发布，请先改回草稿");
        }
        notice.setStatus(NoticeStatus.PUBLISHED.name());
        notice.setPublisherId(publisherId);
        notice.setPublishedAt(LocalDateTime.now());
        notice.setUpdatedBy(publisherId);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return notice;
    }

    @Override
    @Transactional
    public Notice offline(Long id, Long publisherId) {
        Notice notice = requireById(id);
        if (NoticeStatus.OFFLINE.name().equals(notice.getStatus())) {
            return notice;
        }
        notice.setStatus(NoticeStatus.OFFLINE.name());
        notice.setOfflineAt(LocalDateTime.now());
        notice.setUpdatedBy(publisherId);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return notice;
    }

    @Override
    public NoticeDetailVO getById(Long id, Long currentUserId, boolean adminView) {
        Notice notice = requireById(id);
        if (!adminView && !NoticeStatus.PUBLISHED.name().equals(notice.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "公告不可见");
        }
        return toDetailVO(notice, currentUserId);
    }

    @Override
    public NoticePageVO<NoticeListItemVO> listPublished(NoticeQueryRequest request, Long currentUserId) {
        List<Notice> notices = queryNotices(request, true);
        return new NoticePageVO<>(notices.size(), toListItems(notices, currentUserId));
    }

    @Override
    public NoticePageVO<NoticeListItemVO> listAdmin(NoticeQueryRequest request, Long currentUserId) {
        List<Notice> notices = queryNotices(request, false);
        return new NoticePageVO<>(notices.size(), toListItems(notices, currentUserId));
    }

    @Override
    @Transactional
    public NoticeDetailVO read(Long id, Long currentUserId) {
        Notice notice = requireById(id);
        if (!NoticeStatus.PUBLISHED.name().equals(notice.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "公告不可见");
        }
        if (!noticeReadMapper.existsByNoticeIdAndUserId(id, currentUserId)) {
            NoticeRead noticeRead = new NoticeRead();
            noticeRead.setNoticeId(id);
            noticeRead.setUserId(currentUserId);
            noticeRead.setReadAt(LocalDateTime.now());
            noticeReadMapper.insert(noticeRead);
        }
        notice.setViewCount((notice.getViewCount() == null ? 0L : notice.getViewCount()) + 1);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        NoticeDetailVO vo = toDetailVO(notice, currentUserId);
        vo.setRead(true);
        return vo;
    }

    @Override
    public NoticeUnreadCountVO unreadCount(Long currentUserId) {
        return new NoticeUnreadCountVO(noticeReadMapper.countUnreadByUser(currentUserId));
    }

    private List<Notice> queryNotices(NoticeQueryRequest request, boolean publishedOnly) {
        if (request == null) {
            request = new NoticeQueryRequest();
        }
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getDeleted, 0)
                .like(request.getKeyword() != null && !request.getKeyword().isBlank(), Notice::getTitle, request.getKeyword())
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), Notice::getStatus, request.getStatus())
                .eq(request.getTopFlag() != null, Notice::getTopFlag, request.getTopFlag())
                .orderByDesc(Notice::getTopFlag)
                .orderByDesc(Notice::getPublishedAt)
                .orderByDesc(Notice::getUpdatedAt);
        if (publishedOnly) {
            wrapper.eq(Notice::getStatus, NoticeStatus.PUBLISHED.name());
        }
        int page = Math.max(1, request.getPage() == null ? 1 : request.getPage());
        int size = Math.max(1, request.getSize() == null ? 20 : request.getSize());
        List<Notice> all = noticeMapper.selectList(wrapper);
        int fromIndex = Math.min((page - 1) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        if (fromIndex >= toIndex) {
            return Collections.emptyList();
        }
        return all.subList(fromIndex, toIndex);
    }

    private List<NoticeListItemVO> toListItems(List<Notice> notices, Long currentUserId) {
        List<NoticeListItemVO> result = new ArrayList<>();
        for (Notice notice : notices) {
            result.add(toListItemVO(notice, currentUserId));
        }
        return result;
    }

    private Notice requireById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || Integer.valueOf(1).equals(notice.getDeleted())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "公告不存在");
        }
        return notice;
    }

    private String resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return NoticeStatus.DRAFT.name();
        }
        return NoticeStatus.valueOf(status).name();
    }

    private NoticeDetailVO toDetailVO(Notice notice, Long currentUserId) {
        NoticeDetailVO vo = new NoticeDetailVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setSummary(notice.getSummary());
        vo.setContent(notice.getContent());
        vo.setPublisherId(notice.getPublisherId());
        vo.setStatus(notice.getStatus());
        vo.setTopFlag(Boolean.TRUE.equals(notice.getTopFlag()));
        vo.setPublishedAt(notice.getPublishedAt());
        vo.setOfflineAt(notice.getOfflineAt());
        vo.setViewCount(notice.getViewCount());
        vo.setRead(currentUserId != null && noticeReadMapper.existsByNoticeIdAndUserId(notice.getId(), currentUserId));
        return vo;
    }

    private NoticeListItemVO toListItemVO(Notice notice, Long currentUserId) {
        NoticeListItemVO vo = new NoticeListItemVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setSummary(notice.getSummary());
        vo.setStatus(notice.getStatus());
        vo.setTopFlag(Boolean.TRUE.equals(notice.getTopFlag()));
        vo.setPublishedAt(notice.getPublishedAt());
        vo.setViewCount(notice.getViewCount());
        vo.setRead(currentUserId != null && noticeReadMapper.existsByNoticeIdAndUserId(notice.getId(), currentUserId));
        return vo;
    }
}
