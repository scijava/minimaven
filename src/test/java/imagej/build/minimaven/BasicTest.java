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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.scijava.util.FileUtils;
import org.xml.sax.SAXException;

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
		final MavenProject project = writeExampleProject();
		final File tmp = project.directory;
		project.buildJar();

		final File blub = new File(tmp, "target/blub-1.0.0.jar");
		assertTrue(blub.exists());
		assertEquals("1.0.0\n", TestUtils.read(new JarFile(blub), "version.txt"));
		FileUtils.deleteRecursively(tmp);
	}

	@Test
	public void testCopyToImageJApp() throws Exception {
		final MavenProject project = writeExampleProject();
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
		final MavenProject excluded = writeExampleProject(
				"<groupId>test2</groupId>",
				"<artifactId>excluded</artifactId>",
				"<version>0.0.1</version>");

		final MavenProject dependency = writeExampleProject(excluded.env,
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

		final MavenProject project = writeExampleProject(excluded.env,
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

		assertDependencies(excluded);
		assertDependencies(dependency, "test2:excluded:0.0.1:jar");
		assertDependencies(project, "test:dependency:1.0.0:jar");
	}

	private void assertDependencies(final MavenProject project, final String... gavs) throws IOException, ParserConfigurationException, SAXException {
		final Set<String> haystack = new HashSet<String>();
		for (final String gav : gavs) {
			haystack.add(gav);
		}
		for (final MavenProject dependency : project.getDependencies(true, false, "test")) {
			final String gav = dependency.getGAV();
			assertTrue("Unexpected dependency: " + gav, haystack.contains(gav));
			haystack.remove(gav);
		}
		assertTrue("Missing: " + haystack.toString(), haystack.isEmpty());
	}

	private final static String pomPrefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
			+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
			+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
			+ "http://maven.apache.org/xsd/maven-4.0.0.xsd\">"
			+ "<modelVersion>4.0.0</modelVersion>";

	private MavenProject writeExampleProject(String... projectConfiguration) throws IOException {
		return writeExampleProject(null, projectConfiguration);
	}

	private MavenProject writeExampleProject(BuildEnvironment env, String... projectConfiguration) throws IOException {
		if (projectConfiguration == null || projectConfiguration.length == 0) {
			projectConfiguration = new String[] {
					"<groupId>test</groupId>",
					"<artifactId>blub</artifactId>",
					"<version>1.0.0</version>"
			};
		}

		final StringBuilder builder = new StringBuilder();
		builder.append(pomPrefix);
		for (final String line : projectConfiguration) {
			builder.append(line);
		}
		builder.append("</project>");

		final File tmp = TestUtils.createTemporaryDirectory("minimaven-");
		TestUtils.writeFile(new File(tmp, "src/main/resources/version.txt"),
				"1.0.0\n");

		final File pom = new File(tmp, "pom.xml");
		final Writer out = new FileWriter(pom);
		TestUtils.prettyPrintXML(builder.toString(), out);
		out.close();

		if (env == null) {
			env = new BuildEnvironment(null, false,	false, false);
		}
		try {
			return env.parse(pom);
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
