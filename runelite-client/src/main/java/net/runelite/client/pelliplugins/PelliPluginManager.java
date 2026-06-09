package net.runelite.client.pelliplugins;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
@Slf4j
public class PelliPluginManager
{
	private static final String MANIFEST_URL = "https://api.pelliplugins.com/client-api/v1/pelli-plugins.json";
	private static final String FILES_BASE_URL = "https://api.pelliplugins.com/files/";

	private final PluginManager pluginManager;
	private final OkHttpClient okHttpClient;
	private final Gson gson;

	@Inject
	PelliPluginManager(PluginManager pluginManager, OkHttpClient okHttpClient, Gson gson)
	{
		this.pluginManager = pluginManager;
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	/**
	 * Called from RuneLite.java during startup, before startPlugins().
	 * Downloads any new/changed plugin JARs to cache/p/, then loads the
	 * plugin classes into the existing PluginManager so that startPlugins()
	 * picks them up automatically.
	 */
	public void loadPelliPlugins() throws PluginInstantiationException
	{
		RuneLite.PELLI_PLUGIN_CACHE_DIR.mkdirs();

		List<PelliPluginManifest> manifests;
		try
		{
			manifests = fetchManifest();
		}
		catch (IOException e)
		{
			log.warn("Could not fetch Pelli plugin manifest — custom plugins skipped", e);
			return;
		}

		List<Class<?>> classes = new ArrayList<>();
		for (PelliPluginManifest m : manifests)
		{
			try
			{
				File jar = new File(RuneLite.PELLI_PLUGIN_CACHE_DIR, m.getFileName());
				ensureJar(jar, m);
				PelliClassLoader cl = new PelliClassLoader(jar.toURI().toURL());
				classes.add(cl.loadClass(m.getMainClass()));
				log.info("Loaded Pelli plugin '{}' v{}", m.getName(), m.getVersion());
			}
			catch (Exception e)
			{
				log.warn("Failed to load Pelli plugin '{}' — skipping", m.getName(), e);
			}
		}

		if (!classes.isEmpty())
		{
			pluginManager.loadPlugins(classes, null);
		}
	}

	private List<PelliPluginManifest> fetchManifest() throws IOException
	{
		Request req = new Request.Builder().url(MANIFEST_URL).build();
		try (Response resp = okHttpClient.newCall(req).execute())
		{
			if (!resp.isSuccessful())
			{
				throw new IOException("HTTP " + resp.code() + " fetching Pelli manifest");
			}
			Type listType = new TypeToken<List<PelliPluginManifest>>()
			{
			}.getType();
			return gson.fromJson(resp.body().charStream(), listType);
		}
	}

	private void ensureJar(File jar, PelliPluginManifest m) throws IOException
	{
		if (jar.exists() && sha256Hex(jar).equalsIgnoreCase(m.getHash()))
		{
			log.debug("Cache hit for Pelli plugin '{}'", m.getFileName());
			return;
		}

		log.info("Downloading Pelli plugin '{}'", m.getFileName());
		String url = FILES_BASE_URL + m.getFileName();
		Request req = new Request.Builder().url(url).build();
		try (Response resp = okHttpClient.newCall(req).execute())
		{
			if (!resp.isSuccessful())
			{
				throw new IOException("HTTP " + resp.code() + " downloading " + m.getFileName());
			}
			Files.asByteSink(jar).writeFrom(resp.body().byteStream());
		}

		String actual = sha256Hex(jar);
		if (!actual.equalsIgnoreCase(m.getHash()))
		{
			jar.delete();
			throw new IOException("Hash mismatch for " + m.getFileName()
				+ ": expected " + m.getHash() + ", got " + actual);
		}
	}

	private static String sha256Hex(File f) throws IOException
	{
		MessageDigest md;
		try
		{
			md = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
		try (InputStream in = new FileInputStream(f))
		{
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1)
			{
				md.update(buf, 0, n);
			}
		}
		byte[] digest = md.digest();
		StringBuilder sb = new StringBuilder(64);
		for (byte b : digest)
		{
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
