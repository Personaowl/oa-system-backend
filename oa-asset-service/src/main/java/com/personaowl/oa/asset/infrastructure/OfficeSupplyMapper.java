package com.personaowl.oa.asset.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.asset.domain.OfficeSupply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface OfficeSupplyMapper extends BaseMapper<OfficeSupply> {
    @Select("""
        <script>
        SELECT * FROM office_supply
        WHERE deleted=0
        <if test="includeDisabled == false">AND status=1</if>
        <if test="keyword != null and keyword != ''">AND (name LIKE CONCAT('%',#{keyword},'%') OR category LIKE CONCAT('%',#{keyword},'%'))</if>
        ORDER BY category, name
        </script>
        """)
    List<OfficeSupply> findAll(@Param("includeDisabled") boolean includeDisabled, @Param("keyword") String keyword);

    @Select("SELECT * FROM office_supply WHERE id=#{id} AND deleted=0 LIMIT 1")
    OfficeSupply findActiveById(@Param("id") Long id);

    @Update("UPDATE office_supply SET stock_quantity=stock_quantity-#{quantity}, updated_by=#{operatorId}, updated_at=CURRENT_TIMESTAMP, version=version+1 WHERE id=#{id} AND deleted=0 AND status=1 AND stock_quantity>=#{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("operatorId") Long operatorId);

    @Update("""
        UPDATE office_supply SET name=#{supply.name}, category=#{supply.category}, unit=#{supply.unit}, stock_quantity=#{supply.stockQuantity},
        safety_stock=#{supply.safetyStock}, status=#{supply.status}, updated_by=#{operatorId}, updated_at=CURRENT_TIMESTAMP, version=version+1
        WHERE id=#{supply.id} AND version=#{supply.version} AND deleted=0
        """)
    int updateWithVersion(@Param("supply") OfficeSupply supply, @Param("operatorId") Long operatorId);
}
