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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.scijava.util.ClassUtils;
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
		final File tmp = writeExampleProject();
		final BuildEnvironment env = new BuildEnvironment(null, false,
				false, false);
		final MavenProject project = env.parse(new File(tmp, "pom.xml"));
		project.buildJar();

		final File blub = new File(tmp, "target/blub-1.0.0.jar");
		assertTrue(blub.exists());
		assertEquals("1.0.0\n", read(new JarFile(blub), "version.txt"));
		FileUtils.deleteRecursively(tmp);
	}

	@Test
	public void testCopyToImageJApp() throws Exception {
		final File tmp = writeExampleProject();
		final File ijDir = createTemporaryDirectory("ImageJ.app-");
		final File jarsDir = new File(ijDir, "jars");
		assertTrue(jarsDir.mkdir());
		final File oldVersion = new File(jarsDir, "blub-0.0.5.jar");
		writeFile(oldVersion, "old");
		assertTrue(oldVersion.exists());

		final BuildEnvironment env = new BuildEnvironment(null, false,
				false, false);
		final MavenProject project = env.parse(new File(tmp, "pom.xml"));
		project.buildAndInstall(ijDir);

		final File blub = new File(jarsDir, "blub-1.0.0.jar");
		assertTrue(blub.exists());
		assertFalse(oldVersion.exists());

		FileUtils.deleteRecursively(tmp);
		FileUtils.deleteRecursively(ijDir);
	}

	@Test
	public void testExcludeDependencies() throws Exception {
		final BuildEnvironment env = new BuildEnvironment(null, false, false, false);
		final File directory = createTemporaryDirectory("excludes-test-");

		final String pom = pomPrefix
				+ "\t<groupId>test2</groupId>\n"
				+ "\t<artifactId>excluded</artifactId>\n"
				+ "\t<version>0.0.1</version>\n"
				+ "</project>";
		final InputStream in = new ByteArrayInputStream(pom.getBytes());
		final MavenProject excluded = env.parse(in, directory, null, null);

		final String pom2 = pomPrefix
				+ "\t<groupId>test</groupId>\n"
				+ "\t<artifactId>dependency</artifactId>\n"
				+ "\t<version>1.0.0</version>\n"
				+ "\t<dependencies>\n"
				+ "\t\t<dependency>\n"
				+ "\t\t\t<groupId>test2</groupId>\n"
				+ "\t\t\t<artifactId>excluded</artifactId>\n"
				+ "\t\t\t<version>0.0.1</version>\n"
				+ "\t\t</dependency>\n"
				+ "\t</dependencies>\n"
				+ "</project>";
		final InputStream in2 = new ByteArrayInputStream(pom2.getBytes());
		final MavenProject dependency = env.parse(in2, directory, null, null);

		final String pom3 = pomPrefix
				+ "\t<groupId>test3</groupId>\n"
				+ "\t<artifactId>top-level</artifactId>\n"
				+ "\t<version>1.0.2</version>\n"
				+ "\t<dependencies>\n"
				+ "\t\t<dependency>\n"
				+ "\t\t\t<groupId>test</groupId>\n"
				+ "\t\t\t<artifactId>dependency</artifactId>\n"
				+ "\t\t\t<version>1.0.0</version>\n"
				+ "\t\t\t<exclusions>\n"
				+ "\t\t\t\t<exclusion>\n"
				+ "\t\t\t\t\t<groupId>test2</groupId>\n"
				+ "\t\t\t\t\t<artifactId>excluded</artifactId>\n"
				+ "\t\t\t\t</exclusion>\n"
				+ "\t\t\t</exclusions>\n"
				+ "\t\t</dependency>\n"
				+ "\t</dependencies>\n"
				+ "</project>";
		final InputStream in3 = new ByteArrayInputStream(pom3.getBytes());
		final MavenProject project = env.parse(in3, directory, null, null);

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

	private final static String pomPrefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
			+ "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
			+ "\txsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
			+ "\t\thttp://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
			+ "\t<modelVersion>4.0.0</modelVersion>\n";

	private File writeExampleProject() throws IOException {
		final File tmp = createTemporaryDirectory("minimaven-");
		writeFile(new File(tmp, "src/main/resources/version.txt"),
				"1.0.0\n");
		writeFile(
				new File(tmp, "pom.xml"), pomPrefix
						+ "\t<groupId>test</groupId>\n"
						+ "\t<artifactId>blub</artifactId>\n"
						+ "\t<version>1.0.0</version>\n" + "</project>");
		return tmp;
	}

	private static void writeFile(final File file, final String contents)
			throws IOException {
		final File dir = file.getParentFile();
		if (dir != null && !dir.isDirectory() && !dir.mkdirs())
			throw new IOException("Could not make " + dir);
		final FileWriter writer = new FileWriter(file);
		writer.write(contents);
		writer.close();
	}

	private static String read(final JarFile jar, final String path)
			throws IOException {
		final ZipEntry entry = jar.getEntry(path);
		final InputStream in = jar.getInputStream(entry);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				in));
		final StringBuilder builder = new StringBuilder();
		for (;;) {
			final String line = reader.readLine();
			if (line == null)
				break;
			builder.append(line).append("\n");
		}
		reader.close();
		return builder.toString();
	}

	/**
	 * Makes a temporary directory for use with unit tests.
	 * <p>
	 * When the unit test runs in a Maven context, the temporary directory will be
	 * created in the <i>target/</i> directory corresponding to the calling class
	 * instead of <i>/tmp/</i>.
	 * </p>
	 * 
	 * @param prefix the prefix for the directory's name
	 * @return the reference to the newly-created temporary directory
	 * @throws IOException
	 */
	private static File createTemporaryDirectory(final String prefix) throws IOException {
		return createTemporaryDirectory(prefix, getCallingClass(null));
	}

	/**
	 * Makes a temporary directory for use with unit tests.
	 * <p>
	 * When the unit test runs in a Maven context, the temporary directory will be
	 * created in the corresponding <i>target/</i> directory instead of
	 * <i>/tmp/</i>.
	 * </p>
	 * 
	 * @param prefix the prefix for the directory's name
	 * @param forClass the class for context (to determine whether there's a
	 *          <i>target/<i> directory)
	 * @return the reference to the newly-created temporary directory
	 * @throws IOException
	 */
	private static File createTemporaryDirectory(final String prefix,
		final Class<?> forClass) throws IOException
	{
		final URL directory = ClassUtils.getLocation(forClass);
		if (directory != null && "file".equals(directory.getProtocol())) {
			final String path = directory.getPath();
			if (path != null && path.endsWith("/target/test-classes/")) {
				final File baseDirectory =
					new File(path.substring(0, path.length() - 13));
				final File file = File.createTempFile(prefix, "", baseDirectory);
				if (file.delete() && file.mkdir()) return file;
			}
		}
		return FileUtils.createTemporaryDirectory(prefix, "");
	}

	/**
	 * Returns the class of the caller (excluding the specified class).
	 * <p>
	 * Sometimes it is convenient to determine the caller's context, e.g. to
	 * determine whether running in a maven-surefire-plugin context (in which case
	 * the location of the caller's class would end in
	 * <i>target/test-classes/</i>).
	 * </p>
	 * 
	 * @param excluding the class to exclude (or null)
	 * @return the class of the caller
	 */
	public static Class<?> getCallingClass(final Class<?> excluding) {
		final String thisClassName = BasicTest.class.getName();
		final String thisClassName2 = excluding == null ? null : excluding.getName();
		final Thread currentThread = Thread.currentThread();
		for (final StackTraceElement element : currentThread.getStackTrace()) {
			final String thatClassName = element.getClassName();
			if (thatClassName == null || thatClassName.equals(thisClassName) ||
				thatClassName.equals(thisClassName2) ||
				thatClassName.startsWith("java.lang.")) {
				continue;
			}
			final ClassLoader loader = currentThread.getContextClassLoader();
			try {
				return loader.loadClass(element.getClassName());
			}
			catch (ClassNotFoundException e) {
				throw new UnsupportedOperationException("Could not load " +
					element.getClassName() + " with the current context class loader (" +
					loader + ")!");
			}
		}
		throw new UnsupportedOperationException("No calling class outside " + thisClassName + " found!");
	}

}
