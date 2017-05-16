package com.example;

import com.example.config.MybatisConfig;
import com.example.mapper.UserMapper;
import com.example.model.User;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;

public class MapperTest {

    @Test
    public void test() throws Exception {
        SqlSessionFactory sessionFactory = MybatisConfig.getSessionFactory();
        SqlSession session = sessionFactory.openSession();

        UserMapper userMapper = session.getMapper(UserMapper.class);

        User user = new User();

        user = userMapper.getById(1L);
        System.out.println("user:" + user);

        session.commit();
        session.close();
    }

}
