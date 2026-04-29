package com.company.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.vehicle.entity.FuelRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface FuelRecordMapper extends BaseMapper<FuelRecord> {

    /**
     * 一条 SQL 同时统计各状态计数，替代原来的 7 次独立 COUNT 查询。
     */
    @Select("""
            <script>
            SELECT
              COUNT(*) AS total,
              SUM(CASE WHEN (cash_report_status IS NULL OR cash_report_status = \'\' OR cash_report_status = \'NONE\') THEN 1 ELSE 0 END) AS noApproval,
              SUM(CASE WHEN cash_report_status = \'PENDING\' THEN 1 ELSE 0 END) AS pendingApproval,
              SUM(CASE WHEN cash_report_status = \'APPROVED\' THEN 1 ELSE 0 END) AS approved,
              SUM(CASE WHEN cash_report_status = \'REJECTED\' THEN 1 ELSE 0 END) AS rejected,
              SUM(CASE WHEN (reimbursement_status IS NULL OR reimbursement_status = \'\' OR reimbursement_status = \'NONE\' OR reimbursement_status = \'UNREIMBURSED\') THEN 1 ELSE 0 END) AS unreimbursed,
              SUM(CASE WHEN reimbursement_status = \'REIMBURSED\' THEN 1 ELSE 0 END) AS reimbursed
            FROM fuel_record
            WHERE (deleted IS NULL OR deleted = 0)
            <if test="vehicleId != null">AND vehicle_id = #{vehicleId}</if>
            <if test="driverId != null">AND driver_id = #{driverId}</if>
            <if test="isCash != null">AND is_cash = #{isCash}</if>
            <if test="yearStart != null">AND fuel_date &gt;= #{yearStart}</if>
            <if test="yearEnd != null">AND fuel_date &lt; #{yearEnd}</if>
            <if test="status != null and status != &apos;&apos;">AND cash_report_status = #{status}</if>
            </script>
            """)
    Map<String, Object> querySummaryStats(
            @Param("vehicleId") Long vehicleId,
            @Param("driverId") Long driverId,
            @Param("isCash") Integer isCash,
            @Param("yearStart") LocalDateTime yearStart,
            @Param("yearEnd") LocalDateTime yearEnd,
            @Param("status") String status);
}
