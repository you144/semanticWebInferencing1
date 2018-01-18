// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules.model;

/**
 * <p>
 * Title: Atom Object Visitor
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Ron Alford
 */
public interface AtomObjectVisitor
{
	default public void visit(@SuppressWarnings("unused") final AtomDConstant constant)
	{//
	}

	default public void visit(@SuppressWarnings("unused") final AtomDVariable variable)
	{//
	}

	default public void visit(@SuppressWarnings("unused") final AtomIConstant constant)
	{//
	}

	default public void visit(@SuppressWarnings("unused") final AtomIVariable variable)
	{//
	}
}
