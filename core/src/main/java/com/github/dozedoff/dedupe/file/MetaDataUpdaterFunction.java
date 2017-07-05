/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.dedupe.db.BatchWriter;
import com.github.dozedoff.dedupe.db.dao.FileLinkDao;
import com.github.dozedoff.dedupe.db.dao.FileMetaDataDao;
import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.duplicate.VerifyMetaData;

/**
 * Function for updating metadata.
 * 
 * @author Nicholas Wright
 *
 */
public class MetaDataUpdaterFunction implements Function<Path, FileMetaData> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataUpdaterFunction.class);

	private final FileMetaDataDao metaDao;
	private final FileLinkDao linkDao;
	private final VerifyMetaData verify;
	private final MetaData metaData;
	private final BatchWriter<FileMetaDataDao, FileMetaData> batchWriter;

	private final AtomicInteger updatedMeta;
	private final AtomicInteger existingMeta;
	private final AtomicInteger newMeta;
	private final AtomicInteger totalFiles;

	/**
	 * Create a new Function instance for updating metadata
	 * 
	 * @param metaDao
	 *            DAO for metadata access
	 * @param linkDao
	 *            DAO for link data access
	 * @param verify
	 *            class for verifying metadata
	 * @param metaData
	 *            class for generating metadata
	 * @param batchWriter
	 *            class for creating / updating file metadata
	 */
	public MetaDataUpdaterFunction(FileMetaDataDao metaDao, FileLinkDao linkDao, VerifyMetaData verify, MetaData metaData,
			BatchWriter<FileMetaDataDao, FileMetaData> batchWriter) {

		this.metaDao = metaDao;
		this.linkDao = linkDao;
		this.verify = verify;
		this.metaData = metaData;
		this.batchWriter = batchWriter;

		this.updatedMeta = new AtomicInteger();
		this.existingMeta = new AtomicInteger();
		this.newMeta = new AtomicInteger();
		this.totalFiles = new AtomicInteger();
	}

	/**
	 * Apply the path to the function. This will check the database for the metadata and update the database if needed.
	 * 
	 * @param t
	 *            path for which the metadata should be updated
	 * @return the {@link FileMetaData} or an empty {@link FileMetaData} if there was an error
	 */
	@Override
	public FileMetaData apply(Path t) {
		FileMetaData meta = null;

		totalFiles.getAndIncrement();

		try {
			if (metaDao.hasMetaData(t)) {
				meta = metaDao.getMetaDataForPath(t);

				if (verify.hasChanged(meta)) {
					LOGGER.info("File {} has changed, updating metadata", meta.getPath());
					updatedMeta.getAndIncrement();
					metaData.updateMetaData(meta);
					batchWriter.add(meta);
					linkDao.deleteLinksWith(meta);
				}

				existingMeta.getAndIncrement();
			} else {
				meta = metaData.createMetaDataFromFile(t);
				batchWriter.add(meta);
				newMeta.getAndIncrement();
			}

			return meta;
		} catch (IOException e) {
			LOGGER.warn("Failed to generate metadata for {}: {}", t, e.toString());
		} catch (SQLException e) {
			LOGGER.warn("Failed to access database: {} cause: {}", e.toString(),
					e.getCause() == null ? "null" : e.getCause().toString());
		}

		return new FileMetaData();
	}

	/**
	 * Get the count of {@link FileMetaData} updated.
	 * 
	 * @return number of updates
	 */
	public int updated() {
		return updatedMeta.get();
	}

	/**
	 * Get the count of {@link Path}s for with {@link FileMetaData} was found in the database.
	 * 
	 * @return number of files with existing metadata
	 */
	public int existing() {
		return existingMeta.get();
	}

	/**
	 * Get the count of {@link Path}s that did not have a {@link FileMetaData} record.
	 * 
	 * @return number of created {@link FileMetaData} entries
	 */
	public int created() {
		return newMeta.get();
	}

	/**
	 * Get the total count of processed {@link Path}s.
	 * 
	 * @return number of processed {@link Path}s
	 */
	public int total() {
		return totalFiles.get();
	}

	/**
	 * Get the number of errors encountered. Counts IO errors and database errors.
	 * 
	 * @return number of errors
	 */
	public int errors() {
		return total() - existing() - created();
	}
}
