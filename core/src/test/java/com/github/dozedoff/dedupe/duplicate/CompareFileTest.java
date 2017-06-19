/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

public class CompareFileTest {
	private static final int TEST_DATA_SIZE = 10000;

	private Random rand;

	private FileSystem fs;

	private Path pathA;
	private Path pathB;
	private Path pathC;
	private Path pathZero;

	private byte[] dataA;
	private byte[] dataB;

	@Before
	public void setUp() throws Exception {
		fs = Jimfs.newFileSystem();
		rand = new Random();

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
	}

	private byte[] randomData(int length) {
		byte[] data = new byte[length];

		for (int i = 0; i < length; i++) {
			data[i] = (byte) (rand.nextInt(Byte.MAX_VALUE + 1));
		}

		return data;
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
}
