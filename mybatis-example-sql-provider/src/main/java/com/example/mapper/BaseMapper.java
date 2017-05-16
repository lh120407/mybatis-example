package com.example.mapper;

import com.example.mybatis.BaseMapperProvider;
import org.apache.ibatis.annotations.*;

public interface BaseMapper<T, K> {

    public Long insert(T model);

    public Long updateById(T model);

    public Long deleteById(@Param("id") K id);

    @SelectProvider(type = BaseMapperProvider.class, method = "build")
    public T getById(@Param("id") K id);

    public Boolean existById(@Param("id") K id);

}
