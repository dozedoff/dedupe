/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.duplicate.CompareFile;
import com.google.common.jimfs.Jimfs;

public class HardLinkerTest {
	private static final byte[] DATA_A_TEMPLATE = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	private static final byte[] DATA_B_TEMPLATE = { 2, 65, 7, 5, 3, 21, 2, 4, 5, 8, 97, 4, 2, 34, 38, 1, 38, 44, 2 };

	private FileSystem fs;
	private FileSystem fs2;

	private Path fileA;
	private Path fileB;
	private Path fileC;

	private byte[] dataA;
	private byte[] dataB;

	private HardLinker cut;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();
		fs2 = Jimfs.newFileSystem();

		dataA = DATA_A_TEMPLATE.clone();
		dataB = DATA_B_TEMPLATE.clone();

		fileA = fs.getPath("A");
		fileB = fs.getPath("B");
		fileC = fs2.getPath("C");

		Files.write(fileA, dataA);
		Files.write(fileB, dataB);
		Files.write(fileC, dataB);

		cut = new HardLinker();

		assertThat(CompareFile.equal(fileA, fileB), is(false));
	}

	@Test
	public void testLink() throws Exception {
		cut.link(fileA, fileB);

		assertThat(CompareFile.equal(fileA, fileB), is(true));
	}

	@Test
	public void testLinkAcrossFileSystem() throws Exception {
		cut.link(fileA, fileC);

		assertThat(CompareFile.equal(fileA, fileC), is(false));
	}

	@Test
	public void testLinkBackupAlreadyExists() throws Exception {
		Files.createFile(fs.getPath("B.tmp"));

		cut.link(fileA, fileB);

		assertThat(CompareFile.equal(fileA, fileB), is(false));
	}

	@Test
	public void testAllOk() throws Exception {
		assertThat(cut.link(fileA, fileB), is(true));
	}

	@Test
	public void testAllNotOk() throws Exception {
		cut.link(fileA, fileC);

		assertThat(cut.link(fileA, fileC), is(false));
	}
}
