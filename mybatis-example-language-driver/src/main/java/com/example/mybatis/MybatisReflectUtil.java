package com.example.mybatis;

import com.example.mapper.BaseMapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MybatisReflectUtil {

    public static Class<?>[] getMapperGenerics(Class<?> mapperClass) {
        Type[] types = mapperClass.getGenericInterfaces();
        for (Type type : types) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (BaseMapper.class != (Class<?>) parameterizedType.getRawType())
                continue;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Class<?>[] generics = new Class[typeArguments.length];
            for (int i = 0; i < typeArguments.length; i++)
                generics[i] = (Class<?>) typeArguments[i];
            return generics;
        }
        return null;
    }

    public static Field[] getModelField(Class<?> modelClass) {
        List<Field> fields = new ArrayList<>();
        Field[] declaredFields = modelClass.getDeclaredFields();
        for (Field field : declaredFields) {
            if ("serialVersionUID".equals(field.getName()))
                continue;
            fields.add(field);
        }
        return fields.toArray(new Field[0]);
    }

}
