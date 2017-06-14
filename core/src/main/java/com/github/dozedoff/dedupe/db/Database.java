/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db;

import java.sql.SQLException;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

/**
 * The SQLite database the program uses.
 * 
 * @author Nicholas Wright
 *
 */
public class Database {
	private static final String DEFAULT_DATABASE_PATH = "dedupe.db";

	private final JdbcConnectionSource connectionSource;

	/**
	 * Create or open a Database in the default location.
	 * 
	 * @throws SQLException
	 *             if there is an error creating the database
	 */
	public Database() throws SQLException {
		this(DEFAULT_DATABASE_PATH);
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

		DatabaseConnection dbConn = connectionSource.getReadWriteConnection();
		dbConn.executeStatement("PRAGMA page_size=4096;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA cache_size=5120;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA locking_mode=EXCLUSIVE;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA synchronous=NORMAL;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		dbConn.executeStatement("PRAGMA temp_store=MEMORY;", DatabaseConnection.DEFAULT_RESULT_FLAGS);
		connectionSource.releaseConnection(dbConn);

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
