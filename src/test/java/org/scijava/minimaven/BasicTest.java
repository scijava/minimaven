/*
 * #%L
 * MiniMaven build system for small Java projects.
 * %%
 * Copyright (C) 2012 - 2015 Board of Regents of the University of
 * Wisconsin-Madison.
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

package org.scijava.minimaven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.scijava.minimaven.TestUtils.assertDependencies;
import static org.scijava.minimaven.TestUtils.createTemporaryDirectory;
import static org.scijava.minimaven.TestUtils.haveNetworkConnection;
import static org.scijava.minimaven.TestUtils.read;
import static org.scijava.minimaven.TestUtils.writeExampleProject;
import static org.scijava.minimaven.TestUtils.writeFile;

import java.io.File;
import java.util.jar.JarFile;

import org.junit.Test;

/**
 * A simple test for MiniMaven.
 * <p>
 * This tests the rudimentary function of MiniMaven.
 * </p>
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
		assertEquals("1.0.0\n", read(new JarFile(blub), "version.txt"));
	}

	@Test
	public void testCopyToImageJApp() throws Exception {
		final MavenProject project = writeExampleProject();
		final File ijDir = createTemporaryDirectory("ImageJ.app-");
		final File jarsDir = new File(ijDir, "jars");
		assertTrue(jarsDir.mkdir());
		final File oldVersion = new File(jarsDir, "blub-0.0.5.jar");
		writeFile(oldVersion, "old");
		assertTrue(oldVersion.exists());

		project.buildAndInstall(ijDir);

		final File blub = new File(jarsDir, "blub-1.0.0.jar");
		assertTrue(blub.exists());
		assertFalse(oldVersion.exists());
	}

	@Test
	public void testExcludeDependencies() throws Exception {
		final MavenProject excludedToo = writeExampleProject(
			"<groupId>test3</groupId>", //
			 "<artifactId>excludedToo</artifactId>",
			"<version>0.0.1</version>");

		final MavenProject excluded = writeExampleProject(excludedToo.env,
			"<groupId>test2</groupId>", //
			"<artifactId>excluded</artifactId>", //
			"<version>0.0.1</version>", //
			"<dependencies>", //
			"<dependency>", //
			"<groupId>test3</groupId>", //
			"<artifactId>excludedToo</artifactId>", //
			"<version>0.0.1</version>", //
			"</dependency>", //
			"</dependencies>");

		final MavenProject dependency = writeExampleProject(excluded.env,
			"<groupId>test</groupId>", //
			"<artifactId>dependency</artifactId>", //
			"<version>1.0.0</version>", //
			"<dependencies>", //
			"<dependency>", //
			"<groupId>test2</groupId>", //
			"<artifactId>excluded</artifactId>", //
			"<version>0.0.1</version>", //
			"</dependency>", //
			"</dependencies>");

		final MavenProject project = writeExampleProject(excluded.env,
			"<groupId>test3</groupId>", //
			"<artifactId>top-level</artifactId>", //
			"<version>1.0.2</version>", //
			"<dependencies>", //
			"<dependency>", //
			"<groupId>test</groupId>", //
			"<artifactId>dependency</artifactId>", //
			"<version>1.0.0</version>", //
			"<exclusions>", //
			"<exclusion>", //
			"<groupId>test2</groupId>", //
			"<artifactId>excluded</artifactId>", //
			"</exclusion>", //
			"</exclusions>", //
			"</dependency>", //
			"</dependencies>");

		assertDependencies(excluded, "test3:excludedToo:0.0.1:jar");
		assertDependencies(dependency, "test2:excluded:0.0.1:jar",
			"test3:excludedToo:0.0.1:jar");
		assertDependencies(project, "test:dependency:1.0.0:jar");
	}

	@Test
	public void testDependencyManagement() throws Exception {
		final MavenProject parent = writeExampleProject( //
			"<groupId>test</groupId>", //
			"<artifactId>parent</artifactId>", //
			"<version>0.0.1</version>", //
			"<packaging>pom</packaging>", //
			"<dependencyManagement>", //
			"<dependencies>", //
			"<groupId>test</groupId>", //
			"<artifactId>dependency</artifactId>", //
			"<version>0.0.3</version>", //
			"<dependency>", //
			"</dependency>", //
			"</dependencies>", //
			"</dependencyManagement>");

		writeExampleProject(parent.env, //
			"<groupId>test</groupId>", //
			"<artifactId>dependency</artifactId>", //
			"<version>0.0.3</version>");

		final MavenProject project = writeExampleProject(parent.env, //
			"<parent>", //
			"<groupId>test</groupId>", //
			"<artifactId>parent</artifactId>", //
			"<version>0.0.1</version>", //
			"</parent>", //
			"<groupId>test</groupId>", //
			"<artifactId>project</artifactId>", //
			"<version>1.0.0</version>", //
			"<dependencies>", //
			"<dependency>", //
			"<groupId>test</groupId>", //
			"<artifactId>dependency</artifactId>", //
			"</dependency>", //
			"</dependencies>");

		assertDependencies(project, "test:dependency:0.0.3:jar");
	}

	@Test
	public void testClassifiers() throws Exception {
		assumeTrue(haveNetworkConnection());

		final String groupId = "com.miglayout";
		final String artifactId = "miglayout";
		final String version = "3.7.3.1";
		final String classifier = "swing";

		final BuildEnvironment env = new BuildEnvironment(null, true, false, false);
		final MavenProject project = writeExampleProject(env,
			"<groupId>test</groupId>", //
			"<artifactId>project</artifactId>", //
			"<version>1.0.0</version>", //
			"<dependencies>", //
			"<dependency>", //
			"<groupId>" + groupId + "</groupId>", //
			"<artifactId>" + artifactId + "</artifactId>", //
			"<version>" + version + "</version>", //
			"</dependency>", //
			"<dependency>", //
			"<groupId>" + groupId + "</groupId>", //
			"<artifactId>" + artifactId + "</artifactId>", //
			"<version>" + version + "</version>", //
			"<classifier>" + classifier + "</classifier>", //
			"</dependency>", //
			"</dependencies>");

		final File ijDir = createTemporaryDirectory("ImageJ.app-");
		project.buildAndInstall(ijDir);

		final File jarsDir = new File(ijDir, "jars");
		final File file = new File(jarsDir, artifactId + "-" + version + ".jar");
		assertTrue(file.exists());
		final File file2 = new File(jarsDir, artifactId + "-" + version + "-" +
			classifier + ".jar");
		assertTrue(file2.exists());
	}

	@Test
	public void testCleanImageJApp() throws Exception {
		final MavenProject project = writeExampleProject();
		final File ijDir = createTemporaryDirectory("ImageJ.app-");
		final File jarsDir = new File(ijDir, "jars");
		assertTrue(jarsDir.mkdir());
		final File oldVersion = new File(jarsDir, "blub-0.0.5.jar");
		writeFile(oldVersion, "old");
		assertTrue(oldVersion.exists());
		final File pluginsDir = new File(ijDir, "plugins");
		assertTrue(pluginsDir.mkdir());
		final File oldVersion2 = new File(pluginsDir, "blub-0.0.jar");
		writeFile(oldVersion2, "old2");
		assertTrue(oldVersion2.exists());
		final File oldVersion3 = new File(pluginsDir, "blub-0.1.jar");
		writeFile(oldVersion3, "old3");
		assertTrue(oldVersion3.exists());
		final File different = new File(pluginsDir, "blub-1.0.0-swing.jar");
		writeFile(different, "different");
		assertTrue(different.exists());

		project.clean(ijDir);

		assertFalse(oldVersion.exists());
		assertFalse(oldVersion2.exists());
		assertFalse(oldVersion3.exists());
		assertTrue(different.exists());
	}
}
