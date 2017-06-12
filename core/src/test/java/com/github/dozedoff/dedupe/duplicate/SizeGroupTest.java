/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.dedupe.file.MetaData;

@RunWith(MockitoJUnitRunner.class)
public class SizeGroupTest {
	private static final Duration TIMEOUT = new Duration(2, TimeUnit.SECONDS);

	@Mock
	private Path fileA;
	@Mock
	private Path fileB;
	@Mock
	private Path fileC;

	private List<Path> files;

	@Mock
	private MetaData metaData;

	private SizeGroup cut;

	@Before
	public void setUp() throws Exception {
		when(metaData.size(fileA)).thenReturn(1L);
		when(metaData.size(fileB)).thenReturn(2L);
		when(metaData.size(fileC)).thenReturn(1L);

		files = Arrays.asList(fileA, fileB, fileC);
		
		cut = new SizeGroup(metaData);
		cut.add(files.stream());
	}

	@Test
	public void testSameSizeFiles() throws Exception {
		Path[] expected = { fileA, fileC };

		await().atMost(TIMEOUT).untilCall(to(cut).sameSizeFiles(), containsInAnyOrder(expected));
	}

}
