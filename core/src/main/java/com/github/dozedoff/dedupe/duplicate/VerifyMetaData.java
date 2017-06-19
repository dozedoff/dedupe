/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.file.MetaData;

/**
 * Class for verifying stored metadata.
 * 
 * @author Nicholas Wright
 *
 */
public class VerifyMetaData {
	private final FileSystem fileSystem;
	private final MetaData metaData;

	/**
	 * Create a new instance for verifying metadata using the provided {@link FileSystem}.
	 * 
	 * @param metaData
	 *            meta data instance to read file meta data
	 * 
	 * @param fileSystem
	 *            to use to resolve file paths
	 */
	public VerifyMetaData(MetaData metaData, FileSystem fileSystem) {
		this.fileSystem = fileSystem;
		this.metaData = metaData;
	}

	/**
	 * Create a new instance for verifying metadata using the {@link FileSystems#getDefault()} {@link FileSystem}.
	 * 
	 * @param metaData
	 *            meta data instance to read file meta data
	 */
	public VerifyMetaData(MetaData metaData) {
		this(metaData, FileSystems.getDefault());
	}

	/**
	 * Check if the stored file size matches the stored one.
	 * 
	 * @param metadata
	 *            to check file size for
	 * @return true if the file sizes match
	 * @throws IOException
	 *             if there is an error accessing the file
	 */
	public boolean hasMatchingSize(FileMetaData metadata) throws IOException {
		return this.metaData.size(metadata.getPath(fileSystem)) == metadata.getSize();
	}

	/**
	 * Check if the stored modified time matches the stored one.
	 * 
	 * @param metadata
	 *            to check the modified time for
	 * @return true if the modified time stamps match
	 * @throws IOException
	 *             if there is an error accessing the file
	 */
	public boolean hasMatchingModifiedTime(FileMetaData metadata) throws IOException {

		return this.metaData.lastModified(metadata.getPath(fileSystem)) == metadata.getModifiedTime();
	}

	/**
	 * Performs a fast check if a file has changed using the modified time stamp and file size.<br>
	 * <br>
	 * <b>Note:</b><br>
	 * This method may not accurately detect changes
	 * 
	 * @param metadata
	 *            to check for changes
	 * @return true if the file and the stored meta data differ
	 * @throws IOException
	 *             if there is an error accessing the file
	 */
	public boolean hasChanged(FileMetaData metadata) throws IOException {
		return !(hasMatchingModifiedTime(metadata) && hasMatchingSize(metadata));
	}
}
