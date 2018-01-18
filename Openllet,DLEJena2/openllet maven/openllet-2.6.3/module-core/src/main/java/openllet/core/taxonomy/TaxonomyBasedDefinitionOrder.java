// Copyright (c) 2006 - 2010, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.taxonomy;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.CollectionUtils;

/**
 * @author Evren Sirin
 */
public class TaxonomyBasedDefinitionOrder extends AbstractDefinitionOrder
{
	private TaxonomyImpl<ATermAppl> _definitionOrderTaxonomy;

	public TaxonomyBasedDefinitionOrder(final KnowledgeBase kb, final Comparator<ATerm> comparator)
	{
		super(kb, comparator);
	}

	@Override
	protected void initialize()
	{
		_definitionOrderTaxonomy = new TaxonomyImpl<>(_kb.getClasses(), ATermUtils.TOP, ATermUtils.BOTTOM);
	}

	@Override
	protected void addUses(final ATermAppl c, final ATermAppl d)
	{
		if (_definitionOrderTaxonomy.isEquivalent(c, d).isTrue())
			return;

		final TaxonomyNode<ATermAppl> cNode = _definitionOrderTaxonomy.getNode(c);
		final TaxonomyNode<ATermAppl> dNode = _definitionOrderTaxonomy.getNode(d);
		if (cNode == null)
			throw new InternalReasonerException(c + " is not in the definition _order");
		else
			if (cNode.equals(_definitionOrderTaxonomy.getTop()))
				_definitionOrderTaxonomy.merge(cNode, dNode);
			else
			{
				_definitionOrderTaxonomy.addSuper(c, d);
				_definitionOrderTaxonomy.removeCycles(cNode);
			}
	}

	@Override
	protected Set<ATermAppl> computeCycles()
	{
		final Set<ATermAppl> cyclicConcepts = CollectionUtils.makeIdentitySet();
		for (final TaxonomyNode<ATermAppl> node : _definitionOrderTaxonomy.getNodes().values())
		{
			final Set<ATermAppl> names = node.getEquivalents();
			if (names.size() > 1)
				cyclicConcepts.addAll(names);
		}

		return cyclicConcepts;
	}

	@Override
	protected List<ATermAppl> computeDefinitionOrder()
	{
		_definitionOrderTaxonomy.assertValid();

		return _definitionOrderTaxonomy.topologocialSort(true, _comparator);
	}
}
