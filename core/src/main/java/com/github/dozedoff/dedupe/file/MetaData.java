/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
	private static final HashFunction MURMUR_HASH = Hashing.murmur3_128();

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
			HashCode hc = MoreFiles.asByteSource(path).hash(MURMUR_HASH);
			return hc.asBytes();
	}
}
