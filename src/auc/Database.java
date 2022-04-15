package auc;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
	//This class creates a connection to the remote database

	private Connection connection;
	private Statement stmt;

	public Database() {
		this.connection = getConnection();
		try {
			this.stmt = connection.createStatement();
		} catch (SQLException e) {
			System.err.println("Failed to connect to create statement");
			System.err.println(e.getMessage());
		}
	}

	private Connection getConnection() {
		try {
			URI dbUri = new URI(System.getenv("DATABASE_URL"));
			String username = dbUri.getUserInfo().split(":")[0];
			String password = dbUri.getUserInfo().split(":")[1];
			String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?user="
					+ username + "&password=" + password + "&ssl=true&sslmode=require";
			return DriverManager.getConnection(dbUrl);
		} catch (URISyntaxException | SQLException e) {
			System.err.println("Failed to connect to database");
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	public Statement getStatement() {
		return stmt;
	}

}
