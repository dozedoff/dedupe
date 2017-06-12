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

public class DatabaseTest {
	private Database cut;
	private Path tempDb;

	@Before
	public void setUp() throws Exception {
		tempDb = Files.createTempFile(DatabaseTest.class.getSimpleName(), ".db");
	}

	@After
	public void tearDown() throws Exception {
		cut.close();
	}

	@Test
	public void testInMemoryDatabase() throws Exception {
		cut = Database.inMemoryDatabase();

		assertThat(cut, is(notNullValue()));
	}

	@Test
	public void testDatabaseString() throws Exception {
		cut = new Database(tempDb.toString());

		assertThat(cut, is(notNullValue()));
	}

	@Test
	public void testGetConnectionSource() throws Exception {
		cut = new Database(tempDb.toString());

		assertThat(cut.getConnectionSource(), is(notNullValue()));
	}

}
