/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.github.dozedoff.dedupe.util.TestDataUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.jimfs.Jimfs;

public class CompareFileTest {
	private static final int TEST_DATA_SIZE = 10000;
	private static final int TEST_TIMEOUT = 1000;

	private Random rand;

	private FileSystem fs;

	private Path pathA;
	private Path pathB;
	private Path pathC;
	private Path pathZero;

	private byte[] dataA;
	private byte[] dataB;

	private Multimap<String, FileMetaData> identicalCandiadates;
	private CompareFile cut;

	private FileMetaData metaA;
	private FileMetaData metaB;
	private FileMetaData metaC;
	private FileMetaData metaZero;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();

		dataA = randomData(TEST_DATA_SIZE);
		dataB = randomData(TEST_DATA_SIZE * 2);

		pathA = fs.getPath("A");
		pathB = fs.getPath("B");
		pathC = fs.getPath("C");
		pathZero = fs.getPath("Z");

		Files.write(pathA, dataA);
		Files.write(pathB, dataB);
		Files.write(pathC, dataA);
		Files.write(pathZero, new byte[0]);

		identicalCandiadates = MultimapBuilder.hashKeys().hashSetValues().build();

		String group1Hash = "1";
		identicalCandiadates.put(group1Hash, new FileMetaData(pathA.toString(), 0, 0, new byte[0]));
		identicalCandiadates.put(group1Hash, new FileMetaData(pathB.toString(), 0, 0, new byte[0]));
		identicalCandiadates.put(group1Hash, new FileMetaData(pathC.toString(), 0, 0, new byte[0]));

		metaA = new FileMetaData(pathA.toString(), 0, 0, new byte[0]);
		metaB = new FileMetaData(pathB.toString(), 0, 0, new byte[0]);
		metaC = new FileMetaData(pathC.toString(), 0, 0, new byte[0]);

		String group2Hash = "2";
		metaZero = new FileMetaData(pathZero.toString(), 0, 0, new byte[0]);
		identicalCandiadates.put(group2Hash, metaZero);

		cut = new CompareFile(fs);
	}

	private byte[] randomData(int length) {
		return TestDataUtil.randomData(length);
	}

	@Test
	public void testNotEqualFiles() throws Exception {
		assertThat(CompareFile.equal(pathA, pathB), is(false));
	}

	@Test
	public void testLargerFileFirst() throws Exception {
		assertThat(CompareFile.equal(pathA, pathB), is(false));
	}

	@Test
	public void testEqualFiles() throws Exception {
		assertThat(CompareFile.equal(pathA, pathC), is(true));
	}

	@Test
	public void testFileWithSelf() throws Exception {
		assertThat(CompareFile.equal(pathA, pathA), is(true));
	}

	@Test
	public void testWithZeroFile() throws Exception {
		assertThat(CompareFile.equal(pathZero, pathA), is(false));
	}

	@Test
	public void testAgainstZeroFile() throws Exception {
		assertThat(CompareFile.equal(pathA, pathZero), is(false));
	}

	@Test(expected = NoSuchFileException.class)
	public void testFile1NotFound() throws Exception {
		Files.delete(pathA);

		CompareFile.equal(pathA, pathB);
	}

	@Test(expected = NoSuchFileException.class)
	public void testFile2NotFound() throws Exception {
		Files.delete(pathA);

		CompareFile.equal(pathB, pathA);
	}

	@Test(expected = FileSystemException.class)
	public void testReadDirectory() throws Exception {
		Path dir = fs.getPath("dir");
		Files.createDirectory(dir);

		CompareFile.equal(dir, pathA);
	}

	@Test
	public void testGroupIdenticalFilesFirstGroup() throws Exception {
		List<Collection<FileMetaData>> grouped = cut.groupIdenticalFiles(identicalCandiadates);

		assertThat(grouped, hasItem(containsInAnyOrder(metaA, metaC)));
	}

	@Test(timeout = TEST_TIMEOUT)
	public void testGroupIdenticalFilesSingleCandidate() throws Exception {
		assertThat(cut.groupIdenticalFiles(identicalCandiadates), hasItem(containsInAnyOrder(metaZero)));
	}
}
