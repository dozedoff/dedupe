/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.table;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

public class FileMetaDataTest {
	private static final String PATH = "foo";
	private static final byte[] HASH_TEMPLATE = new byte[] { 1, 2, 3, 4, 5 };

	private FileSystem fs;
	private Path path;
	private byte[] hash;

	private FileMetaData cut;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();
		path = fs.getPath(PATH);
		hash = HASH_TEMPLATE.clone();

		cut = new FileMetaData(PATH, 0, 0, hash);
	}

	@Test
	public void testGetPathFileSystem() throws Exception {
		assertThat(cut.getPath(fs), is(path));
	}

	@Test
	public void testGetPath() throws Exception {
		assertThat(cut.getPath(), is(Paths.get(PATH)));
	}

	@Test
	public void testGetPathAsString() throws Exception {
		assertThat(cut.getPathAsString(), is(PATH));
	}

	@Test
	public void testGetHash() throws Exception {
		assertArrayEquals(cut.getHash(), hash);
	}
}
