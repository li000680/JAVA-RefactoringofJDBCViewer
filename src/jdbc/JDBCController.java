package jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jdbc.builder.JDBCURLBuilder;


public class JDBCController {
	private JDBCURLBuilder builder;
	private StringProperty tableInUse;
	private JDBCModel model;
	private ObservableList<String> tableNames;

	public JDBCController() {
		this.tableNames = FXCollections.observableList(new ArrayList<>());
		this.model = new JDBCModel();
		this.tableInUse = new SimpleStringProperty();
		tableInUse.addListener((value, oldValue, newValue) -> {
			try {
				model.getAndInitializeColumnNames(newValue);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	public void close() throws SQLException {
		model.close();
	}

	public List<String> getColumnNames() throws SQLException {
		return model.getAndInitializeColumnNames(tableInUse.get());
	}

	public StringProperty tableInUseProperty() {
		return tableInUse;
	}

	public JDBCController setURLBuilder(JDBCURLBuilder builder) {
		this.builder=builder;
		return new JDBCController();
	}

	public JDBCController setDataBase(String address, String port, String catalog) {
		builder.setAddress(address); 
		builder.setPort(port); 
		builder.setCatalog(catalog);
		return new JDBCController();
	}

	public JDBCController addConnectionURLProperty(String key, String value) {
		 builder.addURLProperty(key,value);
		 return new JDBCController(); 
	}

	public JDBCController setCredentials(String user, String pass) {
		model.setCredentials(user,pass);
		return  new JDBCController(); 
	}

	public boolean isConnected() throws SQLException {
		return model.isConnected();
	}

	public List<List<Object>> getAll() throws SQLException {
		return model.getAll(tableInUse.get());
	}

	public ObservableList<String> getTableNames() throws SQLException {
		if (model.isConnected()) {
			tableNames.clear();
			tableNames.addAll(model.getAndInitializeTableNames());
		}
		return tableNames;
	}

	public List<List<Object>> search(String searchTerm) throws SQLException {
		return model.search(tableInUse.get(), searchTerm);
	}

	public JDBCController connect() throws SQLException {
		model.connectTo(builder.getURL());
		return new JDBCController();
	}

}
