// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.profiler;

import java.util.logging.Logger;
import openllet.core.KnowledgeBase;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.shared.tools.Log;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class ProfileUtils
{
	private static final Logger _logger = Log.getLogger(ProfileUtils.class);

	/**
	 * Prints an error message and terminates the VM.
	 *
	 * @param msg error message
	 */
	public static void error(final String msg)
	{
		System.err.println("ERROR: " + msg);
		System.exit(0);
	}

	public static void error(final String msg, final Throwable t)
	{
		Log.error(_logger, t);
		error(msg);
	}

	/**
	 * Turns the given file path into a more user-friendly format. Strips the file extension and makes sure the formatted string does not exceed the given
	 * length limit.
	 *
	 * @param fileNameParam file name to be formatted
	 * @param length max length of the formatted string
	 * @return formatted name
	 */
	public static String formatFileName(final String fileNameParam, final int length)
	{
		String fileName = fileNameParam;
		final int lastSlash = fileName.lastIndexOf('/') + 1;
		final int lastDot = fileName.lastIndexOf('.');

		if (lastDot < lastSlash)
			fileName = fileName.substring(lastSlash);
		else
			if (lastSlash - lastDot > length)
				fileName = fileName.substring(lastSlash, lastSlash + length);
			else
				fileName = fileName.substring(lastSlash, lastDot);

		return fileName;
	}

	public static double mb(final long bytes)
	{
		return bytes / (1024. * 1024.);
	}

	public static void printCounts(final ABox abox)
	{
		if (abox == null)
		{
			System.out.println("NO ABOX");
			return;
		}

		int typeCount = 0;
		int edgeCount = 0;
		int literalCount = 0;
		for (final Node node : abox.getNodes().values())
			if (node.isLiteral())
				literalCount++;
			else
			{
				edgeCount += ((Individual) node).getOutEdges().size();
				typeCount += node.getTypes().size();
			}
		final int individualCount = abox.getNodes().size() - literalCount;
		System.out.println("Individuals    : " + individualCount);
		System.out.println("Literals       : " + literalCount);
		System.out.println("Types          : " + typeCount);
		System.out.println("Edges          : " + edgeCount);

		System.out.println();
	}

	public static void printCounts(final KnowledgeBase kb)
	{
		final int classCount = kb.getClasses().size();
		final int objPropertyCount = kb.getObjectProperties().size();
		final int dataPropertyCount = kb.getDataProperties().size();

		System.out.println("Expressivity   : " + kb.getExpressivity());
		System.out.println("Classes        : " + classCount);
		System.out.println("Obj Properties : " + objPropertyCount);
		System.out.println("Data Properties: " + dataPropertyCount);

		printCounts(kb.getABox());
	}
}
