// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.cache;

import openllet.aterm.ATermAppl;
import openllet.core.boxes.abox.Individual;
import openllet.core.expressivity.Expressivity;

/**
 * A singleton implementation of CacheSafety that always says it is safe to reuse cached results.
 *
 * @author Evren Sirin
 */
public class CacheSafetyAlwaysSafe implements CacheSafety
{
	private static CacheSafetyAlwaysSafe INSTANCE = new CacheSafetyAlwaysSafe();

	public static CacheSafetyAlwaysSafe getInstance()
	{
		return INSTANCE;
	}

	private CacheSafetyAlwaysSafe()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSafe(final ATermAppl c, final Individual ind)
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean canSupport(final Expressivity expressivity)
	{
		return expressivity.hasNominal() && expressivity.hasInverse();
	}
}
