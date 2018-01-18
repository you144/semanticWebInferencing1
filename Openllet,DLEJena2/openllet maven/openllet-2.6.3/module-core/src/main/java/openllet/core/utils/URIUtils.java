/*
 * Created on May 6, 2004
 */
package openllet.core.utils;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.shared.tools.Log;

/**
 * Utility functions for URI's.
 *
 * @author Evren Sirin
 */
public class URIUtils
{
	public final static Logger _logger = Log.getLogger(URIUtils.class);

	public static String getQName(final String uri)
	{
		return getFilePart(uri) + ":" + getLocalName(uri);
	}

	public static String getFilePart(final String uri)
	{
		try
		{
			final URI u = URI.create(uri);
			final String path = u.getPath();

			//if(uri.length() == 0) return
			final int begin = path.lastIndexOf("/");
			int end = path.lastIndexOf(".");

			if (end == -1 || begin > end)
				end = path.length();

			return path.substring(begin + 1, end);
		}
		catch (final Exception e)
		{
			_logger.log(Level.FINE, "", e);
			return "http://invalid/uri/";
		}
	}

	/**
	 * @return the local name of a URI. This function is not equiavlent to URI.getFragment() because it tries to handle handle slashy URI's such as the ones
	 *         found in Dublin Core. It is equiavalent to getLocalName(uri.toString()).
	 * @param uri
	 */
	public static String getLocalName(final URI uri)
	{
		return getLocalName(uri.toString());
	}

	/**
	 * @return the local name of a URI string. This naive implementation splits the URI from the position of a '#' character or the last occurunce of '/'
	 *         character. If neither of these characters are found, the parameter itself is returned.
	 * @param uri
	 */
	public static String getLocalName(final String uri)
	{
		final int index = splitPos(uri);

		if (index == -1)
			return uri;

		return uri.substring(index + 1);
	}

	public static String getNameSpace(final URI uri)
	{
		return getNameSpace(uri.toString());
	}

	public static String getNameSpace(final String uri)
	{
		final int index = uri.indexOf("#");

		if (index == -1)
			return uri;

		return uri.substring(0, index);
	}

	private static int splitPos(final String uri)
	{
		int pos = uri.indexOf("#");

		if (pos == -1)
			pos = uri.lastIndexOf("/");

		return pos;
	}
}
