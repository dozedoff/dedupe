/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db;

import java.sql.SQLException;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * The SQLite database the program uses.
 * 
 * @author Nicholas Wright
 *
 */
public class Database {
	private final JdbcConnectionSource connectionSource;

	/**
	 * Create or open a Database in the default location.
	 * 
	 * @throws SQLException
	 *             if there is an error creating the database
	 */
	public Database() throws SQLException {
		connectionSource = new JdbcConnectionSource("jdbc:sqlite:dedupe.db");
	}

	/**
	 * Create a in memory Database in the default location.
	 * 
	 * @return a in memory database instance
	 * @throws SQLException
	 *             if there is an error creating the database
	 */
	public static Database inMemoryDatabase() throws SQLException {
		return new Database(":memory:");
	}

	/**
	 * Create or open a Database in the given location.
	 * 
	 * @param databaseFile
	 *            path to the database file
	 * @throws SQLException
	 *             if there is an error creating the database
	 */
	public Database(String databaseFile) throws SQLException {
		connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + databaseFile);

		TableUtils.createTableIfNotExists(getConnectionSource(), FileMetaData.class);
	}

	/**
	 * Get the connection source for this database.
	 * 
	 * @return the connectionsource
	 */
	public JdbcConnectionSource getConnectionSource() {
		return connectionSource;
	}

	/**
	 * Close the database.
	 * 
	 * @throws SQLException
	 *             if there is a database error
	 */
	public void close() throws SQLException {
		connectionSource.close();
	}
}
