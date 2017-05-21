package com.example.mapper;

import com.example.model.User;
import com.example.mybatis.BaseMapper;
import com.example.mybatis.SqlSourceBuilder;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User, Long> {

    @Select("SELECT id,account,password FROM user")
    public List<User> list();

}
