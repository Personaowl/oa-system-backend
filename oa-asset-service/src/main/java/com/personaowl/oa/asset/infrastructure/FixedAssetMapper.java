package com.personaowl.oa.asset.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.asset.domain.FixedAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface FixedAssetMapper extends BaseMapper<FixedAsset> {
    String BASE_SELECT = """
        SELECT a.*, u.display_name AS custodianName, d.name AS departmentName
        FROM fixed_asset a
        LEFT JOIN sys_user u ON u.id=a.custodian_id
        LEFT JOIN sys_department d ON d.id=a.department_id
        """;

    @Select("""
        <script>
        """ + BASE_SELECT + """
        WHERE a.deleted=0
        <if test="allAccess == false">
          AND (a.custodian_id=#{userId}
          <if test="departmentIds != null and departmentIds.size() > 0">
            OR a.department_id IN
            <foreach collection="departmentIds" item="departmentId" open="(" separator="," close=")">#{departmentId}</foreach>
          </if>)
        </if>
        <if test="keyword != null and keyword != ''">AND (a.asset_code LIKE CONCAT('%',#{keyword},'%') OR a.name LIKE CONCAT('%',#{keyword},'%') OR a.category LIKE CONCAT('%',#{keyword},'%') OR u.display_name LIKE CONCAT('%',#{keyword},'%'))</if>
        <if test="status != null and status != ''">AND a.status=#{status}</if>
        ORDER BY a.updated_at DESC, a.id DESC
        </script>
        """)
    List<FixedAsset> findVisible(@Param("allAccess") boolean allAccess, @Param("departmentIds") List<Long> departmentIds,
                                 @Param("userId") Long userId, @Param("keyword") String keyword, @Param("status") String status);

    @Select(BASE_SELECT + " WHERE a.id=#{id} AND a.deleted=0 LIMIT 1")
    FixedAsset findActiveById(@Param("id") Long id);

    @Update("""
        UPDATE fixed_asset SET asset_code=#{asset.assetCode}, name=#{asset.name}, category=#{asset.category}, specification=#{asset.specification},
        purchase_date=#{asset.purchaseDate}, original_value=#{asset.originalValue}, status=#{asset.status}, location=#{asset.location}, remark=#{asset.remark},
        custodian_id=#{asset.custodianId}, department_id=#{asset.departmentId}, updated_by=#{operatorId}, updated_at=CURRENT_TIMESTAMP, version=version+1
        WHERE id=#{asset.id} AND version=#{asset.version} AND deleted=0
        """)
    int updateWithVersion(@Param("asset") FixedAsset asset, @Param("operatorId") Long operatorId);

    @Update("UPDATE fixed_asset SET status='IN_USE', custodian_id=#{custodianId}, department_id=#{departmentId}, location=#{location}, updated_by=#{operatorId}, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=#{id} AND version=#{version} AND status='IDLE' AND deleted=0")
    int assign(@Param("id") Long id, @Param("custodianId") Long custodianId, @Param("departmentId") Long departmentId,
               @Param("location") String location, @Param("operatorId") Long operatorId, @Param("version") Integer version);

    @Update("UPDATE fixed_asset SET status='IDLE', custodian_id=NULL, department_id=NULL, location=#{location}, updated_by=#{operatorId}, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=#{id} AND version=#{version} AND status='IN_USE' AND deleted=0")
    int returnAsset(@Param("id") Long id, @Param("location") String location, @Param("operatorId") Long operatorId, @Param("version") Integer version);

    @Update("UPDATE fixed_asset SET deleted=1, updated_by=#{operatorId}, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=#{id} AND status != 'IN_USE' AND deleted=0")
    int softDelete(@Param("id") Long id, @Param("operatorId") Long operatorId);
}
