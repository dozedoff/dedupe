/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe.util;

import java.util.Random;

public class TestDataUtil {
	private static final Random RAND = new Random();

	private TestDataUtil() {
	}

	/**
	 * Create a array with random data.
	 * 
	 * @param length
	 *            of the array to create
	 * @return the initialized array
	 */
	public static byte[] randomData(int length) {
		byte[] data = new byte[length];

		for (int i = 0; i < length; i++) {
			data[i] = (byte) (RAND.nextInt(Byte.MAX_VALUE + 1));
		}

		return data;
	}
}
