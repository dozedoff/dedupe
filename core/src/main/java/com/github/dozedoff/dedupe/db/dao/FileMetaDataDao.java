/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.dao;

import java.nio.file.Path;
import java.sql.SQLException;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;

public class FileMetaDataDao extends BaseDaoImpl<FileMetaData, Integer> {
	private final SelectArg pathArg;
	private PreparedQuery<FileMetaData> pathQuery;

	/**
	 * Create an extended DAO for {@link FileMetaData}.
	 * 
	 * @param connectionSource
	 *            connection to use
	 * @param dataClass
	 *            class this DAO is used for
	 * @throws SQLException
	 *             if there is a database error
	 */
	public FileMetaDataDao(ConnectionSource connectionSource, Class<FileMetaData> dataClass) throws SQLException {
		super(connectionSource, dataClass);

		pathArg = new SelectArg();
		prepareStatements();
	}

	private void prepareStatements() throws SQLException {
		pathQuery = this.queryBuilder().where().eq(FileMetaData.PATH_COLUMN_NAME, pathArg).prepare();
	}

	/**
	 * Check if the database contains metadata for the path.
	 * 
	 * @param path
	 *            to query for
	 * @return true if meta data is present for the path
	 * @throws SQLException
	 *             if there is an error accessing the database
	 */
	public boolean hasMetaData(Path path) throws SQLException {
		return getMetaDataForPath(path) != null;
	}

	/**
	 * Get the metadata for the given path
	 * 
	 * @param path
	 *            to load metadata for
	 * @return the metadata if found, else null
	 * @throws SQLException
	 *             if there is an error accessing the database
	 */
	public FileMetaData getMetaDataForPath(Path path) throws SQLException {
		FileMetaData meta;

		synchronized (pathArg) {
			pathArg.setValue(path);
			meta = queryForFirst(pathQuery);
		}

		return meta;
	}
}
