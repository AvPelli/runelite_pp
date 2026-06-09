package net.runelite.client.pelliplugins;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLClassLoader;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.util.ReflectUtil;

/**
 * Classloader for Pelli plugin JARs. Mirrors PluginHubClassLoader:
 * delegates net.runelite.* resolution to the framework classloader so that
 * Plugin subclasses loaded here share the same Plugin/EventBus/etc. types,
 * and installs the PrivateLookup helper so Guice can inject private fields.
 */
class PelliClassLoader extends URLClassLoader implements ReflectUtil.PrivateLookupableClassLoader
{
	@Getter
	@Setter
	private MethodHandles.Lookup lookup;

	PelliClassLoader(URL jarUrl)
	{
		super(new URL[]{jarUrl}, PelliClassLoader.class.getClassLoader());
		ReflectUtil.installLookupHelper(this);
	}

	@Override
	public Class<?> defineClass0(String name, byte[] b, int off, int len) throws ClassFormatError
	{
		return defineClass(name, b, off, len);
	}
}
