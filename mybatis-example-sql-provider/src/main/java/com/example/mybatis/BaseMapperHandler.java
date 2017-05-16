package com.example.mybatis;

import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

public class BaseMapperHandler {

    private XMLLanguageDriver languageDriver = new XMLLanguageDriver();

    public void handle(Configuration configuration) {
        for (MappedStatement mappedStatement : configuration.getMappedStatements()) {
            if (mappedStatement.getSqlSource() instanceof ProviderSqlSource) {

                Class<?> mapperClass = getMapperClass(mappedStatement);
                String

                // DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(mappedStatement.getConfiguration(), sqlNode);
                SqlSource sqlSource = createSqlSource(mappedStatement, "select * from user where id=#{id}");
                setSqlSource(mappedStatement, sqlSource);
            }
        }
    }

    protected void setSqlSource(MappedStatement mappedStatement, SqlSource sqlSource) {
        MetaObject metaObject = SystemMetaObject.forObject(mappedStatement);
        metaObject.setValue("sqlSource", sqlSource);
    }

    public SqlSource createSqlSource(MappedStatement mappedStatement, String xmlSql) {
        return languageDriver.createSqlSource(mappedStatement.getConfiguration(), "<script>\n\t" + xmlSql + "</script>", null);
    }

    public Class<?> getMapperClass(MappedStatement mappedStatement) {
        try {
            String mappedStatementId = mappedStatement.getId();
            String mapperClassName = mappedStatementId.substring(0, mappedStatementId.lastIndexOf("."));
            return Class.forName(mapperClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public String getMethodName(MappedStatement mappedStatement) {
        String mappedStatementId = mappedStatement.getId();
        return mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
    }

}
