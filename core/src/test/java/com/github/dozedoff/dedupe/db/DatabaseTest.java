/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

public class DatabaseTest {
	private Database cut;
	private Database memoryDb;
	private Path tempDb;

	@Before
	public void setUp() throws Exception {
		tempDb = Files.createTempFile(DatabaseTest.class.getSimpleName(), ".db");
		cut = new Database(tempDb.toString());
		memoryDb = Database.inMemoryDatabase();
	}

	@After
	public void tearDown() throws Exception {
		memoryDb.close();
		cut.close();
	}

	@Test
	public void testInMemoryDatabase() throws Exception {
		assertThat(memoryDb, is(notNullValue()));
	}

	@Test
	public void testDatabaseString() throws Exception {
		assertThat(cut, is(notNullValue()));
	}

	@Test
	public void testGetConnectionSource() throws Exception {
		assertThat(cut.getConnectionSource(), is(notNullValue()));
	}

	@Test
	public void testFileMetadataTable() throws Exception {
		Dao<FileMetaData, Integer> dao = DaoManager.createDao(memoryDb.getConnectionSource(), FileMetaData.class);

		dao.create(new FileMetaData());
	}
}
