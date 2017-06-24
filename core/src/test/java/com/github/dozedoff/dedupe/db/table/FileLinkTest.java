/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.db.table;

import org.junit.Before;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class FileLinkTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testEquals() throws Exception {
		EqualsVerifier.forClass(FileLink.class).allFieldsShouldBeUsedExcept("id").suppress(Warning.NONFINAL_FIELDS)
				.verify();
	}
}
