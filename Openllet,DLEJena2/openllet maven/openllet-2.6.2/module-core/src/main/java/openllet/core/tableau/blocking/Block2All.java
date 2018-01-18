// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.blocking;

import java.util.Set;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.boxes.abox.Node;
import openllet.core.boxes.rbox.Role;
import openllet.core.utils.ATermUtils;

/**
 * @author Evren Sirin
 */
public class Block2All implements BlockingCondition
{
	@Override
	public boolean isBlocked(final BlockingContext cxt)
	{
		for (final ATermAppl av : cxt._blocker.getTypes(Node.ALL))
		{
			final ATerm p = av.getArgument(0);
			final ATermAppl c = (ATermAppl) av.getArgument(1);
			final Role s = cxt._blocked.getABox().getRole(p);

			if (p instanceof ATermList)
			{
				final ATermList chain = (ATermList) p;

				if (!isBlockedByChain(cxt, chain, c))
					return false;
			}
			else
				if (s.isDatatypeRole())
					continue;
				else
				{
					if (cxt.isRSuccessor(s.getInverse()) && !cxt._blocked.getParent().hasType(c))
						return false;

					if (!s.isSimple())
					{
						final Set<ATermList> subRoleChains = s.getSubRoleChains();
						for (final ATermList chain : subRoleChains)
							if (!isBlockedByChain(cxt, chain, c))
								return false;
					}
				}
		}

		return true;
	}

	private static boolean isBlockedByChain(final BlockingContext cxt, final ATermList chain, final ATermAppl c)
	{
		final Role firstRole = cxt._blocked.getABox().getRole(chain.getFirst());

		return !cxt.getIncomingRoles().contains(firstRole.getInverse()) && cxt._blocked.getParent().hasType(ATermUtils.makeAllValues(chain.getNext(), c));
	}
}
