/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.duplicate;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.to;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;

import com.github.dozedoff.dedupe.db.table.FileMetaData;

public class HashGroupTest {
	private static final Duration TIMEOUT = new Duration(2, TimeUnit.SECONDS);

	private HashGroup cut;

	private FileMetaData metaA;
	private FileMetaData metaB;
	private FileMetaData metaC;
	private FileMetaData metaD;

	@Before
	public void setUp() throws Exception {
		cut = new HashGroup();

		byte[] hashA = {0};
		byte[] hashB = {1};
		
		metaA = new FileMetaData("A", 0, 0, hashA.clone());
		metaB = new FileMetaData("B", 0, 0, hashB.clone());
		metaC = new FileMetaData("C", 0, 0, hashA.clone());
		metaD = new FileMetaData();

		cut.add(Arrays.asList(metaA, metaB, metaC, metaD).stream());
	}

	@Test
	public void testSameHash() throws Exception {
		FileMetaData[] expected = { metaA, metaC };

		await().atMost(TIMEOUT).untilCall(to(cut).sameHash(), containsInAnyOrder(expected));
	}
}
