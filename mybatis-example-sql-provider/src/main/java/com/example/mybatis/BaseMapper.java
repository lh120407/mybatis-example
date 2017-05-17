package com.example.mybatis;

import com.example.mybatis.SqlSourceBuilder;
import org.apache.ibatis.annotations.*;

public interface BaseMapper<T, K> {

    public Long insert(T model);

    public Long updateById(T model);

    public Long deleteById(@Param("id") K id);

    @SelectProvider(type = SqlSourceBuilder.class, method = "build")
    public T getById(@Param("id") K id);

    public Boolean existById(@Param("id") K id);

}
