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
	private static final byte[] TEST_DATA_TEMPLATE2 = "Lorem ipsum"
			.getBytes(StandardCharsets.US_ASCII);
	private static final String TEST_DATA_SHA512_HASH = "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6";
	private static final String TEST_DATA_SHA512_HASH2 = "e2e9cdde07e34612b5a6a81aa41e065fbc8ba5c6dbfd637314b9f2349263dc4a3037bad914f766075e423b5061538adc9650ca25a318c323d9bef4c8940498a4";

	private MetaData cut;

	private byte[] data;
	private byte[] data2;

	private FileSystem fs;
	private Path testFile;
	private FileTime modTime;

	private FileMetaData meta;

	@Before
	public void setUp() throws Exception {


		fs = Jimfs.newFileSystem();

		cut = new MetaData(fs);

		data = TEST_DATA_TEMPLATE.clone();
		data2 = TEST_DATA_TEMPLATE2.clone();

		modTime = FileTime.from(1, TimeUnit.SECONDS);

		createTestFile();

		meta = cut.createMetaDataFromFile(testFile);
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
		assertThat(meta, is(new FileMetaData(testFile.toString(), TEST_DATA_TEMPLATE.length, modTime.toMillis(),
				HashCode.fromString(TEST_DATA_SHA512_HASH).asBytes())));
	}

	@Test
	public void testIsRegularFile() throws Exception {
		assertThat(Files.isRegularFile(testFile), is(true));
	}

	@Test
	public void testUpdateMetaDataSize() throws Exception {
		Files.write(testFile, data2);

		cut.updateMetaData(meta);

		assertThat(meta.getSize(), is(Long.valueOf(TEST_DATA_TEMPLATE2.length)));

	}

	@Test
	public void testUpdateMetaDataModifiedTime() throws Exception {
		modTime = FileTime.from(2, TimeUnit.SECONDS);

		Files.setLastModifiedTime(testFile, modTime);

		cut.updateMetaData(meta);

		assertThat(meta.getModifiedTime(), is(modTime.toMillis()));

	}

	@Test
	public void testUpdateMetaDataHash() throws Exception {
		Files.write(testFile, data2);

		cut.updateMetaData(meta);

		assertThat(meta.getHash(), is(HashCode.fromString(TEST_DATA_SHA512_HASH2).asBytes()));
	}
}
