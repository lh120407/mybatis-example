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

        user.setId(111L);
        user.setAccount("conanli");
        userMapper.insert(user);
        System.out.println("insert: " + userMapper.getById(111L));

        user = new User();
        user.setId(111L);
        user.setPassword("123456");
        System.out.println("update: " + userMapper.updateById(user));

        userMapper.deleteById(111L);
        System.out.println("exist:" + userMapper.existById(111L));

        session.commit();
        session.close();
    }

}
