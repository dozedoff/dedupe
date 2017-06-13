/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.to;
import static org.hamcrest.CoreMatchers.is;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

@RunWith(MockitoJUnitRunner.class)
public class BatchWriterTest {
	private static final long DURATION = 100;
	private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;

	private static final long TIMEOUT_FACTOR = 5;
	private static final Duration TIMEOUT = new Duration(DURATION * TIMEOUT_FACTOR, UNIT);

	private BatchWriter<Dao<FileMetaData, Integer>, FileMetaData> cut;
	private Database database;

	private Dao<FileMetaData, Integer> dao;
	
	private List<FileMetaData> testData;

	@Before
	public void setUp() throws Exception {
		database = Database.inMemoryDatabase();
		dao = DaoManager.createDao(database.getConnectionSource(), FileMetaData.class);
		cut = new BatchWriter<Dao<FileMetaData, Integer>, FileMetaData>(dao);

		testData = new LinkedList<FileMetaData>();

		testData.add(new FileMetaData("", 0, 0, new byte[0]));
		testData.add(new FileMetaData("a", 0, 0, new byte[0]));
		testData.add(new FileMetaData("b", 0, 0, new byte[0]));
	}

	@After
	public void tearDown() throws Exception {
		database.close();
	}

	@Test
	public void testAddWithFlushThresholdReached() throws Exception {
		cut = new BatchWriter<Dao<FileMetaData, Integer>, FileMetaData>(dao, DURATION, UNIT);

		cut.add(testData.get(0));
		
		await().atMost(TIMEOUT).until(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				cut.flushCheck();
				return dao.countOf() == 1L;
			}
		});
	}

	@Test
	public void testAddWithoutFlushThresholdReached() throws Exception {
		cut.add(testData.get(0));

		await().pollDelay(new Duration(DURATION / 2, UNIT)).untilCall(to(dao).countOf(), is(0L));
	}

	@Test
	public void testFlush() throws Exception {
		testData.forEach(meta -> cut.add(meta));

		cut.flush();

		await().atMost(TIMEOUT).untilCall(to(dao).countOf(), is((long) testData.size()));
	}

	@Test
	public void testShutdownFlushes() throws Exception {
		testData.forEach(meta -> cut.add(meta));

		cut.shutdown();

		await().atMost(TIMEOUT).untilCall(to(dao).countOf(), is((long) testData.size()));
	}

	@Test(expected = IllegalStateException.class)
	public void testAddAfterShutdownThrowsException() throws Exception {
		cut.shutdown();

		cut.add(testData.get(0));
	}
}
