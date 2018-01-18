// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet;

import openllet.core.KnowledgeBase;

/**
 * <p>
 * Description: Check the consistency of an ontology
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
public class OpenlletConsistency extends OpenlletCmdApp
{

	public OpenlletConsistency()
	{
	}

	@Override
	public String getAppCmd()
	{
		return "openllet consistency " + getMandatoryOptions() + "[options] <file URI>...";
	}

	@Override
	public String getAppId()
	{
		return "OpenlletConsistency: Check the consistency of an ontology";
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

		if (isConsistent)
			output("Consistent: Yes");
		else
		{
			output("Consistent: No");
			output("Reason: " + kb.getExplanation());
		}
	}

}
