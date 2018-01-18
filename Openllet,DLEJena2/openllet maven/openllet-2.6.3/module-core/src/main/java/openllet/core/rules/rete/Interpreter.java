// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.rete;

import java.util.Collection;
import java.util.Collections;
import openllet.core.rules.PartialBinding;

/**
 * <p>
 * Title: Interpreter
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class Interpreter
{
	public AlphaNetwork _alphaNet;

	public Interpreter(final AlphaNetwork alphaNet)
	{
		super();

		_alphaNet = alphaNet;
	}

	/**
	 * Remove all facts from the interpreter, leaving the rules intact.
	 */
	public void reset()
	{
		for (final AlphaNode alpha : _alphaNet)
			alpha.reset();
	}

	/**
	 * Restore _abox to the given _branch
	 *
	 * @param branch
	 */
	public void restore(final int branch)
	{
		for (final AlphaNode alpha : _alphaNet)
			alpha.unmark();

		for (final AlphaNode alpha : _alphaNet)
			alpha.restore(branch);
	}

	public void run()
	{
		_alphaNet.activateAll();
	}

	public Collection<PartialBinding> getBindings()
	{
		return Collections.emptyList();
	}
}
