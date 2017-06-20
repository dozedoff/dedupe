/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class RegexPathPredicateTest {
	private static final String REGEX = ".*\\.tmp$";

	private Path pathA;
	private Path pathB;
	private Path pathC;

	private RegexPathPredicate cut;

	@Before
	public void setUp() throws Exception {
		pathA = Paths.get("foo.db");
		pathB = Paths.get("bar.tmp");
		pathC = Paths.get("tmp.baz");

		cut = new RegexPathPredicate(REGEX);
	}

	@Test
	public void testNotMatching() throws Exception {
		assertThat(cut.test(pathA), is(false));
	}

	@Test
	public void testMatching() throws Exception {
		assertThat(cut.test(pathB), is(true));
	}

	@Test
	public void testSimilarButNotMatching() throws Exception {
		assertThat(cut.test(pathC), is(false));
	}
}
