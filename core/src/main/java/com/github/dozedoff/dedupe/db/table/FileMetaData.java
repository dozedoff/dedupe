/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.table;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.Immutable;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * This class represents a row of the file metadata table.
 * 
 * @author Nicholas Wright
 *
 */
@DatabaseTable
@Immutable
final public class FileMetaData {
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(unique = true, index = true)
	private String path;
	@DatabaseField(index = true)
	private long size;
	@DatabaseField
	private long modifiedTime;
	@DatabaseField(index = true)
	private byte[] hash;

	/**
	 * Creates a empty {@link FileMetaData} instance.
	 */
	public FileMetaData() {
		this.path = "";
		this.hash = new byte[0];
	}

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
	public FileMetaData(String path, long size, long modifiedTime, byte[] hash) {
		this.path = path;
		this.size = size;
		this.modifiedTime = modifiedTime;
		this.hash = hash.clone();
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
	public byte[] getHash() {
		return hash.clone();
	}

	/**
	 * Check if the objects are equal.
	 * 
	 * @param obj
	 *            the object to compare to this instance
	 * @return true if all fields except the id match
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FileMetaData) {
			FileMetaData other = (FileMetaData) obj;
			return Objects.equals(this.path, other.path) && Objects.equals(this.size, other.size)
					&& Objects.equals(this.modifiedTime, other.modifiedTime)
					&& Objects.deepEquals(this.hash, other.hash);
		}

		return false;
	}

	/**
	 * Hashcode of this instance of all fields except id.
	 * 
	 * @return the hashcode of this instance
	 */
	@Override
	public int hashCode() {
		return Objects.hash(path, size, modifiedTime, Arrays.hashCode(hash));
	}

	/**
	 * String representation of this object.
	 * 
	 * @return the fields of this instance encoded as a {@link String}
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(FileMetaData.class).add("path", path).add("size", size)
				.add("modt", modifiedTime).add("hash", hash).toString();
	}
}
