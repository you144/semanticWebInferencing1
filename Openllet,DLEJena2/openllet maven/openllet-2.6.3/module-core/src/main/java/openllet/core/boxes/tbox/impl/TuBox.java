// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC.
// <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms
// of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under
// the terms of the MIT License.
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

package openllet.core.boxes.tbox.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.OpenlletOptions;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.CollectionUtils;
import openllet.core.utils.IdentityHashSet;

public class TuBox extends TBoxBase
{
	private Map<ATermAppl, List<Unfolding>> _unfoldingMap;
	private Collection<ATermAppl> _termsToNormalize = null;

	/*
	 * Constructors
	 */

	public TuBox(final TBoxExpImpl tbox)
	{
		super(tbox);
	}

	@Override
	public boolean addDef(final ATermAppl axiom)
	{
		boolean added = false;

		final ATermAppl name = (ATermAppl) axiom.getArgument(0);
		TermDefinition td = getTD(name);
		if (td == null)
		{
			td = new TermDefinition();
			_termhash.put(name, td);
		}

		added = td.addDef(axiom);

		if (added && _termsToNormalize != null)
			_termsToNormalize.add(name);

		return added;
	}

	@Override
	public boolean removeDef(final ATermAppl axiom)
	{
		final boolean removed = super.removeDef(axiom);

		if (removed && _termsToNormalize != null)
			_termsToNormalize.add((ATermAppl) axiom.getArgument(0));

		return removed;
	}

	public void updateDef(final ATermAppl axiom)
	{
		final ATermAppl c = (ATermAppl) axiom.getArgument(0);
		if (ATermUtils.isPrimitive(c))
			_termsToNormalize.add(c);
	}

	public List<Unfolding> unfold(final ATermAppl c)
	{
		final List<Unfolding> list = _unfoldingMap.get(c);
		return list != null ? list : Collections.emptyList();
	}

	/**
	 * Normalize all the definitions in the Tu
	 */
	public void normalize()
	{
		if (_termsToNormalize == null)
		{
			_termsToNormalize = _termhash.keySet();
			_unfoldingMap = CollectionUtils.makeIdentityMap();
		}
		_logger.fine(() -> "Normalizing " + _termsToNormalize);

		for (final ATermAppl c : _termsToNormalize)
		{
			final TermDefinition td = getTD(c);
			if (null == td) // FIXME related to bug 453 @see testUnsatisfiable453
			{
				_logger.severe(() -> "While normalizing the TuBox " + c + " has no TermDefinition but was still declare in termsToNormalize");
				continue;
			}

			td.clearDependencies();

			final ATermAppl notC = ATermUtils.makeNot(c);

			final List<Unfolding> unfoldC = new ArrayList<>();

			if (!td.getEqClassAxioms().isEmpty())
			{
				final List<Unfolding> unfoldNotC = new ArrayList<>();

				for (final ATermAppl eqClassAxiom : td.getEqClassAxioms())
				{
					final ATermAppl unfolded = (ATermAppl) eqClassAxiom.getArgument(1);
					final Set<ATermAppl> ds = _tbox.getAxiomExplanation(eqClassAxiom);

					final ATermAppl normalized = ATermUtils.normalize(unfolded);
					final ATermAppl normalizedNot = ATermUtils.negate(normalized);

					unfoldC.add(Unfolding.create(normalized, ds));
					unfoldNotC.add(Unfolding.create(normalizedNot, ds));
				}

				_unfoldingMap.put(notC, unfoldNotC);
			}
			else
				_unfoldingMap.remove(notC);

			for (final ATermAppl subClassAxiom : td.getSubClassAxioms())
			{
				final ATermAppl unfolded = (ATermAppl) subClassAxiom.getArgument(1);
				final Set<ATermAppl> ds = _tbox.getAxiomExplanation(subClassAxiom);

				final ATermAppl normalized = ATermUtils.normalize(unfolded);
				unfoldC.add(Unfolding.create(normalized, ds));
			}

			if (!unfoldC.isEmpty())
				_unfoldingMap.put(c, unfoldC);
			else
				_unfoldingMap.remove(c);
		}

		_termsToNormalize = new HashSet<>();

		if (OpenlletOptions.USE_ROLE_ABSORPTION)
			absorbRanges(_tbox);
	}

	private void absorbRanges(final TBoxExpImpl tbox)
	{
		final List<Unfolding> unfoldTop = _unfoldingMap.get(ATermUtils.TOP);
		if (unfoldTop == null)
			return;

		final List<Unfolding> newUnfoldTop = new ArrayList<>();
		for (final Unfolding unfolding : unfoldTop)
		{
			final ATermAppl unfolded = unfolding.getResult();
			final Set<ATermAppl> explain = unfolding.getExplanation();

			if (ATermUtils.isAllValues(unfolded))
			{
				final ATerm r = unfolded.getArgument(0);
				final ATermAppl range = (ATermAppl) unfolded.getArgument(1);

				_kb.addRange(r, range, explain);

				tbox.getAbsorbedAxioms().addAll(explain);
			}
			else
				if (ATermUtils.isAnd(unfolded))
				{
					ATermList l = (ATermList) unfolded.getArgument(0);
					ATermList newList = ATermUtils.EMPTY_LIST;
					for (; !l.isEmpty(); l = l.getNext())
					{
						final ATermAppl term = (ATermAppl) l.getFirst();
						if (term.getAFun().equals(ATermUtils.ALLFUN))
						{
							final ATerm r = term.getArgument(0);
							final ATermAppl range = (ATermAppl) term.getArgument(1);

							_kb.addRange(r, range, explain);

							tbox.getAbsorbedAxioms().addAll(explain);
						}
						else
							newList = newList.insert(term);
					}

					if (!newList.isEmpty())
						newUnfoldTop.add(Unfolding.create(ATermUtils.makeAnd(newList), explain));
				}
				else
					newUnfoldTop.add(unfolding);
		}

		if (newUnfoldTop.isEmpty())
			_unfoldingMap.remove(ATermUtils.TOP);

	}

	/*
	 * Accessor Methods
	 */

	public boolean addIfUnfoldable(final ATermAppl term)
	{
		final ATermAppl name = (ATermAppl) term.getArgument(0);
		if (!ATermUtils.isPrimitive(name))
			return false;

		TermDefinition td = getTD(name);

		if (td == null)
			td = new TermDefinition();

		// Basic Check
		if (!td.isUnique(term))
			return false;

		// Loop Checks
		final ATermAppl body = (ATermAppl) term.getArgument(1);
		final Set<ATermAppl> nameDependencies = td.getDependencies();
		final Set<ATermAppl> bodyDependencies = ATermUtils.findPrimitives(body);

		if (!nameDependencies.containsAll(bodyDependencies))
		{
			final Set<ATermAppl> seen = new IdentityHashSet<>(); // Identity hashSet because dependencies are manage on identity hash set
			for (final ATermAppl current : bodyDependencies)
				if (findTarget(current, name, seen))
					return false;
		}

		return addDef(term);
	}

	protected boolean findTarget(final ATermAppl term, final ATermAppl target, final Set<ATermAppl> seen)
	{
		final List<ATermAppl> queue = new ArrayList<>();
		queue.add(term);

		while (!queue.isEmpty())
		{
			_kb.getTimers().checkTimer("preprocessing");
			final ATermAppl current = queue.remove(queue.size() - 1);

			if (!seen.add(current))
				continue;

			if (current.equals(target))
				return true;

			final TermDefinition td = getTD(current);
			if (td != null)
			{
				// Shortcut
				if (td.getDependencies().contains(target))
					return true;

				queue.addAll(td.getDependencies());
			}
		}

		return false;
	}

	public void print(final Appendable out)
	{
		try
		{
			out.append("Tu: [\n");
			for (final ATermAppl c : _unfoldingMap.keySet())
			{
				final List<Unfolding> unfoldedList = unfold(c);
				if (!unfoldedList.isEmpty())
				{
					out.append(ATermUtils.toString(c)).append(" -> ");
					for (final Unfolding unf : unfoldedList)
						out.append(ATermUtils.toString(unf.getResult())).append(", ");
					out.append("\n");
				}
			}
			out.append("]\n");
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		print(sb);
		return sb.toString();
	}
}
