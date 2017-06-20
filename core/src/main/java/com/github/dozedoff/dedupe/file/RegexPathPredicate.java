/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Predicate that will match if the path matches the given regex {@link Pattern}.
 * 
 * @author Nicholas Wright
 */
public class RegexPathPredicate implements Predicate<Path> {
	private final Pattern regexPattern;

	/**
	 * Create a instance with the given {@link Pattern} to match paths.
	 * 
	 * @param regexPattern
	 *            for matching paths
	 */
	public RegexPathPredicate(Pattern regexPattern) {
		this.regexPattern = regexPattern;
	}

	/**
	 * Create a instance with the given String to match paths. The String will be compiled to a {@link Pattern}.
	 * 
	 * @param regex
	 *            for matching paths
	 */
	public RegexPathPredicate(String regex) {
		this.regexPattern = Pattern.compile(regex);
	}

	/**
	 * Test the path against the {@link Pattern}.
	 * 
	 * @param test
	 *            path to test
	 * @return true if the path matches the {@link Pattern}
	 */
	@Override
	public boolean test(Path test) {
		return regexPattern.matcher(test.toString()).matches();
	}

}
