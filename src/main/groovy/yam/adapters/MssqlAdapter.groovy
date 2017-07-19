package yam.adapters

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.regex.Pattern

class MssqlAdapter implements DbmsAdapter {

    static Pattern uriMatcher() {
        return ~'^jdbc:sqlserver://(.*)'
    }

    def setup() {
        Connection connection = this.connectionResource
        Statement stmt = connection.createStatement()

        String createSql = """
        IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[changeSet]') AND type in (N'U'))
        BEGIN
        CREATE TABLE changeSet(
        [id] [int] NOT NULL IDENTITY(1,1),
        [resource] [nvarchar](255) NOT NULL,
        [version] [nvarchar](32) NOT NULL,
        [changeLogClass] [nvarchar](175) NOT NULL,
        [changeSetMethod] [nvarchar](80) NOT NULL,
        [author] [nvarchar](150) NOT NULL,
        [runGroup] [int] NOT NULL,
        [date] [datetime] NOT NULL,
        [action] [nvarchar](50) NOT NULL);
        
        CREATE NONCLUSTERED INDEX NDX_CHANGESET ON changeSet(resource, version, action); 
        END
        """
        stmt.executeUpdate(createSql)
    }

    def createConnection() {
        return DriverManager.getConnection(this.connectionURI)
    }

    Long getMaxRunGroup() {
        Connection connection = this.connectionResource
        String sql = "SELECT MAX(runGroup) as maxRunGroup FROM changeSet"
        PreparedStatement stmt = connection.prepareStatement(sql)
        ResultSet rs = stmt.executeQuery()
        rs.next()
        return rs.getLong(1)
    }

    def closeConnection() {
        Connection connection = this.connectionResource
        connection?.close()
    }

    boolean changeSetExists(String resource, String version, String action) {
        Connection connection = this.connectionResource
        String sql = "SELECT * FROM changeSet WHERE resource=? AND version=? AND action=?"
        PreparedStatement stmt = connection.prepareStatement(sql)
        stmt.setString(1, resource)
        stmt.setString(2, version)
        stmt.setString(3, action)

        ResultSet rs = stmt.executeQuery()
        return rs.next()
    }

    List list(Map filterCriteria) {
        List data = []
        Connection connection = this.connectionResource
        Map tableColumns = [
                id             : { ResultSet rs -> rs.getInt(1) },
                resource       : { ResultSet rs -> rs.getString(2) },
                version        : { ResultSet rs -> rs.getString(3) },
                changeLogClass : { ResultSet rs -> rs.getString(4) },
                changeSetMethod: { ResultSet rs -> rs.getString(5) },
                author         : { ResultSet rs -> rs.getString(6) },
                runGroup       : { ResultSet rs -> rs.getLong(7) },
                date           : { ResultSet rs -> rs.getTimestamp(8) },
                action         : { ResultSet rs -> rs.getString(9) },
        ]
        Map filterKeys = [
                "id"             : { PreparedStatement stmt, def value -> stmt.setInt(value) },
                "resource"       : { PreparedStatement stmt, def value -> stmt.setString(value) },
                "version"        : { PreparedStatement stmt, def value -> stmt.setString(value) },
                "changeLogClass" : { PreparedStatement stmt, def value -> stmt.setString(value) },
                "changeSetMethod": { PreparedStatement stmt, def value -> stmt.setString(value) },
                "author"         : { PreparedStatement stmt, def value -> stmt.setString(value) },
                "runGroup"       : { PreparedStatement stmt, def value -> stmt.setInt(value) },
                "action"         : { PreparedStatement stmt, def value -> stmt.setString(value) }
        ]

        String sql = "SELECT ${tableColumns.keySet().join(", ")} FROM changeSet WHERE ${filterCriteria.keySet().collect { "${it}=?" }.join(" AND ")}"
        PreparedStatement preparedStatement = connection.prepareStatement(sql)

        filterCriteria.each { key, value ->
            if (filterKeys.keySet().contains(key)) {
                filterKeys[key](preparedStatement, value)
            }
        }
        ResultSet rs = preparedStatement.executeQuery()
        while (rs.next()) {
            Map row = [:]
            tableColumns.each { key, closure ->
                row[key] = closure(rs)
            }
            data << row
        }
        return data
    }

    Map insertChangeSet(Map changeSet) {
        Connection connection = this.connectionResource
        Calendar cal = Calendar.getInstance()

        String sql = "INSERT INTO changeSet(resource, version, changeLogClass, changeSetMethod, runGroup,author,date, action) VALUES (?,?,?,?,?,?,?,?)"
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        stmt.setString(1, changeSet.resource)
        stmt.setString(2, changeSet.version)
        stmt.setString(3, changeSet.changeLogClass)
        stmt.setString(4, changeSet.changeSetMethod)
        stmt.setLong(5, changeSet.runGroup)
        stmt.setString(6, changeSet.author)
        stmt.setTimestamp(7, new java.sql.Timestamp(cal.getTimeInMillis()))
        stmt.setString(8, changeSet.action)
        stmt.executeUpdate()

        try {
            ResultSet generatedKeys = stmt.getGeneratedKeys()
            if (generatedKeys.next()) {
                changeSet.id = generatedKeys.getInt(1)
            }
        } finally {
            return changeSet
        }
    }
}
