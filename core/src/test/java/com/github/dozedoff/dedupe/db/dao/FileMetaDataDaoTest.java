/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.Database;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.dao.DaoManager;

public class FileMetaDataDaoTest {
	private static final String PATH_EXISTS = "exists";
	private static final String PATH_NEW = "new";

	private Database database;
	private FileMetaDataDao cut;

	private Path pathExists;
	private Path pathNew;

	private FileMetaData existingMeta;

	@Before
	public void setUp() throws Exception {
		database = Database.inMemoryDatabase();

		cut = DaoManager.createDao(database.getConnectionSource(), FileMetaData.class);
		existingMeta = new FileMetaData(PATH_EXISTS, 0, 0, new byte[0]);
		
		pathExists = Paths.get(PATH_EXISTS);
		pathNew = Paths.get(PATH_NEW);

		cut.create(existingMeta);
		cut.clearObjectCache();
	}

	@Test
	public void testHasMetaDataWithExistingPath() throws Exception {
		assertThat(cut.hasMetaData(pathExists), is(true));
	}

	@Test
	public void testHasMetaDataWithNonExistingPath() throws Exception {
		assertThat(cut.hasMetaData(pathNew), is(false));
	}

	@Test
	public void testGetMetaDataForPathWithExistingPath() throws Exception {
		assertThat(existingMeta.equals(cut.getMetaDataForPath(pathExists)), is(true));
	}

	@Test
	public void testGetMetaDataForPathWithNonExistingPath() throws Exception {
		assertThat(cut.getMetaDataForPath(pathNew), is(nullValue()));
	}
}
