// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under the terms of the MIT License.
//
// The MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package openllet.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import openllet.atom.OpenError;

public class FileUtils
{
	public static boolean exists(final String file)
	{
		return new File(file).exists();
	}

	public static String readURL(final URL fileURL) throws IOException
	{
		return readAll(new InputStreamReader(fileURL.openStream()));
	}

	public static String readFile(final File file) throws FileNotFoundException, IOException
	{
		try (final FileReader reader = new FileReader(file))
		{
			return readAll(reader);
		}
	}

	public static String readFile(final String fileName) throws FileNotFoundException, IOException
	{
		try (FileReader reader = new FileReader(fileName))
		{
			return readAll(reader);
		}
	}

	public static String readAll(final Reader reader) throws IOException
	{
		final StringBuffer buffer = new StringBuffer();

		try (final BufferedReader in = new BufferedReader(reader))
		{
			int ch;
			while ((ch = in.read()) > -1)
				buffer.append((char) ch);
		}

		return buffer.toString();
	}

	public static String toURI(final String fileName)
	{
		try
		{
			final URI uri = URI.create(fileName);
			if (uri.isAbsolute())
				return uri.toString();
			else
				return new File(fileName).toURI().toString();
		}
		catch (final IllegalArgumentException e1) // If the given string violates RFC 2396
		{
			final File localFile = new File(fileName);
			if (!localFile.exists())
				throw new OpenError(new FileNotFoundException(localFile.getAbsolutePath()));
			try
			{
				return localFile.toURI().toURL().toString();
			}
			catch (final MalformedURLException e2)
			{
				throw new OpenError(e1.getMessage() + " " + e2.getMessage(), new FileNotFoundException(localFile.getAbsolutePath()));

			}
		}
	}

	/**
	 * <p>
	 * Creates a collection of URIs from a given regex list. The given list can contain either absolute (local or remote) URIs or a Java regex expression for a
	 * local path. If a regex is given all the files whose name matches the regex will be added to the resulting list.
	 * </p>
	 * <p>
	 * The regular expressions supported by this function are Java regular expressions. If we want to get the URIS for all the files in a directory we need to
	 * pass <code>/path/to/dir/.*</code>
	 * </p>
	 *
	 * @param fileNameRegexList list of regular expressions for fiel URIs
	 * @return list of file URIs matching the given regular expressions
	 */
	public static Collection<String> getFileURIsFromRegex(final String... fileNameRegexList)
	{
		final Collection<String> uris = new ArrayList<>();

		for (final String fileNameRegex : fileNameRegexList)
		{
			final File file = new File(fileNameRegex);
			final File dir = file.getParentFile();

			if (dir != null && dir.exists())
			{
				final String filter = file.getName();

				final File[] files = dir.listFiles((FilenameFilter) (dir1, name) -> dir1 != null && name.matches(filter));

				if (files.length == 0)
					throw new OpenError("File not found: " + fileNameRegex);

				Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);

				for (final File f : files)
					uris.add(f.toURI().toString());
			}
			else
				if (file.exists())
					uris.add(file.toURI().toString());
				else
					if (URI.create(fileNameRegex) == null)
						throw new OpenError(new FileNotFoundException(fileNameRegex));
					else
						uris.add(fileNameRegex);
		}

		return uris;
	}

	/**
	 * Creates a collection of URIs from a given list of files.
	 *
	 * @param fileNameList the list of files
	 * @return a list of URIs
	 */
	public static Collection<String> getFileURIs(final String... fileNameList)
	{
		final Collection<String> uris = new ArrayList<>();

		for (final String fileName : fileNameList)
			uris.add(getFileURI(fileName));

		return uris;
	}

	/**
	 * Creates a URI from a given file name.
	 *
	 * @param fileName the file
	 * @return a string representing the file URI
	 */
	public static String getFileURI(final String fileName)
	{
		final File file = new File(fileName);
		final File dir = file.getParentFile();

		if (file.exists())
			return file.toURI().toString();
		else
			if (dir != null && dir.exists())
				throw new OpenError(new FileNotFoundException(fileName));
			else
				if (URI.create(fileName) == null)
					throw new OpenError(new FileNotFoundException(fileName));
				else
					return fileName;
	}
}
