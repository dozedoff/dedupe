/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.github.dozedoff.dedupe.db.table.FileLink;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;

public class FileLinkDao extends BaseDaoImpl<FileLink, Integer> {
	private static final String LINK_ID_COLUMN = "link_id";
	private static final String SOURCE_ID_COLUMN = "source_id";

	private final PreparedDelete<FileLink> linkDelete;
	private final PreparedDelete<FileLink> allMetaDelete;
	private final PreparedQuery<FileLink> linkQuery;

	private final SelectArg deleteLink;
	private final SelectArg deleteAllLink;
	private final SelectArg deleteAllSource;
	private final SelectArg querySource;

	/**
	 * Create an extended dao for {@link FileLink}.
	 * 
	 * @param connectionSource
	 *            connection to use
	 * @param dataClass
	 *            class this DAO is used for
	 * @throws SQLException
	 *             if there is a database error
	 */
	public FileLinkDao(ConnectionSource connectionSource, Class<FileLink> dataClass) throws SQLException {
		super(connectionSource, dataClass);

		this.deleteLink = new SelectArg();
		this.querySource = new SelectArg();
		this.deleteAllLink = new SelectArg();
		this.deleteAllSource = new SelectArg();

		DeleteBuilder<FileLink, Integer> db = deleteBuilder();
		db.where().eq(LINK_ID_COLUMN, deleteLink);
		this.linkDelete = db.prepare();

		DeleteBuilder<FileLink, Integer> metaDeleteBuilder = deleteBuilder();
		metaDeleteBuilder.where().eq(LINK_ID_COLUMN, deleteAllLink).or().eq(SOURCE_ID_COLUMN, deleteAllSource);
		this.allMetaDelete = metaDeleteBuilder.prepare();

		this.linkQuery = queryBuilder().where().eq(SOURCE_ID_COLUMN, querySource).prepare();
	}

	/**
	 * Create a link from a file to a source. If link already exists, it will be deleted.
	 * 
	 * @param source
	 *            to link to
	 * @param link
	 *            that points to the source
	 * @throws SQLException
	 *             if there is an error accessing the database
	 */
	public void linkFiles(FileMetaData source, FileMetaData link) throws SQLException {
		synchronized (deleteLink) {
			TransactionManager.callInTransaction(connectionSource, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				deleteLink.setValue(link);
				delete(linkDelete);
				create(new FileLink(source, link));

				return null;
			}
		});
		}
	}

	/**
	 * Get all known files that point to the given source.
	 * 
	 * @param source
	 *            to find links for
	 * @return a list of files pointing to this source
	 * @throws SQLException
	 *             if there is an error accessing the database
	 */
	public List<FileMetaData> getLinksTo(FileMetaData source) throws SQLException {
		List<FileLink> links;

		synchronized (querySource) {
			querySource.setValue(source);
			links = query(linkQuery);
		}

		return links.parallelStream().map(link -> link.getLink()).collect(Collectors.toList());
	}

	/**
	 * Delete all links that reference the given {@link FileMetaData}.
	 * 
	 * @param metadata
	 *            for which all links should be deleted
	 * @throws SQLException
	 *             if there is an error accessing the database
	 */
	public void deleteLinksWith(FileMetaData metadata) throws SQLException {
		synchronized (deleteAllLink) {
			deleteAllLink.setValue(metadata);
			deleteAllSource.setValue(metadata);

			delete(allMetaDelete);
		}
	}
}
