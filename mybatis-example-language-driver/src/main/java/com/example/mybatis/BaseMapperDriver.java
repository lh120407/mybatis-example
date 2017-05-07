package com.example.mybatis;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

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

	private ResultMap getResultMap(Collection<ResultMap> resultMaps, Class<?> modelClass) {
		for (ResultMap resultMap : resultMaps)
			if (modelClass == resultMap.getType() && !resultMap.getId().contains("-"))
				return resultMap;
		return null;
	}

	/**
	 * 设置表名
	 * 把 ResultMap 的 id 值作为表名，
	 * 若 ResultMap 不存在，则把驼峰命名法的 Model 类名转以下划线命名作为表名
	 * 
	 * @param script
	 * @param modelClass
	 * @param resultMap
	 * @return
	 */
	private String setTable(String script, Class<?> mapperClass, Class<?> modelClass, Class<?> idClass, ResultMap resultMap) {
		if (script.contains("${table}")) {
			String tableName = null;

			if (resultMap != null) {
				tableName = resultMap.getId().substring(mapperClass.getName().length() + 1);
			}

			if (tableName == null) {
				tableName = toUnderline(modelClass.getSimpleName());
			}

			script = script.replace("${table}", tableName);
		}
		return script;
	}

	private String setId(String script, Class<?> mapperClass, Class<?> modelClass, Class<?> idClass, ResultMap resultMap) {
		if (script.contains("${id}")) {
			String idName = null;

			ResultMapping resultMapping = getIdResultMapping(resultMap, null);
			if (resultMapping != null) {
				idName = resultMapping.getColumn();
			}

			if (idName == null) {
				idName = "id";
			}

			script = script.replace("${id}", idName);
		}
		return script;
	}

	private String setValues(String script, Class<?> mapperClass, Class<?> modelClass, Class<?> idClass, ResultMap resultMap) {
		if (script.contains("${values}")) {
			StringBuilder buf = new StringBuilder();

			Field[] fields = MybatisReflectUtil.getModelField(modelClass);

			buf.append("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
			for (Field field : fields) {
				buf.append(String.format("<if test=\"%s\">", getEmptyTesting(field)));

				ResultMapping resultMapping = getResultMapping(resultMap, field);
				buf.append(String.format("%s,", getColumnName(resultMapping, field)));

				buf.append("</if>");
			}
			buf.append("</trim>");

			buf.append("<trim prefix=\"VALUES (\" suffix=\")\" suffixOverrides=\",\">");
			for (Field field : fields) {
				buf.append(String.format("<if test=\"%s\">", getEmptyTesting(field)));

				ResultMapping resultMapping = getResultMapping(resultMap, field);
				buf.append(String.format("%s,", getColumnValue(resultMapping, field)));

				buf.append("</if>");
			}
			buf.append("</trim>");

			script = script.replace("${values}", buf.toString());
		}
		return script;
	}

	private String setSets(String script, Class<?> mapperClass, Class<?> modelClass, Class<?> idClass, ResultMap resultMap) {
		if (script.contains("${sets}")) {
			StringBuilder buf = new StringBuilder();

			buf.append("<set>");
			Field[] fields = MybatisReflectUtil.getModelField(modelClass);
			for (Field field : fields) {
				buf.append(String.format("<if test=\"%s\">", getEmptyTesting(field)));

				ResultMapping resultMapping = getResultMapping(resultMap, field);
				buf.append(String.format("%s = %s,", getColumnName(resultMapping, field), getColumnValue(resultMapping, field)));

				buf.append("</if>");
			}
			buf.append("</set>");

			script = script.replace("${sets}", buf.toString());
		}
		return script;
	}

	private ResultMapping getIdResultMapping(ResultMap resultMap, Field field) {
		if (resultMap != null) {
			if (resultMap.getIdResultMappings().size() > 0) {
				return resultMap.getIdResultMappings().get(0);
			}
		}
		return null;
	}

	private ResultMapping getResultMapping(ResultMap resultMap, Field field) {
		if (resultMap != null) {
			for (ResultMapping resultMapping : resultMap.getPropertyResultMappings()) {
				if (resultMapping.getProperty().equals(field.getName()))
					return resultMapping;
			}
		}
		return null;
	}

	private String getEmptyTesting(Field field) {
		if (String.class == field.getType()) {
			return String.format("%s != null and %s != ''", field.getName(), field.getName());
		} else {
			return String.format("%s != null", field.getName());
		}
	}

	private String getColumnName(ResultMapping resultMapping, Field field) {
		if (resultMapping == null)
			return field.getName();
		return resultMapping.getColumn();
	}

	private String getColumnValue(ResultMapping resultMapping, Field field) {
		if (resultMapping == null || resultMapping.getJdbcType() == null)
			return String.format("#{%s}", field.getName());
		return String.format("#{%s,jdbcType=%s}", field.getName(), resultMapping.getJdbcType());
	}

	private String toUnderline(String str) {
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
