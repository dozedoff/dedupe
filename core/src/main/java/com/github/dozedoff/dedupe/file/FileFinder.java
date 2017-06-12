/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Find files in the given paths.
 * 
 * @author Nicholas Wright
 *
 */
public class FileFinder {
	/**
	 * Recursively find files in the given directory.
	 * 
	 * @param directory
	 *            to search for files
	 * @return a stream of files in the directory
	 * @throws IOException if there was an error accessing the file system 
	 */
	public Stream<Path> findFiles(Path directory) throws IOException {
		return Files.walk(directory).filter(new Predicate<Path>() {
			@Override
			public boolean test(Path t) {
				return Files.isRegularFile(t);
			}
		});
	}
}
