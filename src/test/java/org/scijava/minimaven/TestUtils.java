/*
 * #%L
 * MiniMaven build system for small Java projects.
 * %%
 * Copyright (C) 2012 - 2014 Board of Regents of the University of
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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.scijava.util.ClassUtils;
import org.scijava.util.FileUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TestUtils {

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
	protected static File createTemporaryDirectory(final String prefix) throws IOException {
		final Map.Entry<Class<?>, String> calling = getCallingClass(null);
		return createTemporaryDirectory(prefix, calling.getKey(), calling.getValue());
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
	protected static File createTemporaryDirectory(final String prefix,
		final Class<?> forClass, final String suffix) throws IOException
	{
		final URL directory = ClassUtils.getLocation(forClass);
		if (directory != null && "file".equals(directory.getProtocol())) {
			final String path = directory.getPath();
			if (path != null && path.endsWith("/target/test-classes/")) {
				final File baseDirectory =
					new File(path.substring(0, path.length() - 13));
				final File file = new File(baseDirectory, prefix + suffix);
				if (file.exists()) FileUtils.deleteRecursively(file);
				if (!file.mkdir()) throw new IOException("Could not make directory " + file);
				return file;
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
	protected static Map.Entry<Class<?>, String> getCallingClass(final Class<?> excluding) {
		final String thisClassName = TestUtils.class.getName();
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
			final Class<?> clazz;
			try {
				clazz = loader.loadClass(element.getClassName());
			}
			catch (ClassNotFoundException e) {
				throw new UnsupportedOperationException("Could not load " +
					element.getClassName() + " with the current context class loader (" +
					loader + ")!");
			}
			final String suffix = element.getMethodName() + "-L" + element.getLineNumber();
			return new AbstractMap.SimpleEntry<Class<?>, String>(clazz, suffix);
		}
		throw new UnsupportedOperationException("No calling class outside " + thisClassName + " found!");
	}

	protected static String read(final JarFile jar, final String path)
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

	protected static void writeFile(final File file, final String contents)
			throws IOException {
		final File dir = file.getParentFile();
		if (dir != null && !dir.isDirectory() && !dir.mkdirs())
			throw new IOException("Could not make " + dir);
		final FileWriter writer = new FileWriter(file);
		writer.write(contents);
		writer.close();
	}

	protected static void prettyPrintXML(final String string, final Writer writer)
			throws IOException {
		try {
			final DocumentBuilder builder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			final InputSource in = new InputSource(new StringReader(string));
			final Document document = builder.parse(in);

			final Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			final String XALAN_INDENT_AMOUNT = "{http://xml.apache.org/xslt}"
					+ "indent-amount";
			transformer.setOutputProperty(XALAN_INDENT_AMOUNT, "4");
			StreamResult result = new StreamResult(writer);
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected static void assertDependencies(final MavenProject project, final String... gavs) throws IOException, ParserConfigurationException, SAXException {
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

	protected final static String pomPrefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
	+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
	+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
	+ "http://maven.apache.org/xsd/maven-4.0.0.xsd\">"
	+ "<modelVersion>4.0.0</modelVersion>";

	protected static MavenProject writeExampleProject(String... projectConfiguration) throws IOException {
		return TestUtils.writeExampleProject(null, projectConfiguration);
	}

	protected static MavenProject writeExampleProject(BuildEnvironment env, String... projectConfiguration) throws IOException {
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

		final File tmp = createTemporaryDirectory("minimaven-");
		writeFile(new File(tmp, "src/main/resources/version.txt"),
				"1.0.0\n");

		final File pom = new File(tmp, "pom.xml");
		final Writer out = new FileWriter(pom);
		prettyPrintXML(builder.toString(), out);
		out.close();

		if (env == null) {
			env = new BuildEnvironment(null, false, false, false);
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

	/**
	 * @return whether we have a real network connection at the moment (not just
	 *         localhost)
	 */
	protected static boolean haveNetworkConnection() {
		try {
			final Enumeration<NetworkInterface> ifaces =
				NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				final Enumeration<InetAddress> addresses =
					ifaces.nextElement().getInetAddresses();
				while (addresses.hasMoreElements())
					if (!addresses.nextElement().isLoopbackAddress()) return true;
			}
		}
		catch (final SocketException e) { /* ignore */ }
		return false;
	}

}
