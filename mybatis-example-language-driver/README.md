# Mybatis - 自定义BaseMapper

## LanguageDriver

MyBatis 从 3.2 开始支持可插拔的脚本语言，因此你可以在插入一种语言的驱动（language driver）之后来写基于这种语言的动态 SQL 查询。

可以通过实现下面接口的方式来插入一种语言：

```java
public interface LanguageDriver {
    ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);
    SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);
    SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);
}
```

一旦有了自定义的语言驱动，你就可以在 mybatis-config.xml 文件中将它设置为默认语言：

```xml
<typeAliases>
    <typeAlias type="org.sample.MyLanguageDriver" alias="myLanguage"/>
</typeAliases>
<settings>
    <setting name="defaultScriptingLanguage" value="myLanguage"/>
</settings>
```

除了设置默认语言，你也可以针对特殊的语句指定特定语言，这可以通过如下的 `lang` 属性来完成：

```xml
<select id="selectBlog" lang="myLanguage">
    SELECT * FROM BLOG
</select>
```

或者在你正在使用的映射中加上注解 `@Lang` 来完成：

```java
public interface Mapper {
    @Lang(MyLanguageDriver.class)
    @Select("SELECT * FROM BLOG")
    List<Blog> selectBlog();
}
```

## BaseMapper

我们可以使用 `LanguageDriver` 构建属于自己的BaseMapper。

```java
public interface BaseMapper<T, K> {
    @Lang(BaseMapperDriver.class)
    @Insert({"<script>", "INSERT INTO ${table} ${values}", "</script>"})
    public Long insert(T model);

    @Lang(BaseMapperDriver.class)
    @Update({"<script>", "UPDATE ${table} ${sets} WHERE ${id}=#{id}", "</script>"})
    public Long updateById(T model);

    @Lang(BaseMapperDriver.class)
    @Delete("DELETE FROM ${table} WHERE ${id}=#{id}")
    public Long deleteById(@Param("id") K id);

    @Lang(BaseMapperDriver.class)
    @Select("SELECT * FROM ${table} WHERE ${id}=#{id}")
    public T getById(@Param("id") K id);

    @Lang(BaseMapperDriver.class)
    @Select("SELECT COUNT(1) FROM ${table} WHERE ${id}=#{id}")
    public Boolean existById(@Param("id") K id);
}
```

但是由于 `createSqlSource` 在构建的过程中，并没办法知道当前正在解析的Mapper，因此我们得对源码做一些小小的改动。

#### 重写 `org.apache.ibatis.binding.MapperRegistry`

在 `addMapper` 方法中，把当前正在解析的Mapper放在一下全局变量中。 

```java
public class MybatisMapperRegistry extends MapperRegistry {
    ...
    public <T> void addMapper(Class<T> type) {
            ...
                MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
                currentMapper.set(type);
                parser.parse();
                loadCompleted = true;
                currentMapper.set(null);
            ...
        }
    }
}
```

#### 重写 `org.apache.ibatis.session.Configuration`

使用重写后的 `MybatisMapperRegistry`

```java
public class MybatisConfiguration extends Configuration {
    protected final MapperRegistry mapperRegistry = new MybatisMapperRegistry(this);
    ...
}
```

## BaseMapperDriver

获取当前正在解析的Mapper，并通过反射知悉是那个实体的操作，从而替换关键的字符，例如表名，主键等。

```java
public class BaseMapperDriver extends XMLLanguageDriver {
    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        Class<?> mapperClass = MybatisMapperRegistry.getCurrentMapper();

        Class<?>[] generics = MybatisReflectUtil.getMapperGenerics(mapperClass);
        Class<?> modelClass = generics[0];
        Class<?> idClass = generics[1];

        ResultMap resultMap = getResultMap(configuration.getResultMaps(), modelClass);
        script = setTable(script, mapperClass, modelClass, idClass, resultMap);
        script = setId(script, mapperClass, modelClass, idClass, resultMap);
        script = setValues(script, mapperClass, modelClass, idClass, resultMap);
        script = setSets(script, mapperClass, modelClass, idClass, resultMap);

        return super.createSqlSource(configuration, script, parameterType);
    }
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