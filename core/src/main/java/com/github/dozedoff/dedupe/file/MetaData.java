/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;

/**
 * Read or generate metadata for files.
 * 
 * @author Nicholas Wright
 *
 */
public class MetaData {
	private static final HashFunction SHA512 = Hashing.sha512();

	private final FileSystem fileSystem;

	/**
	 * Create a new instance using the default {@link FileSystem} from {@link FileSystems#getDefault()}.
	 */
	public MetaData() {
		this.fileSystem = FileSystems.getDefault();
	}

	/**
	 * Create a new instance using the provided {@link FileSystem}.
	 * 
	 * @param fileSystem
	 *            to use for resolving paths
	 */
	public MetaData(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	/**
	 * Get the file size.
	 * 
	 * @param path
	 *            to query
	 * @return file size in bytes
	 * @throws IOException
	 *             if there is an IO error
	 */
	public long size(Path path) throws IOException {
		return Files.size(path);
	}

	/**
	 * Get the last modified timestamp of the file.
	 * 
	 * @param path
	 *            to query
	 * @return the timestamp in milliseconds from epoch
	 * @throws IOException
	 *             if there is an IO error
	 */
	public long lastModified(Path path) throws IOException {
		return Files.getLastModifiedTime(path).toMillis();
	}

	/**
	 * Get the hash of the file contents.
	 * 
	 * @param path
	 *            of the file to hash
	 * @return the hash of the file
	 * @throws IOException
	 *             if there is an IO error
	 */
	public byte[] contentHash(Path path) throws IOException {
		HashCode hc = MoreFiles.asByteSource(path).hash(SHA512);
			return hc.asBytes();
	}

	/**
	 * Convenience method to create {@link FileMetaData} instances from file paths.
	 * 
	 * @param file
	 *            for which metadata should be created
	 * @return metadata for the file
	 * @throws IOException
	 *             if there is an IO error
	 */
	public FileMetaData createMetaDataFromFile(Path file) throws IOException {
		return new FileMetaData(file.toString(), size(file), lastModified(file), contentHash(file));
	}

	/**
	 * Update size, modified time and hash for the {@link FileMetaData} object.
	 * 
	 * @param meta
	 *            to update
	 * @throws IOException
	 *             if there is an IO error
	 */
	public void updateMetaData(FileMetaData meta) throws IOException {
		Path path = fileSystem.getPath(meta.getPathAsString());

		meta.setSize(size(path));
		meta.setModifiedTime(lastModified(path));
		meta.setHash(contentHash(path));
	}
}
