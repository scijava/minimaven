/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package imagej.build.minimaven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.jar.JarFile;

import org.junit.Test;
import org.scijava.util.FileUtils;

/**
 * A simple test for MiniMaven.
 * 
 * This tests the rudimentary function of MiniMaven.
 * 
 * @author Johannes Schindelin
 */
public class BasicTest {
	@Test
	public void testResources() throws Exception {
		final MavenProject project = TestUtils.writeExampleProject();
		final File tmp = project.directory;
		project.buildJar();

		final File blub = new File(tmp, "target/blub-1.0.0.jar");
		assertTrue(blub.exists());
		assertEquals("1.0.0\n", TestUtils.read(new JarFile(blub), "version.txt"));
		FileUtils.deleteRecursively(tmp);
	}

	@Test
	public void testCopyToImageJApp() throws Exception {
		final MavenProject project = TestUtils.writeExampleProject();
		final File ijDir = TestUtils.createTemporaryDirectory("ImageJ.app-");
		final File jarsDir = new File(ijDir, "jars");
		assertTrue(jarsDir.mkdir());
		final File oldVersion = new File(jarsDir, "blub-0.0.5.jar");
		TestUtils.writeFile(oldVersion, "old");
		assertTrue(oldVersion.exists());

		project.buildAndInstall(ijDir);

		final File blub = new File(jarsDir, "blub-1.0.0.jar");
		assertTrue(blub.exists());
		assertFalse(oldVersion.exists());

		FileUtils.deleteRecursively(project.directory);
		FileUtils.deleteRecursively(ijDir);
	}

	@Test
	public void testExcludeDependencies() throws Exception {
		final MavenProject excluded = TestUtils.writeExampleProject(
				"<groupId>test2</groupId>",
				"<artifactId>excluded</artifactId>",
				"<version>0.0.1</version>");

		final MavenProject dependency = TestUtils.writeExampleProject(excluded.env,
				"<groupId>test</groupId>",
				"<artifactId>dependency</artifactId>",
				"<version>1.0.0</version>",
				"<dependencies>",
				"<dependency>",
				"<groupId>test2</groupId>",
				"<artifactId>excluded</artifactId>",
				"<version>0.0.1</version>",
				"</dependency>",
				"</dependencies>");

		final MavenProject project = TestUtils.writeExampleProject(excluded.env,
				"<groupId>test3</groupId>",
				"<artifactId>top-level</artifactId>",
				"<version>1.0.2</version>",
				"<dependencies>",
				"<dependency>",
				"<groupId>test</groupId>",
				"<artifactId>dependency</artifactId>",
				"<version>1.0.0</version>",
				"<exclusions>",
				"<exclusion>",
				"<groupId>test2</groupId>",
				"<artifactId>excluded</artifactId>",
				"</exclusion>",
				"</exclusions>",
				"</dependency>",
				"</dependencies>");

		TestUtils.assertDependencies(excluded);
		TestUtils.assertDependencies(dependency, "test2:excluded:0.0.1:jar");
		TestUtils.assertDependencies(project, "test:dependency:1.0.0:jar");
	}
}
