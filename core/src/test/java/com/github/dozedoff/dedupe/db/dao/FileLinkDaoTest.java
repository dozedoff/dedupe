/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.Database;
import com.github.dozedoff.dedupe.db.table.FileLink;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;

public class FileLinkDaoTest {

	private FileMetaData metaA;
	private FileMetaData metaB;
	private FileMetaData metaC;
	private FileMetaData metaD;

	private FileLink linkAC;
	private FileLink linkAD;
	private FileLink linkBC;

	private Database database;

	private FileLinkDao cut;
	private FileMetaDataDao metaDao;

	@Before
	public void setUp() throws Exception {
		setupDatabase();

		metaA = createMeta("A");
		metaB = createMeta("B");
		metaC = createMeta("C");
		metaD = createMeta("D");

		linkAC = new FileLink(metaA, metaC);
		linkAD = new FileLink(metaA, metaD);
		linkBC = new FileLink(metaB, metaC);

		cut.create(linkAC);
		cut.create(linkAD);

		cut.clearObjectCache();
	}

	private void setupDatabase() throws SQLException {
		database = Database.inMemoryDatabase();
		TableUtils.createTableIfNotExists(database.getConnectionSource(), FileLink.class);
		TableUtils.createTableIfNotExists(database.getConnectionSource(), FileMetaData.class);


		metaDao = DaoManager.createDao(database.getConnectionSource(), FileMetaData.class);
		cut = DaoManager.createDao(database.getConnectionSource(), FileLink.class);
	}

	private FileMetaData createMeta(String path) throws SQLException {
		FileMetaData meta = new FileMetaData(path, 0, 0, new byte[] {});
		metaDao.create(meta);
		return meta;
	}

	@After
	public void tearDown() throws Exception {
		database.close();
	}

	@Test
	public void testHasLink() throws Exception {
		List<FileLink> links = cut.queryForAll();

		assertThat(links, hasItem(linkAC));
	}

	@Test
	public void testOverwriteExistingLink() throws Exception {
		cut.linkFiles(metaB, metaC);

		List<FileLink> links = cut.queryForAll();

		assertThat(links, hasItem(linkBC));
	}

	@Test
	public void testCreateNewLink() throws Exception {
		cut.linkFiles(metaB, metaA);

		List<FileLink> links = cut.queryForAll();

		assertThat(links, hasItem(new FileLink(metaB, metaA)));
	}

	@Test
	public void testGetLinksTo() throws Exception {
		assertThat(cut.getLinksTo(metaA), containsInAnyOrder(metaC, metaD));
	}

	@Test
	public void testDeleteLinksWithMetaInLink() throws Exception {
		cut.deleteLinksWith(metaC);

		assertThat(cut.queryForAll(), containsInAnyOrder(linkAD));
	}

	@Test
	public void testDeleteLinksWithMetaInSource() throws Exception {
		cut.deleteLinksWith(metaA);

		assertThat(cut.queryForAll(), is(empty()));
	}

}
