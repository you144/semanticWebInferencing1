// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet;

import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.taxonomy.printer.ClassTreePrinter;
import openllet.core.taxonomy.printer.TaxonomyPrinter;

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
public class OpenlletRealize extends OpenlletCmdApp
{

	public OpenlletRealize()
	{
		super();
	}

	@Override
	public String getAppCmd()
	{
		return "openllet realize " + getMandatoryOptions() + "[options] <file URI>...";
	}

	@Override
	public String getAppId()
	{
		return "OpenlletRealize: Compute and display the most specific instances for each class";
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

		startTask("classification");
		kb.classify();
		finishTask("classification");

		startTask("realization");
		kb.realize();
		finishTask("realization");

		final TaxonomyPrinter<ATermAppl> printer = new ClassTreePrinter();
		printer.print(kb.getTaxonomy());
	}

}
