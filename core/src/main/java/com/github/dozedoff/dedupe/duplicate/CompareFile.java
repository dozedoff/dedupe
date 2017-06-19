/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for comparing files.
 * 
 * @author Nicholas Wright
 *
 */
public class CompareFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(CompareFile.class);

	/**
	 * Compares the contents of two files
	 * 
	 * @param path1
	 *            of a file to compare
	 * @param path2
	 *            of a file to compare against
	 * @return true if the files have the same contents
	 * @throws IOException
	 *             if there is an error accessing the files
	 */
	public static boolean equal(Path path1, Path path2) throws IOException {
		LOGGER.trace("Comparing {} to {}", path1, path2);
		try (InputStream is1 = new BufferedInputStream(Files.newInputStream(path1))) {
			try (InputStream is2 = new BufferedInputStream(Files.newInputStream(path2))) {
				int read;
				long index = 0;

				while (true) {
					read = is1.read();

					if (read == -1) {
						boolean identical;

						if (is2.read() == -1) {
							LOGGER.trace("{} and {} are identical", path1, path2);
							identical = true;
						} else {
							LOGGER.trace("{} contents is shorter than {}, finished comparing at position {}", path1,
									path2, index);
							identical = false;
						}

						return identical;
					}

					if (read != is2.read()) {
						LOGGER.trace("{} differs from {} at byte position {}", path2, path1, index);
						return false;
					}

					index++;
				}
			}
		}
	}
}
