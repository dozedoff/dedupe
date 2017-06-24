/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;

/**
 * Class for batching database writes. Trades reliability for performance.
 * 
 * @author Nicholas Wright
 *
 * @param <D>
 *            DAO class for database access
 * @param <T>
 *            the class that will be written
 */
public class BatchWriter<D extends Dao<T, ?>, T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchWriter.class);

	private static final long DEFAULT_FLUSH_INTERVAL_DUARTION = 1;
	private static final TimeUnit DEFAULT_FLUSH_INTERVAL_UNIT = TimeUnit.MINUTES;

	private final D dao;
	private final ConcurrentLinkedQueue<T> toPersist;
	private final ConcurrentHashMap<T, T> toReplace;
	private final long flushIntervalDuration;
	private final TimeUnit flushIntervalUnit;
	private boolean isShuttingDown;
	private Semaphore isFlushing;
	private Stopwatch intervalTimer;

	/**
	 * Create a new batch writer that will automatically write the rows after a call to {@link BatchWriter#add(Object)}
	 * after the interval has elapsed.
	 * 
	 * @param dao
	 *            to use for database access
	 * @param duration
	 *            value for the interval duration
	 * @param timeunit
	 *            the unit for the interval
	 */
	public BatchWriter(D dao, long duration, TimeUnit timeunit) {
		this.dao = dao;
		this.flushIntervalDuration = duration;
		this.flushIntervalUnit = timeunit;

		this.toPersist = new ConcurrentLinkedQueue<T>();
		this.toReplace = new ConcurrentHashMap<T, T>();
		isFlushing = new Semaphore(1);
		this.intervalTimer = Stopwatch.createStarted();
	}

	/**
	 * Create a new batch writer that will automatically write the rows after a call to {@link BatchWriter#add(Object)}
	 * after the interval has elapsed. Uses the default interval of 1 minute.
	 * 
	 * @param dao
	 *            to use for database access
	 */
	public BatchWriter(D dao) {
		this(dao, DEFAULT_FLUSH_INTERVAL_DUARTION, DEFAULT_FLUSH_INTERVAL_UNIT);
	}

	/**
	 * Add a row to the queue to be written at a later time. If the row exists, it will be updated, otherwise a new row
	 * will be created. If the interval has been exceeded, a {@link BatchWriter#flush()} will be triggered after the
	 * insert.
	 * 
	 * @param enqueue
	 *            element to queue for write
	 */
	public void add(T enqueue) {
		shutdownCheck();

		toPersist.add(enqueue);

		flushCheck();
	}

	private void shutdownCheck() {
		if (isShuttingDown) {
			throw new IllegalStateException("Batchwriter is shutting down");
		}
	}

	/**
	 * Replace an existing entry, performs a atomic delete / create.
	 * 
	 * @param oldEntry
	 *            to replace
	 * @param newEntry
	 *            to use instead
	 */
	public void replace(T oldEntry, T newEntry) {
		shutdownCheck();

		toReplace.put(oldEntry, newEntry);
	}

	/**
	 * Check if the interval has been reached and flush if necessary.
	 */
	public void flushCheck() {
		if (intervalTimer.elapsed(flushIntervalUnit) > flushIntervalDuration) {
			LOGGER.trace("Flush interval exceeded with a time of {} , flushhing...", intervalTimer);
			flush();
		}
	}

	private synchronized void writeToDatabase() {
		this.intervalTimer.reset();

		try {
			writeNewEntries();
			writeReplacedEntries();
		} catch (SQLException e) {
			LOGGER.warn("Batch transaction call failed: {}", e.toString());
		}

		this.intervalTimer.start();
	}

	private void writeNewEntries() throws SQLException {
		TransactionManager.callInTransaction(dao.getConnectionSource(), new Callable<Void>() {
			@Override
			public Void call() {
				while (!toPersist.isEmpty()) {
					T toWrite = toPersist.poll();
					try {
						dao.createOrUpdate(toWrite);
					} catch (SQLException e) {
						LOGGER.warn("Failed to write {}: {}", toWrite, e.toString());
					}
				}
				return null;
			}
		});
	}

	private void writeReplacedEntries() throws SQLException {
		TransactionManager.callInTransaction(dao.getConnectionSource(), new Callable<Void>() {
			@Override
			public Void call() {
				Iterator<Entry<T, T>> iter = toReplace.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<T, T> entry = iter.next();
					T oldEntry = entry.getKey();
					T newEntry = entry.getValue();

					try {
						replaceEntry(oldEntry, newEntry);
						iter.remove();
					} catch (SQLException e) {
						LOGGER.warn("Failed to replace {} with {}: {}", oldEntry, newEntry, e.toString());
					}
				}

				return null;
			}
		});
	}

	private void replaceEntry(T oldEntry, T newEntry) throws SQLException {
		dao.delete(oldEntry);
		dao.create(newEntry);
	}

	/**
	 * Resets the internal flush interval and flushes queued elements to the database.
	 */
	public void flush() {
		if (isFlushing.tryAcquire()) {
			writeToDatabase();
			isFlushing.release();
		}
	}

	/**
	 * Shutdown the {@link BatchWriter} and flush any pending elements. Attempts to {@link BatchWriter#add(Object)}
	 * after this call will throw {@link IllegalStateException}.
	 */
	public void shutdown() {
		isShuttingDown = true;

		LOGGER.info("Batch writer is shutting down, writing {} pending rows...", toPersist.size());

		writeToDatabase();
	}
}
