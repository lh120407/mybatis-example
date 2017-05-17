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

        User user = null;

        // 新增测试
        user = new User();
        user.setId(22L);
        user.setAccount("conanli");
        user.setPassword("123456");
        System.out.println("insert: " + userMapper.insert(user));
        System.out.println("user:" + userMapper.getById(user.getId()));

        // 更新测试
        // user = new User();
        // user.setId(1L);
        // user.setAccount("conanli");
        // System.out.println("update: " + userMapper.updateById(user));
        // System.out.println("user:" + userMapper.getById(1L));

        // 获取测试
        // System.out.println("user:" + userMapper.getById(1L));

        // 删除测试
        // System.out.println("delete:" + userMapper.deleteById(1L));

        // 是否存在测试
        // System.out.println("exist:" + userMapper.existById(1L));

        session.commit();
        session.close();
    }

}
