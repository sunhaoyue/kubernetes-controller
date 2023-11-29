package com.kedacom.ctsp.iomp.k8s.common.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.List;

/**
 * @author sunhaoyue
 * @date Created in 2023/11/9 14:05
 */
@Component
@Slf4j
public class MySQLUtil {

    public static final String DB_URL = "jdbc:mysql://%s:%s/iomp?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC";
    public static final String DB_USERNAME = "iomp_write";
    public static final String DB_PASSWORD = "83Ac0d76b737a8d2@";

    /**
     * 获取数据库连接
     *
     * @return conn
     * @throws ClassNotFoundException
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    public static Connection getConnection(String url, String port) throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(String.format(DB_URL, url, port), DB_USERNAME, DB_PASSWORD);
    }

    /**
     * 查询
     *
     * @param url 数据库名称
     * @param sql
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static <T> List<T> executeQuery(String url, String port, String sql, Class<T> clazz)
            throws SQLException, ClassNotFoundException {
        List<T> list = Lists.newArrayList();
        Connection conn = getConnection(url, port);
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

    /**
     * 关闭数据里连接
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

}
