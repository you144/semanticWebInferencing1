// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.completion.rule;

import java.util.List;
import java.util.logging.Level;
import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.abox.Clash;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.tableau.completion.CompletionStrategy;
import openllet.core.tableau.completion.queue.NodeSelector;
import openllet.core.utils.ATermUtils;

/**
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class NominalRule extends AbstractTableauRule
{

	public NominalRule(final CompletionStrategy strategy)
	{
		super(strategy, NodeSelector.NOMINAL, BlockingType.NONE);
	}

	@Override
	public void apply(final Individual y)
	{
		final List<ATermAppl> types = y.getTypes(Node.NOM);
		final int size = types.size();
		for (int j = 0; j < size; j++)
		{
			final ATermAppl nc = types.get(j);
			final DependencySet ds = y.getDepends(nc);

			if (!OpenlletOptions.MAINTAIN_COMPLETION_QUEUE && ds == null)
				continue;

			applyNominalRule(y, nc, ds);

			if (_strategy.getABox().isClosed())
				return;

			if (y.isMerged())
			{
				apply(y.getSame());
				return;
			}
		}
	}

	private void applyNominalRule(final Individual y, final ATermAppl nc, final DependencySet dsParam)
	{
		DependencySet ds = dsParam;
		_strategy.getABox().copyOnWrite();

		final ATermAppl nominal = (ATermAppl) nc.getArgument(0);
		Individual z = _strategy.getABox().getIndividual(nominal); // first find the _individual for the given nominal
		if (z == null)
			if (ATermUtils.isAnonNominal(nominal))
				z = _strategy.getABox().addIndividual(nominal, ds);
			else
				throw new InternalReasonerException("Nominal " + nominal + " not found in KB!");

		// Get the value of mergedTo because of the following possibility:
		// Suppose there are three individuals like this
		// [x,{}],[y,{value(x)}],[z,{value(y)}]
		// After we merge x to y, the _individual x is now represented by
		// the _node y. It is too hard to update all the references of
		// value(x) so here we find the actual representative _node
		// by calling getSame()
		if (z.isMerged())
		{
			ds = ds.union(z.getMergeDependency(true), _strategy.getABox().doExplanation());

			z = z.getSame();
		}

		if (y.isSame(z))
			return;

		if (y.isDifferent(z))
		{
			ds = ds.union(y.getDifferenceDependency(z), _strategy.getABox().doExplanation());
			if (_strategy.getABox().doExplanation())
				_strategy.getABox().setClash(Clash.nominal(y, ds, z.getName()));
			else
				_strategy.getABox().setClash(Clash.nominal(y, ds));
			return;
		}

		if (_logger.isLoggable(Level.FINE))
			_logger.fine("NOM:  " + y + " -> " + z);

		_strategy.mergeTo(y, z, ds);
	}
}
