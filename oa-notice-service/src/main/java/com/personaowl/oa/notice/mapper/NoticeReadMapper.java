package com.personaowl.oa.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.notice.domain.entity.NoticeRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NoticeReadMapper extends BaseMapper<NoticeRead> {
    @Select("SELECT COUNT(1) FROM notice n WHERE n.deleted = 0 AND n.status = 'PUBLISHED' AND NOT EXISTS (SELECT 1 FROM notice_read nr WHERE nr.notice_id = n.id AND nr.user_id = #{userId} AND nr.deleted = 0)")
    long countUnreadByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) > 0 FROM notice_read WHERE notice_id = #{noticeId} AND user_id = #{userId} AND deleted = 0")
    boolean existsByNoticeIdAndUserId(@Param("noticeId") Long noticeId, @Param("userId") Long userId);
}
