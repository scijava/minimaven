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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Abstract base class for POM SAX handlers. Aggregates multi-chunk character
 * streams into a single string.
 * 
 * @author Curtis Rueden
 */
public abstract class AbstractPOMHandler extends DefaultHandler {

	protected String qName;
	private StringBuilder characters = new StringBuilder();
	private boolean gotCharacters;

	@Override
	public void startElement(final String uri, final String localName,
		final String qName, final Attributes attributes)
	{
		// Note: We ignore characters before any opening tag.
		characters.setLength(0);
		gotCharacters = false;
		this.qName = qName;
	}

	@Override
	public void endElement(final String uri, final String localName,
		final String qName) throws SAXException
	{
		if (gotCharacters) {
			processCharacters(characters);
			characters.setLength(0);
			gotCharacters = false;
		}
		this.qName = null;
	}

	@Override
	public void characters(final char[] ch, final int start, final int length)
		throws SAXException
	{
		characters.append(ch, start, length);
		gotCharacters = true;
	}

	protected abstract void processCharacters(final StringBuilder sb)
		throws SAXException;

}
