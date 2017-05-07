package com.example.config;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;

public class MybatisConfig {

    private static SqlSessionFactory sessionFactory;

    public static SqlSessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            InputStream inputStream = getResource();
            sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        }
        return sessionFactory;
    }

    private static InputStream getResource() {
        try {
            String resource = "mybatis-config.xml";
            return Resources.getResourceAsStream(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
