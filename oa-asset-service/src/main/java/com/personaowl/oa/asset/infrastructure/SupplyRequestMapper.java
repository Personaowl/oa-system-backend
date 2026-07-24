package com.personaowl.oa.asset.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.asset.domain.SupplyRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface SupplyRequestMapper extends BaseMapper<SupplyRequest> {
    String BASE_SELECT = """
        SELECT r.*, applicant.display_name AS applicantName, dept.name AS departmentName,
               supply.name AS supplyName, supply.unit AS supplyUnit, reviewer.display_name AS reviewerName
        FROM office_supply_request r
        JOIN sys_user applicant ON applicant.id=r.applicant_id
        JOIN sys_department dept ON dept.id=r.department_id
        JOIN office_supply supply ON supply.id=r.supply_id
        LEFT JOIN sys_user reviewer ON reviewer.id=r.reviewer_id
        """;

    @Select(BASE_SELECT + " WHERE r.id=#{id} AND r.deleted=0 LIMIT 1")
    SupplyRequest findActiveById(@Param("id") Long id);

    @Select(BASE_SELECT + " WHERE r.applicant_id=#{userId} AND r.deleted=0 ORDER BY r.created_at DESC")
    List<SupplyRequest> findMine(@Param("userId") Long userId);

    @Select("""
        <script>
        """ + BASE_SELECT + """
        WHERE r.deleted=0
        <if test="pendingOnly">AND r.status='PENDING'</if>
        <if test="allAccess == false">
          AND r.department_id IN
          <foreach collection="departmentIds" item="departmentId" open="(" separator="," close=")">#{departmentId}</foreach>
        </if>
        ORDER BY CASE WHEN r.status='PENDING' THEN 0 ELSE 1 END, r.created_at DESC
        </script>
        """)
    List<SupplyRequest> findReviewable(
        @Param("allAccess") boolean allAccess,
        @Param("departmentIds") List<Long> departmentIds,
        @Param("pendingOnly") boolean pendingOnly
    );

    @Update("""
        UPDATE office_supply_request SET status=#{status}, reviewer_id=#{reviewerId}, review_comment=#{comment},
        reviewed_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP, version=version+1
        WHERE id=#{id} AND status='PENDING' AND deleted=0
        """)
    int review(@Param("id") Long id, @Param("status") String status, @Param("reviewerId") Long reviewerId, @Param("comment") String comment);

    @Update("UPDATE office_supply_request SET status='ISSUED', issued_by=#{operatorId}, issued_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=#{id} AND status='APPROVED' AND deleted=0")
    int issue(@Param("id") Long id, @Param("operatorId") Long operatorId);

    @Update("UPDATE office_supply_request SET status='CANCELLED', updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=#{id} AND applicant_id=#{userId} AND status='PENDING' AND deleted=0")
    int cancel(@Param("id") Long id, @Param("userId") Long userId);
}
