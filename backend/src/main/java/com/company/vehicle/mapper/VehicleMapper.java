package com.company.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.vehicle.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    @Select("SELECT * FROM vehicle WHERE id = #{id} FOR UPDATE")
    Vehicle selectByIdForUpdate(Long id);
}
