// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.OpenlletOptions;
import openllet.core.utils.Comparators;
import openllet.core.utils.QNameProvider;
import openllet.core.utils.progress.ProgressMonitor;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 */
public class OpenlletUnsatisfiable extends OpenlletCmdApp
{

	public OpenlletUnsatisfiable()
	{
		super();
	}

	@Override
	public String getAppCmd()
	{
		return "openllet unsatisfiable " + getMandatoryOptions() + "[options] <file URI>...";
	}

	@Override
	public String getAppId()
	{
		return "OpenlletUnsatisfiable: Find the unsatisfiable classes in the ontology";
	}

	@Override
	public OpenlletCmdOptions getOptions()
	{
		final OpenlletCmdOptions options = getGlobalOptions();

		options.add(getLoaderOption());
		options.add(getIgnoreImportsOption());
		options.add(getInputFormatOption());

		return options;
	}

	@Override
	public void run()
	{
		final KnowledgeBase kb = getKB();

		startTask("consistency check");
		final boolean isConsistent = kb.isConsistent();
		finishTask("consistency check");

		if (!isConsistent)
			throw new OpenlletCmdException("Ontology is inconsistent, run \"openllet explain\" to get the reason");

		final QNameProvider qnames = new QNameProvider();
		final Set<String> unsatisfiableClasses = new TreeSet<>(Comparators.stringComparator);

		final ProgressMonitor monitor = OpenlletOptions.USE_CLASSIFICATION_MONITOR.create();
		monitor.setProgressTitle("Finding unsatisfiable");
		monitor.setProgressLength(kb.getClasses().size());

		startTask("find unsatisfiable");
		monitor.taskStarted();

		final Iterator<ATermAppl> i = kb.getClasses().iterator();
		while (i.hasNext())
		{
			monitor.incrementProgress();
			final ATermAppl c = i.next();
			if (!kb.isSatisfiable(c))
				unsatisfiableClasses.add(qnames.shortForm(c.getName()));
		}

		monitor.taskFinished();
		finishTask("find unsatisfiable");

		output("");
		if (unsatisfiableClasses.isEmpty())
			output("Found no unsatisfiable concepts.");
		else
		{
			output("Found " + unsatisfiableClasses.size() + " unsatisfiable concept(s):");

			for (final String c : unsatisfiableClasses)
				output(c);
		}
	}

}
