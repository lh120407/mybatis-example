package com.example.mybatis;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class SqlSourceBuilder {

    private static XMLLanguageDriver languageDriver = new XMLLanguageDriver();

    public static String build(Configuration configuration) {
        for (MappedStatement mappedStatement : configuration.getMappedStatements()) {
            if (mappedStatement.getSqlSource() instanceof ProviderSqlSource) {

                Class<?> mapperClass = getMapperClass(mappedStatement);
                Class<?>[] generics = getMapperGenerics(mapperClass);
                Class<?> modelClass = generics[0];
                Class<?> primaryFieldClass = generics[1];
                Method builderMethod = getBuilderMethod(mappedStatement, SqlSourceBuilder.class, MappedStatement.class, Class.class, Class.class, Class.class, Method.class, ResultMap.class);
                ResultMap resultMap = getResultMap(mappedStatement, modelClass);

                String script = null;
                try {
                    script = builderMethod.invoke(null, mappedStatement, mapperClass, modelClass, primaryFieldClass, builderMethod, resultMap).toString();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                SqlSource sqlSource = createSqlSource(mappedStatement, script);
                setSqlSource(mappedStatement, sqlSource);
            }
        }
        return "sql";
    }


    private static SqlSource createSqlSource(MappedStatement mappedStatement, String script) {
        return languageDriver.createSqlSource(mappedStatement.getConfiguration(), "<script>" + script + "</script>", null);
    }

    private static void setSqlSource(MappedStatement mappedStatement, SqlSource sqlSource) {
        MetaObject metaObject = SystemMetaObject.forObject(mappedStatement);
        metaObject.setValue("sqlSource", sqlSource);
    }

    private static Class<?> getMapperClass(MappedStatement mappedStatement) {
        try {
            String mappedStatementId = mappedStatement.getId();
            String className = mappedStatementId.substring(0, mappedStatementId.lastIndexOf("."));
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method getMapperMethod(MappedStatement mappedStatement, Class<?> mapperClass, Class<?>... parameterTypes) {
        try {
            String mappedStatementId = mappedStatement.getId();
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
            return mapperClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Type type = mapperClass.getSuperclass();
            if (type != null) {
                Method method =  getMapperMethod(mappedStatement, (Class<?>) type, parameterTypes);
                if (method != null)
                    return method;
            }
            Type[] types = mapperClass.getInterfaces();
            for (Type type2 : types) {
                Method method =  getMapperMethod(mappedStatement, (Class<?>) type2, parameterTypes);
                if (method != null)
                    return method;
            }
            return null;
        }
    }

    private static Method getMapperMethod0(MappedStatement mappedStatement, Class<?> mapperClass, Class<?>... parameterTypes) {
        String mappedStatementId = mappedStatement.getId();
        String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
        Method[] methods = mapperClass.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName))
                continue;
            Class<?>[] types = method.getParameterTypes();
            if (types.length == parameterTypes.length) {
                boolean isEqual = true;
                for (int i = 0; i < types.length; i ++) {
                    if (types[i] == Object.class)
                        continue;
                    if (types[i] != parameterTypes[i])
                        isEqual = false;
                }
                if (isEqual)
                    return method;
            }
        }
        return null;
    }

    private static Method getBuilderMethod(MappedStatement mappedStatement, Class<?> builderClass, Class<?>... parameterTypes) {
        try {
            String mappedStatementId = mappedStatement.getId();
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
            return builderClass.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static ResultMap getResultMap(MappedStatement mappedStatement, Class<?> modelClass) {
        Configuration configuration = mappedStatement.getConfiguration();
        for (ResultMap resultMap : configuration.getResultMaps())
            if (modelClass == resultMap.getType() && !resultMap.getId().contains("-"))
                return resultMap;
        return null;
    }

    private static String getById(MappedStatement mappedStatement, Class<?> mapperClass, Class<?> modelClass, Class<?> primaryFieldClass, Method builderMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, modelClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            buf.append(String.format("SELECT * FROM %s WHERE %s = #{id}", tableName, primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String insert(MappedStatement mappedStatement, Class<?> mapperClass, Class<?> modelClass, Class<?> primaryFieldClass, Method builderMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            Boolean generated = false;

            Method mapperMethod = getMapperMethod0(mappedStatement, mapperClass, modelClass);
            Options methodOptions = mapperMethod.getAnnotation(Options.class);
            if (methodOptions != null) {
                generated = methodOptions.useGeneratedKeys();
            }

            String tableName = getTableName(mapperClass, modelClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            Field[] fields = getModelField(modelClass);

            buf.append(String.format("INSERT INTO %s", tableName));

            buf.append("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
            for (Field field : fields) {
                if (generated && primaryColumnName.equals(field.getName()))
                    continue;
                buf.append(String.format("<if test=\"%s != null and %s != ''\">", field.getName(), field.getName()));
                ResultMapping resultMapping = getResultMapping(resultMap, field.getName());
                buf.append(String.format("%s,", resultMapping == null ? field.getName() : resultMapping.getColumn()));
                buf.append("</if>");
            }
            buf.append("</trim>");

            buf.append("VALUE");

            buf.append("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
            for (Field field : fields) {
                if (generated && primaryColumnName.equals(field.getName()))
                    continue;
                buf.append(String.format("<if test=\"%s != null and %s != ''\">", field.getName(), field.getName()));
                buf.append(String.format("#{%s},", field.getName()));
                buf.append("</if>");
            }
            buf.append("</trim>");

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String updateById(MappedStatement mappedStatement, Class<?> mapperClass, Class<?> modelClass, Class<?> primaryFieldClass, Method builderMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, modelClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            Field[] fields = getModelField(modelClass);

            buf.append(String.format("UPDATE %s ", tableName));

            buf.append("<set>");
            for (Field field : fields) {
                if (primaryColumnName.equals(field.getName()))
                    continue;
                buf.append(String.format("<if test=\"%s != null and %s != ''\">", field.getName(), field.getName()));
                ResultMapping resultMapping = getResultMapping(resultMap, field.getName());
                buf.append(String.format("%s = #{%s},", resultMapping == null ? field.getName() : resultMapping.getColumn(), field.getName()));
                buf.append("</if>");
            }
            buf.append("</set>");

            buf.append(String.format("WHERE %s = #{id}", primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String deleteById(MappedStatement mappedStatement, Class<?> mapperClass, Class<?> modelClass, Class<?> primaryFieldClass, Method builderMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, modelClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            buf.append(String.format("DELETE FROM %s WHERE %s = #{id}", tableName, primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String existById(MappedStatement mappedStatement, Class<?> mapperClass, Class<?> modelClass, Class<?> primaryFieldClass, Method builderMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, modelClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            buf.append(String.format("SELECT COUNT(*) FROM %s WHERE %s = #{id}", tableName, primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getTableName(Class<?> mapperClass, Class<?> modelClass, ResultMap resultMap) {
        if (resultMap != null)
            return resultMap.getId().substring(mapperClass.getName().length() + 1);
        return toUnderline(modelClass.getSimpleName());
    }

    private static String getPrimaryColumnName(ResultMap resultMap) {
        ResultMapping resultMapping = null;
        if (resultMap != null) {
            if (resultMap.getIdResultMappings().size() > 0)
                resultMapping = resultMap.getIdResultMappings().get(0);
        }
        if (resultMapping != null)
            return resultMapping.getColumn();
        return null;
    }

    private static ResultMapping getResultMapping(ResultMap resultMap, String fieldName) {
        if (resultMap != null) {
            for (ResultMapping resultMapping : resultMap.getResultMappings()) {
                if (resultMapping.getProperty().equals(fieldName))
                    return resultMapping;
            }
        }
        return null;
    }

    private static String toUnderline(String str) {
        StringBuilder buf = new StringBuilder();
        buf.append(Character.toLowerCase(str.charAt(0)));
        for (int i = 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                buf.append("_" + Character.toLowerCase(c));
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

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
