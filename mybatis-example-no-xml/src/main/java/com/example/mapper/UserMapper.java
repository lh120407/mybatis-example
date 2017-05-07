package com.example.mapper;

import com.example.model.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    public User getById(@Param("id") Long id);

}
