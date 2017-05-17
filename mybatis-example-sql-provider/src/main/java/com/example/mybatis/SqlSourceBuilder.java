package com.example.mybatis;

import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SqlSourceBuilder {

    private static XMLLanguageDriver languageDriver = new XMLLanguageDriver();

    public static void build(Configuration configuration) {
        for (MappedStatement mappedStatement : configuration.getMappedStatements()) {
            if (mappedStatement.getSqlSource() instanceof ProviderSqlSource) {

                Class<?> mapperClass = getMapperClass(mappedStatement);
                Method mapperMethod = getMapperMethod(mappedStatement, mapperClass);
                ResultMap resultMap = getResultMap(mappedStatement, mapperClass);

                String script = null;
                try {
                    script = mapperMethod.invoke(null, mappedStatement, mapperClass, mapperMethod, resultMap).toString();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                SqlSource sqlSource = createSqlSource(mappedStatement, script);
                setSqlSource(mappedStatement, sqlSource);
            }
        }
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

    private static Method getMapperMethod(MappedStatement mappedStatement, Class<?> mapperClass) {
        try {
            String mappedStatementId = mappedStatement.getId();
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
            return mapperClass.getMethod(methodName, MappedStatement.class, Class.class, Method.class, ResultMap.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static ResultMap getResultMap(MappedStatement mappedStatement, Class<?> mapperClass) {
        Configuration configuration = mappedStatement.getConfiguration();
        for (ResultMap resultMap : configuration.getResultMaps())
            if (mapperClass == resultMap.getType() && !resultMap.getId().contains("-"))
                return resultMap;
        return null;
    }

    private static String getById(MappedStatement mappedStatement, Class<?> mapperClass, Method mapperMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            buf.append(String.format("select * from %s where %s = #{id}", tableName, primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String insert(MappedStatement mappedStatement, Class<?> mapperClass, Method mapperMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String updateById(MappedStatement mappedStatement, Class<?> mapperClass, Method mapperMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String deleteById(MappedStatement mappedStatement, Class<?> mapperClass, Method mapperMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            buf.append(String.format("delete from %s where %s = #{id}", tableName, primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String existById(MappedStatement mappedStatement, Class<?> mapperClass, Method mapperMethod, ResultMap resultMap) {
        try {
            StringBuilder buf = new StringBuilder();

            String tableName = getTableName(mapperClass, resultMap);
            String primaryColumnName = getPrimaryColumnName(resultMap);
            buf.append(String.format("select count(*) from %s where %s = #{id}", tableName, primaryColumnName));

            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getTableName (Class<?> mapperClass, ResultMap resultMap) {
        if (resultMap != null)
            return resultMap.getId();
        return toUnderline(mapperClass.getSimpleName());
    }

    private static String getPrimaryColumnName (ResultMap resultMap) {
        ResultMapping resultMapping = null;
        if (resultMap != null) {
            if (resultMap.getIdResultMappings().size() > 0)
                resultMapping = resultMap.getIdResultMappings().get(0);
        }
        if (resultMapping != null)
            return resultMapping.getColumn();
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

}
