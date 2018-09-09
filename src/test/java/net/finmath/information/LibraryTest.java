package net.finmath.information;

import org.junit.Assert;
import org.junit.Test;

public class LibraryTest {

	@Test
	public void testBuildString() {
		String buildString = Library.getBuildString();
		
		System.out.println(buildString);
		Assert.assertNotNull("Build string", buildString);
	}

	@Test
	public void testVersionString() {
		String versionString = Library.getVersionString();
		
		System.out.println(versionString);
		Assert.assertNotNull("Version string", versionString);
	}
}
