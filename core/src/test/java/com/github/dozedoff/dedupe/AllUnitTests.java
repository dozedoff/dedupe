/* The MIT License (MIT)
 * Copyright (c) 2017 Nicholas Wright
 * http://opensource.org/licenses/MIT
 */
package com.github.dozedoff.dedupe;

import org.junit.runner.RunWith;

import com.googlecode.junittoolbox.ParallelSuite;
import com.googlecode.junittoolbox.SuiteClasses;

@RunWith(ParallelSuite.class)
@SuiteClasses({ "**/*Test.class", "!**/*IT.class", "!**/learning*" })
public class AllUnitTests {

}
