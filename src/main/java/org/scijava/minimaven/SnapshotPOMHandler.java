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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * TODO
 * 
 * @author Johannes Schindelin
 */
public class SnapshotPOMHandler extends AbstractPOMHandler {
	protected String snapshotVersion, timestamp, buildNumber;

	private static Pattern versionPattern = Pattern.compile("(.*)-(\\d+\\.\\d+)-(\\d+)");

	@Override
	protected void processCharacters(final StringBuilder sb) throws SAXException {
		if (qName == null)
			return;
		else if (qName.equals("version")) {
			String version = sb.toString().trim();
			if (version.endsWith("-SNAPSHOT")) {
				snapshotVersion = version.substring(0, version.length() - "-SNAPSHOT".length());
			}
			else {
				final Matcher matcher = versionPattern.matcher(version);
				if (!matcher.matches()) {
					throw new SAXException("Unhandled version: " + version);
				}
				snapshotVersion = matcher.group(1);
				timestamp = matcher.group(2);
				buildNumber = matcher.group(3);
			}
		}
		else if (qName.equals("timestamp"))
			timestamp = sb.toString().trim();
		else if (qName.equals("buildNumber"))
			buildNumber = sb.toString().trim();
	}

	public static String parse(File xml) throws IOException, ParserConfigurationException, SAXException {
		try {
			return SnapshotPOMHandler.parse(xml.getAbsolutePath(), new FileInputStream(xml));
		}
		catch (final FileNotFoundException e) {
			throw e;
		}
		catch (final IOException e) {
			throw new IOException("Error parsing " + xml, e);
		}
		catch (final SAXException e) {
			throw new SAXException("Error parsing " + xml, e);
		}
	}

	public static String parse(InputStream in) throws IOException, ParserConfigurationException, SAXException {
		return parse(null, in);
	}

	private static String parse(final String systemId, final InputStream in) throws IOException, ParserConfigurationException, SAXException {
		SnapshotPOMHandler handler = new SnapshotPOMHandler();
		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		reader.setContentHandler(handler);
		final InputSource source = new InputSource(in);
		if (systemId != null) source.setSystemId(systemId);
		reader.parse(source);
		if (handler.snapshotVersion != null && handler.timestamp != null && handler.buildNumber != null)
			return handler.snapshotVersion + "-" + handler.timestamp + "-" + handler.buildNumber;
		throw new IOException("Missing timestamp/build number: " + handler.timestamp + ", " + handler.buildNumber);
	}
}
