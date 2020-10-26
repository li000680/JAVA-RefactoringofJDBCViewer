package jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class JDBCModel {
	private List<String> columnNames;
	private List<String> tableNames;
	private Connection connection;
	private String user;
	private String pass;

	public JDBCModel() {
		this.columnNames = new LinkedList<>();
		this.tableNames = new LinkedList<>();
	}

	public void setCredentials(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	private void checkConnectionIsValid() throws SQLException {
		if (connection == null || connection.isClosed())
			throw new IllegalStateException();
	}

	private void checkTableNameAndColumnsAreValid(String table) throws SQLException {
		table = Objects.requireNonNull(table).trim();
		if (tableNames.isEmpty())
			getAndInitializeTableNames();
		if (columnNames.isEmpty())
			getAndInitializeColumnNames(table);
		if (table.isEmpty() || !tableNames.contains(table))
			throw new IllegalArgumentException("table name=\"" + table + "\" is not valid");
	}

	public void connectTo(String url) throws SQLException {
		if (isConnected())
			close();
		connection = DriverManager.getConnection(url, user, pass);
	}

	public boolean isConnected() throws SQLException {
		return connection != null && connection.isValid(1);
	}

	public List<String> getAndInitializeColumnNames(String table) throws SQLException {
		checkConnectionIsValid();
		columnNames.clear();
		DatabaseMetaData dbMeta = connection.getMetaData();
		ResultSet rs = dbMeta.getColumns(connection.getCatalog(), null, table, null);
		while (rs.next()) {
			columnNames.add(rs.getString("COLUMN_NAME"));
		}
		List<String> list = Collections.unmodifiableList(columnNames);
		return list;
	}

	public List<String> getAndInitializeTableNames() throws SQLException {
		checkConnectionIsValid();
		tableNames.clear();
		DatabaseMetaData dbMeta = connection.getMetaData();
		ResultSet rs = dbMeta.getTables(connection.getCatalog(), null, null, new String[] { "TABLE" });
		while (rs.next()) {
			tableNames.add(rs.getString("TABLE_NAME"));
		}
		List<String> list = Collections.unmodifiableList(tableNames);
		return list;
	}

	public List<List<Object>> getAll(String table) throws SQLException {
		return search(table, null);
	}

	public List<List<Object>> search(String table, String searchTerm) throws SQLException {
		checkConnectionIsValid();
		checkTableNameAndColumnsAreValid(table);
		
		searchTerm = searchTerm.trim();

		if (connection == null || connection.isClosed())
			return null;
		
		String query = buildSQLSearchQuery(table, searchTerm != null && !searchTerm.isEmpty());
		List<List<Object>> list = new LinkedList<List<Object>>();

		try (PreparedStatement ps = connection.prepareStatement(query)) {
			if (searchTerm != null && !searchTerm.isEmpty()) {
				searchTerm = String.format("%%%s%%", searchTerm);
				for (int i = 1; i <= columnNames.size(); i++)
					ps.setObject(i, searchTerm);
			}
			extractRowsFromResultSet(ps, list);
		}
		return list;
	}

	private String buildSQLSearchQuery(String table, boolean withParameters) throws SQLException {
		StringBuilder builder = new StringBuilder("SELECT * FROM ");
		builder.append(table);
		if (withParameters == false)
			return builder.toString();

		builder.append(" where ");
		int i = 1;
		for (String columnName : columnNames) {
			builder.append(columnName);
			builder.append(" like ? ");
			if (i != columnNames.size())
				builder.append(" or ");
			i++;
		}
		return builder.toString();
	}

	private void extractRowsFromResultSet(PreparedStatement ps, List<List<Object>> list) throws SQLException {
		try (ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				List<Object> row = new ArrayList<>();
				for (int i = 1; i <= columnNames.size(); i++) {
					row.add(rs.getObject(i));
				}
				list.add(row);
			}
		}
	}

	public void close() throws SQLException {
		if (connection != null)
			connection.close();
	}
}
