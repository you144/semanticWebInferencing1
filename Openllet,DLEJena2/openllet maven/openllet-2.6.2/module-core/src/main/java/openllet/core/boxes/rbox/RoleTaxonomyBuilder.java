// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under the terms of the MIT License.
//
// The MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package openllet.core.boxes.rbox;

import static openllet.core.utils.TermFactory.BOTTOM_DATA_PROPERTY;
import static openllet.core.utils.TermFactory.BOTTOM_OBJECT_PROPERTY;
import static openllet.core.utils.TermFactory.TOP_DATA_PROPERTY;
import static openllet.core.utils.TermFactory.TOP_OBJECT_PROPERTY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATermAppl;
import openllet.atom.OpenError;
import openllet.core.PropertyType;
import openllet.core.taxonomy.Taxonomy;
import openllet.core.taxonomy.TaxonomyImpl;
import openllet.core.taxonomy.TaxonomyNode;
import openllet.core.utils.ATermUtils;
import openllet.shared.tools.Log;

/**
 * @author Evren Sirin
 */
public class RoleTaxonomyBuilder
{
	protected static Logger _logger = Log.getLogger(RoleTaxonomyBuilder.class);

	public static final ATermAppl TOP_ANNOTATION_PROPERTY = ATermUtils.makeTermAppl("_TOP_ANNOTATION_PROPERTY_");
	public static final ATermAppl BOTTOM_ANNOTATION_PROPERTY = ATermUtils.makeTermAppl("_BOTTOM_ANNOTATION_PROPERTY_");

	private static enum Propagate
	{
		UP, DOWN, NONE
	}

	protected Collection<Role> _properties;

	protected Taxonomy<ATermAppl> _taxonomyImpl;
	protected RBox _rbox;
	protected Role _topRole;
	protected Role _bottomRole;
	protected PropertyType _propertyType;
	private int count = 0;

	public RoleTaxonomyBuilder(final RBox rbox, final PropertyType type)
	{
		_rbox = rbox;
		_propertyType = type;
		_properties = rbox.getRoles().values();

		switch (_propertyType)
		{
			case OBJECT:
				_taxonomyImpl = new TaxonomyImpl<>(null, TOP_OBJECT_PROPERTY, BOTTOM_OBJECT_PROPERTY);
				break;
			case DATATYPE:
				_taxonomyImpl = new TaxonomyImpl<>(null, TOP_DATA_PROPERTY, BOTTOM_DATA_PROPERTY);
				break;
			case ANNOTATION:
				_taxonomyImpl = new TaxonomyImpl<>(null, TOP_ANNOTATION_PROPERTY, BOTTOM_ANNOTATION_PROPERTY);
				//Hide the artificial roles TOP_ANNOTATION_PROPERTY and BOTTOM_ANNOTATION_PROPERTY
				_taxonomyImpl.getTop().setHidden(true);
				_taxonomyImpl.getBottomNode().setHidden(true);
				break;
			default:
				throw new AssertionError("Unknown property type: " + _propertyType);
		}

		_topRole = rbox.getRole(_taxonomyImpl.getTop().getName());
		_bottomRole = rbox.getRole(_taxonomyImpl.getBottomNode().getName());
	}

	//	public RoleTaxonomyBuilder(final RBox rbox, final boolean objectRoles)
	//	{
	//		_rbox = rbox;
	//
	//		_properties = rbox.getRoles().values();
	//		_taxonomyImpl = objectRoles ? new TaxonomyImpl<>(null, TOP_OBJECT_PROPERTY, BOTTOM_OBJECT_PROPERTY) : new TaxonomyImpl<>(null, TOP_DATA_PROPERTY, BOTTOM_DATA_PROPERTY);
	//		_topRole = rbox.getRole(_taxonomyImpl.getTop().getName());
	//		_bottomRole = rbox.getRole(_taxonomyImpl.getBottomNode().getName());
	//	}

	public Taxonomy<ATermAppl> classify()
	{
		if (_logger.isLoggable(Level.FINE))
			_logger.fine("Properties: " + _properties.size());

		for (final Role r : _properties)
		{
			if (_propertyType != r.getType())
				continue;

			classify(r);
		}

		return _taxonomyImpl;
	}

	private void classify(final Role c)
	{
		if (_taxonomyImpl.contains(c.getName()))
			return;

		if (_logger.isLoggable(Level.FINER))
			_logger.finer("Property (" + (++count) + ") " + c + "...");

		if (null != _topRole && c.getSubRoles().contains(_topRole))
		{
			_taxonomyImpl.addEquivalentNode(c.getName(), _taxonomyImpl.getTop());
			return;
		}
		else
			if (null != _bottomRole && c.getSuperRoles().contains(_bottomRole))
			{
				_taxonomyImpl.addEquivalentNode(c.getName(), _taxonomyImpl.getBottomNode());
				return;
			}

		Map<TaxonomyNode<ATermAppl>, Boolean> marked = new HashMap<>();
		mark(_taxonomyImpl.getTop(), marked, Boolean.TRUE, Propagate.NONE);
		mark(_taxonomyImpl.getBottomNode(), marked, Boolean.FALSE, Propagate.NONE);

		final Collection<TaxonomyNode<ATermAppl>> superNodes = search(true, c, _taxonomyImpl.getTop(), new HashSet<TaxonomyNode<ATermAppl>>(), new ArrayList<TaxonomyNode<ATermAppl>>(), marked);

		marked = new HashMap<>();
		mark(_taxonomyImpl.getTop(), marked, Boolean.FALSE, Propagate.NONE);
		mark(_taxonomyImpl.getBottomNode(), marked, Boolean.TRUE, Propagate.NONE);

		if (superNodes.size() == 1)
		{
			final TaxonomyNode<ATermAppl> sup = superNodes.iterator().next();

			// if i has only one super class j and j is a subclass
			// of i then it means i = j. There is no need to classify
			// i since we already know everything about j
			if (subsumed(sup, c, marked))
			{
				_logger.finer(() -> ATermUtils.toString(c.getName()) + " = " + ATermUtils.toString(sup.getName()));

				_taxonomyImpl.addEquivalentNode(c.getName(), sup);
				return;
			}
		}

		final Collection<TaxonomyNode<ATermAppl>> subNodes = search(false, c, _taxonomyImpl.getBottomNode(), new HashSet<TaxonomyNode<ATermAppl>>(), new ArrayList<TaxonomyNode<ATermAppl>>(), marked);

		final List<ATermAppl> supers = new ArrayList<>();
		for (final TaxonomyNode<ATermAppl> n : superNodes)
			supers.add(n.getName());

		final List<ATermAppl> subs = new ArrayList<>();
		for (final TaxonomyNode<ATermAppl> n : subNodes)
			subs.add(n.getName());

		_taxonomyImpl.addNode(Collections.singleton(c.getName()), supers, subs, /* hidden = */false);
	}

	private Collection<TaxonomyNode<ATermAppl>> search(final boolean topSearch, final Role c, final TaxonomyNode<ATermAppl> x, final Set<TaxonomyNode<ATermAppl>> visited, final List<TaxonomyNode<ATermAppl>> result, final Map<TaxonomyNode<ATermAppl>, Boolean> marked)
	{
		final List<TaxonomyNode<ATermAppl>> posSucc = new ArrayList<>();
		visited.add(x);

		final Collection<TaxonomyNode<ATermAppl>> list = topSearch ? x.getSubs() : x.getSupers();
		for (final TaxonomyNode<ATermAppl> next : list)
			if (topSearch)
			{
				if (subsumes(next, c, marked))
					posSucc.add(next);
			}
			else
				if (subsumed(next, c, marked))
					posSucc.add(next);

		if (posSucc.isEmpty())
			result.add(x);
		else
			for (final TaxonomyNode<ATermAppl> y : posSucc)
				if (!visited.contains(y))
					search(topSearch, c, y, visited, result, marked);

		return result;
	}

	private boolean subsumes(final TaxonomyNode<ATermAppl> node, final Role c, final Map<TaxonomyNode<ATermAppl>, Boolean> marked)
	{
		final Boolean cached = marked.get(node);
		if (cached != null)
			return cached.booleanValue();

		// check subsumption
		final boolean subsumes = subsumes(_rbox.getRole(node.getName()), c);
		// create an object based on result
		final Boolean value = subsumes ? Boolean.TRUE : Boolean.FALSE;
		// during top search only negative information is propagated down
		final Propagate propagate = subsumes ? Propagate.NONE : Propagate.DOWN;
		// mark the _node appropriately
		mark(node, marked, value, propagate);

		return subsumes;
	}

	private boolean subsumed(final TaxonomyNode<ATermAppl> node, final Role c, final Map<TaxonomyNode<ATermAppl>, Boolean> marked)
	{
		final Boolean cached = marked.get(node);
		if (cached != null)
			return cached.booleanValue();

		// check subsumption
		final boolean subsumed = subsumes(c, _rbox.getRole(node.getName()));
		// create an object based on result
		final Boolean value = subsumed ? Boolean.TRUE : Boolean.FALSE;
		// during bottom search only negative information is propagated down
		final Propagate propagate = subsumed ? Propagate.NONE : Propagate.UP;
		// mark the _node appropriately
		mark(node, marked, value, propagate);

		return subsumed;
	}

	private void mark(final TaxonomyNode<ATermAppl> node, final Map<TaxonomyNode<ATermAppl>, Boolean> marked, final Boolean value, final Propagate propagate)
	{
		final Boolean exists = marked.get(node);
		if (exists != null)
			if (!exists.equals(value))
				throw new OpenError("Inconsistent classification result " + node.getName() + " " + exists + " " + value);
			else
				return;
		marked.put(node, value);

		if (propagate != Propagate.NONE)
		{
			final Collection<TaxonomyNode<ATermAppl>> others = propagate == Propagate.UP ? node.getSupers() : node.getSubs();
			for (final TaxonomyNode<ATermAppl> next : others)
				mark(next, marked, value, propagate);
		}
	}

	private static boolean subsumes(final Role sup, final Role sub)
	{
		final boolean result = sup.isSuperRoleOf(sub);
		ATermUtils.assertTrue(sub.isSubRoleOf(sup) == result);
		return result;
	}

}
