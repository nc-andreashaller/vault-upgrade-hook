package biz.netcentric.vlt.upgrade.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import biz.netcentric.vlt.upgrade.util.ComparableVersion;

@RunWith(Parameterized.class)
public class ComparableVersionTest {

    @Parameters(name = "[{index}] {0} <=> {1} == {2}")
    public static Collection<Object[]> data() {
	return Arrays.asList(new Object[][] { // comments to avoid formatting
		{ "1", "1", 0 }, //
		{ "1", "1-SNAPSHOT", 1 }, //
		{ "2", "1", 1 }, //
		{ "10", "1", 1 }, //
		{ "10", "2", 1 }, //
		{ "10", "2.2", 1 }, //
		{ "1.2", "1", 1 }, //
		{ "1.1", "1.1", 0 }, //
		{ "1.10", "1.1", 1 }, //
		{ "1.10", "1.2", 1 }, //
		{ "1.10", "1.2.2", 1 }, //
		{ "1.1.2", "1.1", 1 }, //
		{ "1.0.9.2", "1.0.9.1", 1 }, //
		{ "1.0.10.1", "1.0.9.3", 1 }, //
		{ "1.10", "1.1.1", 1 } });
    }

    @Parameter(0)
    public String versionA;
    @Parameter(1)
    public String versionB;
    @Parameter(2)
    public int expectedResult;

    @Test
    public void testIt() {
	Assert.assertEquals(expectedResult, new ComparableVersion(versionA).compareTo(new ComparableVersion(versionB)));
    }

}
