# Mybatis 持久层框架

## 简介

MyBatis是支持定制化SQL、存储过程以及高级映射的优秀的持久层框架。

- 避免了几乎所有的JDBC代码和手动设置参数以及获取结果集。
- 可以对配置和原生Map使用简单的XML或注解，将接口和Java的POJOs映射成数据库中的记录。

## 使用

#### 第一步：创建mybatis-config.xml文件，配置数据源，包括链接，用户名，密码

```xml
<configuration>
    <properties resource="config.properties" />

    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${database.driver}"/>
                <property name="url" value="${database.url}"/>
                <property name="username" value="${database.username}"/>
                <property name="password" value="${database.password}"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

#### 第二步：创建MybatisConfig类，用于获取SqlSessionFactory工厂类

```java
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
```

#### 第三步：创建UserMapper接口，供Service调用

```java
public interface UserMapper {
    public User getById(@Param("id") Long id);
}
```

#### 第五步：创建UserMapper映射文件，用于访问数据库

```xml
<mapper namespace="com.example.mapper.UserMapper">
    <resultMap id="user" type="com.example.model.User">
        <id property="id" column="id" jdbcType="BIGINT"/>
        <result property="account" column="account" jdbcType="VARCHAR"/>
        <result property="password" column="password" jdbcType="VARCHAR"/>
    </resultMap>

    <select id="getById" resultType="com.example.model.User">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

#### 第五步：mybatis-config.xml中添加UserMapper.xml映射

```xml
<configuration>

    ...

    <mappers>
        <mapper resource="com/example/mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

#### 第六步：测试

```java
public class MapperTest {
    @Test
    public void test() throws Exception {
        SqlSessionFactory sessionFactory = MybatisConfig.getSessionFactory();
        SqlSession session = sessionFactory.openSession();

        UserMapper userMapper = session.getMapper(UserMapper.class);
        User user = userMapper.getById(1L);
        System.out.println(user);

        session.close();
    }
}
```

*PS：本文使用的是mybatis-3.4.4*