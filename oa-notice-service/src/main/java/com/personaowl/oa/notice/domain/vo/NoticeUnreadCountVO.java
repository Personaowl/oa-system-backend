package com.personaowl.oa.notice.domain.vo;

public class NoticeUnreadCountVO {
    private long unreadCount;

    public NoticeUnreadCountVO() {
    }

    public NoticeUnreadCountVO(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
