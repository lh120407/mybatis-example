# Mybatis - 自定义BaseMapper 2

## SQL Provider

- @InsertProvider
- @UpdateProvider
- @DeleteProvider
- @SelectProvider

这些可选的SQL注解允许你指定一个类名和一个方法，在执行时来返回动态的SQL。

```java
@SelectProvider(type = UserSqlBuilder.class, method = "buildGetUsersByName")
List<User> getUsersByName(String name);

class UserSqlBuilder {
    public String buildGetUsersByName(final String name) {
        return new SQL(){{
            SELECT("*");
            FROM("users");
            if (name != null) {
                WHERE("name like #{value} || '%'");
            }
            ORDER_BY("id");
        }}.toString();
    }
}
```

## BaseMapper

我们可以使用 `SQLProvider` 构建属于自己的BaseMapper。

```java
public interface BaseMapper<T, K> {
    @InsertProvider(type = SqlSourceBuilder.class, method = "build")
    public Long insert(T model);

    @UpdateProvider(type = SqlSourceBuilder.class, method = "build")
    public Long updateById(T model);

    @DeleteProvider(type = SqlSourceBuilder.class, method = "build")
    public Long deleteById(@Param("id") K id);

    @SelectProvider(type = SqlSourceBuilder.class, method = "build")
    public T getById(@Param("id") K id);

    @SelectProvider(type = SqlSourceBuilder.class, method = "build")
    public Boolean existById(@Param("id") K id);
}
```

#### SqlSourceBuilder

把用SQLProvider生成的ProviderSqlSource替换成DynamicSqlSource

```java
public class SqlSourceBuilder {
    public static String build(Configuration configuration) {
        for (MappedStatement mappedStatement : configuration.getMappedStatements()) {
            if (mappedStatement.getSqlSource() instanceof ProviderSqlSource) {
                Class<?> providerClass = getProviderClass(mappedStatement);
                if (providerClass != SqlSourceBuilder.class)
                    continue;

                Class<?> mapperClass = getMapperClass(mappedStatement);
                Class<?>[] generics = getMapperGenerics(mapperClass);
                Class<?> modelClass = generics[0];
                Class<?> primaryFieldClass = generics[1];
                ResultMap resultMap = getResultMap(mappedStatement, modelClass);

                String sqlScript = getSqlScript(mappedStatement, mapperClass, modelClass, primaryFieldClass, resultMap);
                SqlSource sqlSource = createSqlSource(mappedStatement, sqlScript);
                setSqlSource(mappedStatement, sqlSource);
            }
        }
        return "sql";
    }
    ...
}
```

#### MapperTest 测试

```java
public class MapperTest {
    @Test
    public void test() throws Exception {
        SqlSessionFactory sessionFactory = MybatisConfig.getSessionFactory();
        SqlSession session = sessionFactory.openSession();

        UserMapper userMapper = session.getMapper(UserMapper.class);

        User user = null;

        // 新增测试
        System.out.println("------------ 新增测试 ------------");
        user = new User();
        user.setId(1L);
        user.setAccount("conanli");
        user.setPassword("123456");
        System.out.println("insert: " + userMapper.insert(user));

        // 更新测试
        System.out.println("------------ 更新测试 ------------");
        user = new User();
        user.setId(1L);
        user.setPassword("111111");
        System.out.println("update: " + userMapper.updateById(user));

        // 获取测试
        System.out.println("------------ 获取测试 ------------");
        System.out.println("user: " + userMapper.getById(1L));

        // 删除测试
        System.out.println("------------ 删除测试 ------------");
        System.out.println("delete: " + userMapper.deleteById(1L));

        // 存在测试
        System.out.println("------------ 存在测试 ------------");
        System.out.println("exist: " + userMapper.existById(1L));

        session.commit();
        session.close();
    }
}
```

*PS：本文使用的是mybatis-3.4.4*