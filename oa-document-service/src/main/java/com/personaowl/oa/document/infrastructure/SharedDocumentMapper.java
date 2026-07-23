package com.personaowl.oa.document.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.document.domain.SharedDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SharedDocumentMapper extends BaseMapper<SharedDocument> {

    @Select("""
        <script>
        SELECT d.id, d.department_id, d.title, d.content_json,
               d.created_by, d.updated_by, d.created_at, d.updated_at,
               d.version, d.deleted,
               dept.name AS departmentName,
               creator.display_name AS createdByName,
               updater.display_name AS updatedByName
        FROM shared_document d
        JOIN sys_department dept ON dept.id = d.department_id
        LEFT JOIN sys_user creator ON creator.id = d.created_by
        LEFT JOIN sys_user updater ON updater.id = d.updated_by
        WHERE d.deleted = 0
          AND d.department_id IN
          <foreach collection="departmentIds" item="departmentId" open="(" separator="," close=")">
            #{departmentId}
          </foreach>
        <if test="keyword != null and keyword != ''">
          AND d.title LIKE CONCAT('%', #{keyword}, '%')
        </if>
        ORDER BY d.updated_at DESC, d.id DESC
        </script>
        """)
    List<SharedDocument> findVisible(
        @Param("departmentIds") List<Long> departmentIds,
        @Param("keyword") String keyword
    );

    @Select("""
        SELECT d.id, d.department_id, d.title, d.content_json,
               d.created_by, d.updated_by, d.created_at, d.updated_at,
               d.version, d.deleted,
               dept.name AS departmentName,
               creator.display_name AS createdByName,
               updater.display_name AS updatedByName
        FROM shared_document d
        JOIN sys_department dept ON dept.id = d.department_id
        LEFT JOIN sys_user creator ON creator.id = d.created_by
        LEFT JOIN sys_user updater ON updater.id = d.updated_by
        WHERE d.id = #{id} AND d.deleted = 0
        LIMIT 1
        """)
    SharedDocument findActiveById(@Param("id") Long id);

    @Update("""
        UPDATE shared_document
        SET title = #{title},
            content_json = #{contentJson},
            updated_by = #{updatedBy},
            updated_at = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE id = #{id} AND version = #{version} AND deleted = 0
        """)
    int updateWithVersion(
        @Param("id") Long id,
        @Param("title") String title,
        @Param("contentJson") String contentJson,
        @Param("updatedBy") Long updatedBy,
        @Param("version") Integer version
    );

    @Update("""
        UPDATE shared_document
        SET deleted = 1,
            updated_by = #{updatedBy},
            updated_at = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE id = #{id} AND deleted = 0
        """)
    int softDelete(@Param("id") Long id, @Param("updatedBy") Long updatedBy);
}
