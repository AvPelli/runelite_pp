package net.runelite.client.pelliplugins;

import lombok.Data;

@Data
class PelliPluginManifest
{
	/** Human-readable display name, e.g. "Pelli Hello World". */
	private String name;
	/** Semver string shown in the config panel. */
	private String version;
	/** Filename of the JAR as stored in cache/p/ and served from /files/. */
	private String fileName;
	/** SHA-256 hex of the JAR (lowercase). Used for cache validation. */
	private String hash;
	/** Byte length of the JAR — used for download progress. */
	private long size;
	/** Fully-qualified class name of the Plugin subclass entry point. */
	private String mainClass;
}
