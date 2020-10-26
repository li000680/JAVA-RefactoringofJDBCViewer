package jdbc.builder;

import java.util.Map;

public class MySQLURLBuilder extends JDBCURLBuilder  {

	public MySQLURLBuilder() {
		setDB("mysql");
	}
	
	@Override
	public  String getURL() {
		StringBuilder builder = new StringBuilder();
		builder.append(JDBC).append(":");
		builder.append(dbType).append("://");
		builder.append(hostAddress).append(":");
		builder.append(portNumber).append("/");
		builder.append(catalogName).append("?");
		int i=0;
		for (Map.Entry<String, String> property: properties.entrySet()) {
		 builder.append(property);
		 if(i!=properties.size()-1)
			 builder.append("&");
             i++;
         }	
		return builder.toString();
	}
}

