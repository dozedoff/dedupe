/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.table;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * This class represents a row of the file metadata table.
 * 
 * @author Nicholas Wright
 *
 */
@DatabaseTable
public class FileMetaData {
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(unique = true, index = true)
	private String path;
	@DatabaseField(index = true)
	private long size;
	@DatabaseField
	private long modifiedTime;
	@DatabaseField(index = true)
	private long hash;

	/**
	 * Create a new file metadata entry
	 * 
	 * @param path
	 *            of the file
	 * @param size
	 *            of the file in bytes
	 * @param modifiedTime
	 *            when the file was last modified
	 * @param hash
	 *            of the file
	 */
	public FileMetaData(String path, long size, long modifiedTime, long hash) {
		this.path = path;
		this.size = size;
		this.modifiedTime = modifiedTime;
		this.hash = hash;
	}

	/**
	 * Convert the stored file path string to a {@link Path} using the given {@link FileSystem}.
	 * 
	 * @param fileSystem
	 *            to use to convert the path
	 * @return a {@link Path} for the file
	 */
	public Path getPath(FileSystem fileSystem) {
		return fileSystem.getPath(getPathAsString());
	}

	/**
	 * Convert the stored file path string to a {@link Path} using the default {@link FileSystem}.
	 * 
	 * @return a {@link Path} for the file
	 */
	public Path getPath() {
		return getPath(FileSystems.getDefault());
	}

	/**
	 * Returns the {@link String} for the file path.
	 * 
	 * @return the files path as a {@link String}
	 */
	public String getPathAsString() {
		return path;
	}

	/**
	 * The size of the file in bytes.
	 * 
	 * @return the file size in bytes
	 */
	public long getSize() {
		return size;
	}

	/**
	 * The timestamp when the file was last modified.
	 * 
	 * @return the files last modiefied timestamp
	 */
	public long getModifiedTime() {
		return modifiedTime;
	}

	/**
	 * The hash of the file based on it's contents.
	 * 
	 * @return a hash representing the file
	 */
	public long getHash() {
		return hash;
	}
}
