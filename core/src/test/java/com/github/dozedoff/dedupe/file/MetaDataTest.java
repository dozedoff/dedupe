/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.file;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.table.FileMetaData;
import com.google.common.hash.HashCode;
import com.google.common.jimfs.Jimfs;

public class MetaDataTest {
	private static final byte[] TEST_DATA_TEMPLATE = "The quick brown fox jumps over the lazy dog"
			.getBytes(StandardCharsets.US_ASCII);
	private static final String TEST_DATA_SHA512_HASH = "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6";

	private MetaData cut;
	private byte[] data;
	private FileSystem fs;
	private Path testFile;
	private FileTime modTime;

	@Before
	public void setUp() throws Exception {
		cut = new MetaData();

		fs = Jimfs.newFileSystem();
		data = TEST_DATA_TEMPLATE.clone();
		modTime = FileTime.from(1, TimeUnit.SECONDS);

		createTestFile();
	}

	private void createTestFile() throws IOException {
		testFile  = fs.getPath("foo");

		Files.write(testFile, data);
		Files.setLastModifiedTime(testFile, modTime);
	}

	@Test
	public void testSize() throws Exception {
		assertThat(cut.size(testFile), is(Long.valueOf(data.length)));
	}

	@Test
	public void testLastModified() throws Exception {
		assertThat(cut.lastModified(testFile), is(modTime.toMillis()));
	}

	@Test
	public void testContentHash() throws Exception {
		assertThat(cut.contentHash(testFile), is(HashCode.fromString(TEST_DATA_SHA512_HASH).asBytes()));
	}

	@Test
	public void testCreateMetaDataFromFile() throws Exception {
		FileMetaData meta = cut.createMetaDataFromFile(testFile);

		assertThat(meta, is(new FileMetaData(testFile.toString(), TEST_DATA_TEMPLATE.length, modTime.toMillis(),
				HashCode.fromString(TEST_DATA_SHA512_HASH).asBytes())));
	}
}
