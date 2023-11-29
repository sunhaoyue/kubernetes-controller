package com.kedacom.ctsp.iomp.k8s.common.util;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kedacom.ctsp.lang.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author gutianwei
 */
@Component
@Slf4j
public class SqliteUtil {

    //public static final String FILE_PATH = "E:\\data\\iomp_data\\sqlite\\";
    public static final String FILE_PATH = "/data/iomp_data/sqlite/";
    private static final String CLASS_NAME = "org.sqlite.JDBC";
    public static final String DB_URL_PREFIX = "jdbc:sqlite:" + FILE_PATH;

    public static final String PRE_DB = ".db";

    /**
     * 获取数据库连接
     *
     * @param dbname 数据库名称
     * @return conn
     * @throws ClassNotFoundException
     */
    public static Connection getConnection(String dbname) throws SQLException, ClassNotFoundException {
        if (StringUtils.isBlank(dbname)) {
            return null;
        }
        Class.forName(CLASS_NAME);
        if (!dbname.endsWith(PRE_DB)) {
            dbname = dbname + PRE_DB;
        }
        return DriverManager.getConnection(DB_URL_PREFIX + dbname);
    }


    public static boolean checkDb(String dbname) {
        if (StringUtils.isBlank(dbname)) {
            return false;
        }
        if (!dbname.endsWith(PRE_DB)) {
            dbname = dbname + PRE_DB;
        }

        File file = new File(FILE_PATH + dbname);
        if (!file.exists()) {
            log.info("iomp-log文件不存在");

            return false;
        }
        return true;
    }


    /**
     * 获取一个数据连接声明
     *
     * @param conn 数据库连接
     * @return statement
     * @throws SQLException
     */
    private static Statement getStatement(Connection conn) throws SQLException {
        if (null == conn) {
            return null;
        }
        return conn.createStatement();
    }

    /**
     * 根据数据库名称获取数据库连接声明
     *
     * @param dbname 数据库名称
     * @return statement
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Statement getStatementByDbName(String dbname) throws SQLException, ClassNotFoundException {
        if (StringUtils.isBlank(dbname)) {
            return null;
        }
        return getStatement(getConnection(dbname));
    }

    /**
     * 创建sqlite数据库
     *
     * @param dbname 数据库名称
     * @return 0：失败；1：成功
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static int createDatabase(String dbname) throws ClassNotFoundException, SQLException {
        Statement statement = getStatementByDbName(dbname);
        if (null != statement) {
            return 1;
        }
        return 0;
    }

    /**
     * 关闭声明
     *
     * @param statement
     * @throws SQLException
     */
    public static void closeStatement(Statement statement) throws SQLException {
        if (null != statement && !statement.isClosed()) {
            Connection conn = statement.getConnection();
            statement.close();
            closeConnection(conn);
        }
    }

    /**
     * 关闭声明
     *
     * @param conn
     * @throws SQLException
     */
    public static void closeConnection(Connection conn) throws SQLException {
        if (null != conn && !conn.isClosed()) {
            conn.close();
        }
    }

    /**
     * 创建数据库表
     *
     * @param dbname 数据库名称
     * @param sql    创建语句
     * @return 0：创建失败；1：创建成功
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public int createTables(String dbname, String sql) throws ClassNotFoundException, SQLException {
        if (StringUtils.isBlank(sql)) {
            return 0;
        }
        Statement statement = getStatementByDbName(dbname);
        if (null != statement) {
            try {

                statement.executeUpdate(sql);

            } catch (Exception e) {

                log.error(e.getMessage());

            } finally {

                closeStatement(statement);

            }

            return 1;
        }
        return 0;
    }

    /**
     * 插入数据
     *
     * @param dbname 数据库名称
     * @param sql    insert语句
     * @return 0：插入失败；1：插入成功
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public int insert(String dbname, String sql) throws ClassNotFoundException, SQLException {
        return saveOrUpdate(dbname, sql);
    }

    /**
     * 修改数据
     *
     * @param dbname 数据库名称
     * @param sql    update语句
     * @return 0：插入失败；1：插入成功
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public int update(String dbname, String sql) throws ClassNotFoundException, SQLException {
        return saveOrUpdate(dbname, sql);
    }

    private int saveOrUpdate(String dbname, String sql) throws SQLException, ClassNotFoundException {
        if (StringUtils.isBlank(sql)) {
            return 0;
        }
        Statement statement = getStatementByDbName(dbname);
        if (null != statement) {
            statement.executeUpdate(sql);
            closeStatement(statement);
            return 1;
        }
        return 0;
    }

    /**
     * 批量插入数据
     *
     * @param dbname 数据库名称
     * @param sqls   insert语句
     * @return 0：插入失败；1：插入成功
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public int insertBatch(String dbname, List<String> sqls) throws ClassNotFoundException, SQLException {
        if (null == sqls || sqls.isEmpty()) {
            return 0;
        }
        Connection conn = getConnection(dbname);

        if (null == conn) {
            return 0;
        }

        conn.setAutoCommit(false);
        Statement statement = getStatement(conn);

        if (ObjectUtil.isNull(statement)) {
            closeConnection(conn);
            return 0;
        }

        for (String sql : sqls) {
            if (StringUtils.isNotBlank(sql)) {
                statement.executeUpdate(sql);
            }
        }
        closeStatement(statement);
        conn.commit();
        closeConnection(conn);
        return 1;
    }

    /**
     * 更新和删除
     *
     * @param dbname     数据库名称
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static int update(String dbname, String sql, Object[] parameters)
            throws ClassNotFoundException, SQLException {
        return execute(dbname, sql, parameters, 0);
    }

    /**
     * 添加
     *
     * @param dbname     数据库名称
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static int insert(String dbname, String sql, Object[] parameters)
            throws ClassNotFoundException, SQLException {
        return execute(dbname, sql, parameters, 1);
    }


    /**
     * 统计
     *
     * @param dbname 数据库名称
     * @param sql
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static int executeCount(String dbname, String sql)
            throws SQLException, ClassNotFoundException {
        Connection conn = getConnection(dbname);
        PreparedStatement ps = null;
        int count = 0;
        if (conn != null) {
            try {

                ps = conn.prepareStatement(sql);
                if (ObjectUtil.isNull(ps)) {
                    closeConnection(conn);
                    return count;
                }
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    count = resultSet.getInt(1);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                closeStatement(ps);
            }
        }
        return count;
    }

    /**
     * 查询
     *
     * @param dbname 数据库名称
     * @param sql
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static <T> List<T> executeQuery(String dbname, String sql, Class<T> clazz)
            throws SQLException, ClassNotFoundException {
        List<T> list = Lists.newArrayList();
        Connection conn = getConnection(dbname);
        if (null == conn) {
            return list;
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        ps = conn.prepareStatement(sql);
        try {
            // 执行查询方法
            rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            List<String> columnList = Lists.newArrayList();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                columnList.add(rsmd.getColumnName(i + 1));
            }
            // 循环遍历记录
            while (rs.next()) {
                addObject(clazz, list, rs, columnList);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            closeStatement(ps);
        }
        return list;
    }

    private static <T> void addObject(Class<T> clazz, List<T> list, ResultSet rs, List<String> columnList) throws InstantiationException, IllegalAccessException, SQLException, InvocationTargetException {
        // 创建封装记录的对象
        T obj = clazz.newInstance();
        // 遍历一个记录中的所有列
        for (int i = 0; i < columnList.size(); i++) {
            // 获取列名
            String column = columnList.get(i);
            // 根据列名创建set方法
            String setMethd = "set" + column.substring(0, 1).toUpperCase() + column.substring(1);
            setMethd = setMethd.replace("_", "");
            // 获取clazz中所有方法对应的Method对象
            Method[] ms = clazz.getMethods();
            // 循环遍历ms
            for (int j = 0; j < ms.length; j++) {
                // 获取每一个method对象
                Method m = ms[j];
                // 判断m中对应的方法名和数据库中列名创建的set方法名是否形同
                if (m.getName().equalsIgnoreCase(setMethd)) {
                    // 反调set方法封装数据
                    Object value = rs.getObject(column);
                    if (value == null) {
                        break;
                    }
                    if (value instanceof Integer) {
                        m.invoke(obj, ((Integer) value).longValue());
                        break;
                    }
                    // 获取rs中对应的值，封装到obj中
                    m.invoke(obj, rs.getObject(column));
                    break;
                }
            }
        }
        list.add(obj);
    }

    /**
     * 执行增删改
     *
     * @param dbname     数据库名称
     * @param sql
     * @param parameters
     * @param type       0为删改，1为增加
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private static int execute(String dbname, String sql, Object[] parameters, int type)
            throws SQLException, ClassNotFoundException {
        Connection conn = getConnection(dbname);
        PreparedStatement ps = null;
        int count = 0;
        if (conn != null) {
            try {

                ps = conn.prepareStatement(sql);
                if (ObjectUtil.isNull(ps)) {
                    closeConnection(conn);
                    return count;
                }
                for (int i = type + 1; i <= parameters.length + type; i++) {
                    ps.setObject(i, parameters[i - (1 + type)]);
                }
                count = ps.executeUpdate();
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                closeStatement(ps);
            }
        }
        return count;
    }

    /**
     * 执行查询，并将值反射到bean
     *
     * @param sql
     * @param parameters
     * @param clazz
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static <T> List<T> select(String dbname, String sql, Object[] parameters, Class<T> clazz)
            throws ClassNotFoundException, SQLException {
        List<T> list = Lists.newArrayList();
        Connection conn = getConnection(dbname);
        if (null == conn) {
            return list;
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        ps = conn.prepareStatement(sql);
        if (ObjectUtil.isNotNull(parameters) && ObjectUtil.isNotNull(ps)) {

            try {
                for (int i = 1; i <= parameters.length; i++) {
                    ps.setObject(i, parameters[i - 1]);
                }
                // 执行查询方法
                rs = ps.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                List<String> columnList = Lists.newArrayList();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    columnList.add(rsmd.getColumnName(i + 1));
                }
                // 循环遍历记录
                while (rs.next()) {
                    addObject1(clazz, list, rs, columnList);
                }

            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                closeStatement(ps);
            }
        } else {
            closeConnection(conn);
        }
        return list;
    }


    private static <T> void addObject1(Class<T> clazz, List<T> list, ResultSet rs, List<String> columnList) throws InstantiationException, IllegalAccessException, SQLException, InvocationTargetException {
        // 创建封装记录的对象
        T obj = clazz.newInstance();
        // 遍历一个记录中的所有列
        for (int i = 0; i < columnList.size(); i++) {
            // 获取列名
            String column = columnList.get(i);
            // 根据列名创建set方法
            String setMethd = "set" + column.substring(0, 1).toUpperCase() + column.substring(1);
            // 获取clazz中所有方法对应的Method对象
            Method[] ms = clazz.getMethods();
            // 循环遍历ms
            for (int j = 0; j < ms.length; j++) {
                // 获取每一个method对象
                Method m = ms[j];
                // 判断m中对应的方法名和数据库中列名创建的set方法名是否形同
                if (m.getName().equals(setMethd)) {
                    // 反调set方法封装数据
                    // 获取rs中对应的值，封装到obj中
                    m.invoke(obj, rs.getObject(column));
                    break; // 提高效率
                }
            }
        }
        list.add(obj);
    }

    /**
     * 执行查询，并将值反射到map
     *
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static List<Map<String, Object>> select(String dbname, String sql, Object[] parameters)
            throws ClassNotFoundException, SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = getConnection(dbname);
        if (null == conn) {
            return list;
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        ps = conn.prepareStatement(sql);

        try {
            if (parameters != null) {
                for (int i = 1; i <= parameters.length; i++) {
                    ps.setObject(i, parameters[i - 1]);
                }
            }
            // 执行查询方法
            rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            List<String> columnList = Lists.newArrayList();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                columnList.add(rsmd.getColumnName(i + 1));
            }
            // 循环遍历记录
            while (rs.next()) {
                addObject2(list, rs, columnList);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            closeStatement(ps);
        }
        return list;
    }

    private static void addObject2(List<Map<String, Object>> list, ResultSet rs, List<String> columnList) throws SQLException {
        Map<String, Object> obj = Maps.newHashMap();
        // 遍历一个记录中的所有列
        for (int i = 0; i < columnList.size(); i++) {
            // 获取列名
            String column = columnList.get(i);
            obj.put(column, rs.getObject(column));
        }
        list.add(obj);
    }

    public static <T> void insertEntity(String dbName, String table, Class<T> c, Object obj) {
        if (obj == null || !c.isAssignableFrom(obj.getClass())) {
            return;
        }
        Field[] fields = obj.getClass().getDeclaredFields();
        int fieldSize = fields.length;
        // person
        String tableName = table;
        String[] types1 = {"int", "java.lang.String", "boolean", "char",
                "float", "double", "long", "short", "byte"};
        String[] types2 = {"Integer", "java.lang.String", "java.lang.Boolean",
                "java.lang.Character", "java.lang.Float", "java.lang.Double",
                "java.lang.Long", "java.lang.Short", "java.lang.Byte"};

        StringBuilder sql = new StringBuilder();
        sql.append("insert into ");
        sql.append(tableName);
        sql.append(" values(");
        for (int i = 0; i < fieldSize; i++) {
            sql.append("?,");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        //log.info(sql.toString());
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = getConnection(dbName);
            if (ObjectUtil.isNotNull(conn)) {
                ps = conn.prepareStatement(sql.toString());
                for (int j = 0; j < fieldSize; j++) {
                    ReflectionUtils.makeAccessible(fields[j]);
                    for (int i = 0; i < types1.length; i++) {
                        setPreparedStatement(obj, fields, types1, types2, ps, j, i);
                    }
                }
                ps.execute();
                closeConnection(conn);
            } else {
                throw new CommonException("数据库链接错误");
            }
        } catch (Exception e1) {
            log.error(String.format("error save accessLog[%s]", e1));
            e1.printStackTrace();
        }

    }

    private static void setPreparedStatement(Object obj, Field[] fields, String[] types1, String[] types2, PreparedStatement ps, int j, int i) throws IllegalAccessException, SQLException {
        if (fields[j].getType().getName()
                .equalsIgnoreCase(types1[i])
                || fields[j].getType().getName()
                .equalsIgnoreCase(types2[i])) {

            if (fields[j].get(obj) != null
                    && !"".equals(fields[j].get(obj))
                    && !"null".equals(fields[j].get(obj))) {
                ps.setObject(j + 1, fields[j].get(obj));
            } else {
                ps.setObject(j + 1, null);
            }
        }
    }

}

