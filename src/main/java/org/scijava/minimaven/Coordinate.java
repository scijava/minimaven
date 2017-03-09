/*
 * #%L
 * MiniMaven build system for small Java projects.
 * %%
 * Copyright (C) 2011 - 2017 Board of Regents of the University of
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

import java.util.Set;

/**
 * TODO
 *
 * @author Johannes Schindelin
 */
public class Coordinate {

	protected String groupId, artifactId, version, systemPath, classifier, scope,
			snapshotVersion;
	protected boolean optional;

	// entries look like this: <groupId>:<artifactId>
	protected Set<String> exclusions;

	public Coordinate() {}

	public Coordinate(final String groupId, final String artifactId,
		final String version)
	{
		this(groupId, artifactId, version, null, false, null, null, null);
	}

	@Deprecated
	public Coordinate(final String groupId, final String artifactId,
		final String version, final String scope, final boolean optional,
		final String systemPath, final String classifier)
	{
		this(groupId, artifactId, version, scope, optional, systemPath, classifier,
			null);
	}

	protected Coordinate(final String groupId, final String artifactId,
		final String version, final String scope, final boolean optional,
		final String systemPath, final String classifier,
		final Set<String> exclusions)
	{
		this.groupId = normalize(groupId);
		this.artifactId = normalize(artifactId);
		this.version = normalize(version);
		this.scope = normalize(scope);
		this.optional = optional;
		this.systemPath = normalize(systemPath);
		this.classifier = classifier;
		this.exclusions = exclusions;
	}

	public String normalize(final String s) {
		return "".equals(s) ? null : s;
	}

	public String getJarName() {
		return getJarName(false);
	}

	public String getJarName(final boolean withProjectPrefix) {
		return getFileName(withProjectPrefix, true, "jar");
	}

	public String getPOMName() {
		return getPOMName(false);
	}

	public String getPOMName(final boolean withProjectPrefix) {
		return getFileName(withProjectPrefix, false, "pom");
	}

	public String getFileName(final boolean withClassifier,
		final String fileExtension)
	{
		return getFileName(false, withClassifier, fileExtension);
	}

	public String getFileName(final boolean withProjectPrefix,
		final boolean withClassifier, final String fileExtension)
	{
		return (withProjectPrefix ? groupId + "/" : "") + artifactId + "-" +
			getVersion() + (withClassifier && classifier != null ? "-" + classifier
				: "") + (fileExtension != null ? "." + fileExtension : "");
	}

	public String getKey() {
		return groupId + ">" + artifactId + (classifier == null ? "" : ">" +
			classifier);
	}

	public void setSnapshotVersion(final String version) {
		snapshotVersion = version;
	}

	public String getGAV() {
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return snapshotVersion != null ? snapshotVersion : version;
	}

	@Override
	public String toString() {
		String extra = "";
		if (optional) extra += " optional";
		if (scope != null) extra += " scope=" + scope;
		if (extra.startsWith(" ")) extra = "{" + extra.substring(1) + "}";
		return getFileName(true, true, null) + extra;
	}
}
