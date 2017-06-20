/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Predicates;

/**
 * Find files in the given paths.
 * 
 * @author Nicholas Wright
 *
 */
public class FileFinder {
	private final List<Pattern> excludes;

	/**
	 * Create a new instance that does not exclude any files.
	 */
	public FileFinder() {
		excludes = Collections.emptyList();
	}

	/**
	 * Create a new instance that excludes paths that match the given regex patterns.
	 * 
	 * @param exclude
	 *            regex patterns of paths to exclude
	 */
	public FileFinder(String... exclude) {
		this(Arrays.asList(exclude));
	}

	/**
	 * Create a new instance that excludes paths that match the given regex patterns.
	 * 
	 * @param exclude
	 *            regex patterns of paths to exclude
	 */
	public FileFinder(List<String> exclude) {
		this.excludes = exclude.stream().map(Pattern::compile).collect(Collectors.toList());
	}

	/**
	 * Recursively find files in the given directory.
	 * 
	 * @param directory
	 *            to search for files
	 * @return a stream of files in the directory
	 * @throws IOException if there was an error accessing the file system 
	 */
	public Stream<Path> findFiles(Path directory) throws IOException {
		return Files.walk(directory).filter(ignoredPaths().negate()).filter(new Predicate<Path>() {
			@Override
			public boolean test(Path t) {
				return Files.isRegularFile(t);
			}
		});
	}

	private Predicate<Path> ignoredPaths() {
		Predicate<Path> predicate = Predicates.alwaysFalse();

		for (Pattern pattern : excludes) {
			predicate = predicate.or(new RegexPathPredicate(pattern));
		}

		return predicate;
	}
}
