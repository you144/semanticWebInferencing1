// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.builtins;

import java.net.URI;
import java.net.URISyntaxException;
import openllet.core.exceptions.InternalReasonerException;

/**
 * <p>
 * Title: URI Operators
 * </p>
 * <p>
 * Description: Implementations for each of the SWRL URI operators.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 */
public class URIOperators
{
	private static class ResolveURI implements StringToStringFunction
	{
		@Override
		public String apply(final String... args)
		{
			if (args.length != 2)
				throw new InternalReasonerException("ResolveURI takes two and only two arguments");

			URI relativeURI, baseURI;
			final String relative = args[0];
			final String base = args[1];

			// According to
			// http://www.w3.org/TR/xpath-functions/#func-resolve-uri, if
			// relative is an empty sequence, the empty sequence is returned
			if (relative.length() == 0)
				return relative;

			try
			{
				relativeURI = new URI(relative);

				// If relative is absolute it is returned unchanged
				if (relativeURI.isAbsolute())
					return relative;
			}
			catch (final URISyntaxException e)
			{
				throw new InternalReasonerException("Relative URI reference is not a valid URI", e);
			}

			try
			{
				baseURI = new URI(base);
			}
			catch (final URISyntaxException e)
			{
				throw new InternalReasonerException("Base URI reference is not a valid URI", e);
			}

			try
			{
				final URI ret = new URI(baseURI.toASCIIString() + relativeURI.toASCIIString());

				return ret.toASCIIString();
			}
			catch (final URISyntaxException e)
			{
				throw new InternalReasonerException("Evaluation of base and relative URI is not a URI", e);
			}
		}
	}

	private static class AnyURI implements StringToStringFunction
	{

		@Override
		public String apply(final String... args)
		{
			if (args.length != 6)
				throw new InternalReasonerException("AnyURI wrong number of arguments");

			String schema = args[0];
			String host = args[1];
			String port = args[2];
			String path = args[3];
			String query = args[4];
			String fragment = args[5];

			if (!schema.endsWith(":"))
				schema += ":";

			if (!host.startsWith("//"))
				host = "//" + host;

			if (port.length() > 0 && !port.startsWith(":"))
				port = ":" + port;

			if (path.length() > 0 && !path.startsWith("/"))
				path = "/" + path;

			if (query.length() > 0 && !query.startsWith("?"))
				query = "?" + query;

			if (fragment.length() > 0 && !fragment.startsWith("#"))
				fragment = "#" + fragment;

			try
			{
				return new URI(schema + host + port + path + query + fragment).toASCIIString();
			}
			catch (final URISyntaxException e)
			{
				throw new InternalReasonerException("Returned string is not a URI", e);
			}
		}
	}

	public final static Function resolveURI = new StringFunctionAdapter(new ResolveURI());
	public final static Function anyURI = new StringFunctionAdapter(new AnyURI());

}
