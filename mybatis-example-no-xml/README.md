# Mybatis - 纯注解、零XML

#### 第一步：创建MybatisConfig类，用于获取SqlSessionFactory工厂类

```java
public class MybatisConfig {
    private static SqlSessionFactory sessionFactory;

    public static SqlSessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            PooledDataSource dataSource = getDataSource();
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);
            Configuration configuration = new Configuration(environment);
            configuration.addMapper(UserMapper.class);
            sessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        }
        return sessionFactory;
    }

    private static PooledDataSource getDataSource() {
        PooledDataSource dataSource = dataSource = new PooledDataSource();
        dataSource.setDriver("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
        return dataSource;
    }
}
```

#### 第二步：创建UserMapper接口，供Service调用

```java
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    public User getById(@Param("id") Long id);
}
```

#### 第三步：测试

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