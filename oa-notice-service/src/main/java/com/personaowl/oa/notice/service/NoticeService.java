package com.personaowl.oa.notice.service;

import com.personaowl.oa.notice.domain.dto.NoticeCreateRequest;
import com.personaowl.oa.notice.domain.dto.NoticeQueryRequest;
import com.personaowl.oa.notice.domain.dto.NoticeUpdateRequest;
import com.personaowl.oa.notice.domain.entity.Notice;
import com.personaowl.oa.notice.domain.vo.NoticeDetailVO;
import com.personaowl.oa.notice.domain.vo.NoticeListItemVO;
import com.personaowl.oa.notice.domain.vo.NoticePageVO;
import com.personaowl.oa.notice.domain.vo.NoticeUnreadCountVO;

public interface NoticeService {
    Notice create(NoticeCreateRequest request, Long publisherId);

    Notice update(Long id, NoticeUpdateRequest request, Long operatorId);

    Notice delete(Long id, Long operatorId);

    Notice publish(Long id, Long publisherId);

    Notice offline(Long id, Long publisherId);

    NoticeDetailVO getById(Long id, Long currentUserId, boolean adminView);

    NoticePageVO<NoticeListItemVO> listPublished(NoticeQueryRequest request, Long currentUserId);

    NoticePageVO<NoticeListItemVO> listAdmin(NoticeQueryRequest request, Long currentUserId);

    NoticeDetailVO read(Long id, Long currentUserId);

    NoticeUnreadCountVO unreadCount(Long currentUserId);
}
