package com.example.mapper;

import com.example.model.User;
import com.example.mybatis.BaseMapper;
import com.example.mybatis.SqlSourceBuilder;
import org.apache.ibatis.annotations.InsertProvider;

public interface UserMapper extends BaseMapper<User, Long> {

    @InsertProvider(type = SqlSourceBuilder.class, method = "build")
    public Long insert(User model);

}
