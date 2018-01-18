// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.el;

import java.util.Set;
import openllet.aterm.AFun;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.SetUtils;
import openllet.core.utils.iterator.MultiListIterator;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Harris Lin
 */
public class ELSyntaxUtils
{
	/**
	 * Checks is this concept is a valid EL concept expression. Only valid EL constructs are <code>and</code> and <code>some</code> (although EL++ allows
	 * limited use of nominals Pellet's specialized EL classifier currently does not support nominals so we treat them as non-EL).
	 *
	 * @param concept
	 * @return true if the the term is EL
	 */
	public static boolean isEL(final ATermAppl concept)
	{
		final AFun fun = concept.getAFun();

		boolean isEL = false;
		if (ATermUtils.isPrimitive(concept) || ATermUtils.isBottom(concept))
			isEL = true;
		else
			if (fun.equals(ATermUtils.ANDFUN))
			{
				ATermList listEL = (ATermList) concept.getArgument(0);

				while (!listEL.isEmpty())
				{
					if (!isEL((ATermAppl) listEL.getFirst()))
						break;
					listEL = listEL.getNext();
				}
				isEL = listEL.isEmpty();
			}
			else
				if (fun.equals(ATermUtils.SOMEFUN))
				{
					final ATermAppl p = (ATermAppl) concept.getArgument(0);
					final ATermAppl q = (ATermAppl) concept.getArgument(1);
					isEL = ATermUtils.isPrimitive(p) && isEL(q);
				}
				else
					isEL = false;

		return isEL;
	}

	/**
	 * Simplifies an EL class expression. Simplification flattens <code>and</code> constructs and propagates <code>owl:Nothing</code> (Concept
	 * <code>p some owl:Nothing</code> and any <code>and</code> construct with <code>owl:Nothing</code> in it is simplified to <code>owl:Nothing</code>.
	 *
	 * @param elConcept an EL class expression
	 * @return a simplified form of the given class expression
	 * @throws InternalReasonerException if the concept is not an LE class expression
	 */
	public static ATermAppl simplify(final ATermAppl elConcept) throws InternalReasonerException
	{
		ATermAppl simp = elConcept;
		final AFun fun = elConcept.getAFun();

		if (fun.equals(ATermUtils.ANDFUN))
		{
			final ATermList conjuncts = (ATermList) elConcept.getArgument(0);
			final Set<ATermAppl> set = SetUtils.create();
			for (final MultiListIterator i = new MultiListIterator(conjuncts); i.hasNext();)
			{
				final ATermAppl c = i.next();
				if (ATermUtils.isAnd(c))
					i.append((ATermList) c.getArgument(0));
				else
					if (c.equals(ATermUtils.BOTTOM))
						return ATermUtils.BOTTOM;
					else
						if (!c.equals(ATermUtils.TOP))
							set.add(c);
			}

			if (set.size() > 1)
				simp = ATermUtils.makeAnd(ATermUtils.toSet(set));
			else
				if (set.size() == 1)
					simp = set.iterator().next();
		}
		else
			if (fun.equals(ATermUtils.SOMEFUN))
			{
				final ATerm p = elConcept.getArgument(0);
				final ATermAppl q = (ATermAppl) elConcept.getArgument(1); // complex
				// role?
				final ATermAppl qSimp = simplify(q);
				if (qSimp.equals(ATermUtils.BOTTOM))
					simp = ATermUtils.BOTTOM;
				else
					simp = ATermUtils.makeSomeValues(p, qSimp);
			}
			else
				if (!ATermUtils.isPrimitive(elConcept) && !ATermUtils.isBottom(elConcept) && !ATermUtils.isTop(elConcept))
					throw new InternalReasonerException("Concept " + elConcept + " is not an EL concept");

		return simp;
	}
}
