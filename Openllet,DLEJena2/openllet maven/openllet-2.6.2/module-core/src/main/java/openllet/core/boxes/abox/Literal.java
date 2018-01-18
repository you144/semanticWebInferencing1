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
// rights to use, copy, modify, _merge, publish, distribute, sublicense, and/or
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

package openllet.core.boxes.abox;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.OpenlletOptions;
import openllet.core.datatypes.DatatypeReasoner;
import openllet.core.datatypes.OWLRealUtils;
import openllet.core.datatypes.exceptions.DatatypeReasonerException;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.datatypes.exceptions.UnrecognizedDatatypeException;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.utils.ATermUtils;

/**
 * @author Evren Sirin
 */
public class Literal extends Node
{
	private final ATermAppl _atermValue;

	private final Object _value;

	private final boolean _hasValue;

	private volatile NodeMerge _merge;

	private volatile boolean _clashed = false;

	public Literal(final ATermAppl name, final ATermAppl term, final ABox abox, final DependencySet ds)
	{
		super(name, abox);

		if (term != null)
		{
			_hasValue = !term.getArgument(ATermUtils.LIT_URI_INDEX).equals(ATermUtils.NO_DATATYPE);
			if (_hasValue)
			{
				Object value = null;
				try
				{
					value = abox.getDatatypeReasoner().getValue(term);
				}
				catch (final InvalidLiteralException e)
				{
					final String msg = format("Attempt to create literal from invalid literal (%s): %s", term, e.getMessage());
					if (OpenlletOptions.INVALID_LITERAL_AS_INCONSISTENCY)
						_logger.fine(msg);
					else
					{
						_logger.severe(msg);
						throw new InternalReasonerException(msg, e);
					}
				}
				catch (final UnrecognizedDatatypeException e)
				{
					final String msg = format("Attempt to create literal from with unrecognized datatype (%s): %s", term, e.getMessage());
					_logger.severe(msg);
					throw new InternalReasonerException(msg, e);
				}

				_value = value;

				if (_value == null)
					_depends.put(name, ds);
			}
			else
				_value = null;

			_atermValue = ATermUtils.makeValue(term);
		}
		else
		{
			_value = null;
			_atermValue = null;
			_hasValue = false;
		}
	}

	public Literal(final Literal literal, final ABoxImpl abox)
	{
		super(literal, abox);

		_atermValue = literal._atermValue;
		_value = literal._value;
		_hasValue = literal._hasValue;
	}

	@Override
	public DependencySet getNodeDepends()
	{
		return getDepends(ATermUtils.TOP_LIT);
	}

	@Override
	public Node copyTo(final ABoxImpl abox)
	{
		return new Literal(this, abox);
	}

	@Override
	final public boolean isLeaf()
	{
		return true;
	}

	@Override
	public int getNominalLevel()
	{
		return isNominal() ? NOMINAL : BLOCKABLE;
	}

	@Override
	public boolean isNominal()
	{
		return _value != null;
	}

	@Override
	public boolean isBlockable()
	{
		return _value == null;
	}

	@Override
	public boolean isLiteral()
	{
		return true;
	}

	@Override
	public boolean isIndividual()
	{
		return false;
	}

	@Override
	public boolean isDifferent(final Node node)
	{
		if (super.isDifferent(node))
			return true;

		final Literal literal = (Literal) node;
		if (_hasValue && literal._hasValue)
		{
			final Class<? extends Object> thisvalueClass = _value.getClass();
			final Class<? extends Object> thatValueClass = literal._value.getClass();

			// XXX due to simplification of numeric types in openllet a special case is needed to compare values of numeric literals
			if (isAcceptableNumber(thisvalueClass) && isAcceptableNumber(thatValueClass))
				return OWLRealUtils.compare((Number) _value, (Number) literal._value) != 0;
			else
				return thisvalueClass.equals(thatValueClass) && !_value.equals(literal._value);
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private static boolean isAcceptableNumber(final Class<? extends Object> cls)
	{
		return OWLRealUtils.acceptable((Class<? extends Number>) cls);
	}

	@Override
	public boolean hasType(final ATerm typeParam)
	{
		ATerm type = typeParam;

		if (type instanceof ATermAppl)
		{
			final ATermAppl a = (ATermAppl) type;
			if (ATermUtils.isNominal(a))
				try
				{
					final ATermAppl input = (ATermAppl) a.getArgument(0);
					final ATermAppl canonical = _abox.getDatatypeReasoner().getCanonicalRepresentation(input);
					if (!canonical.equals(input))
						type = ATermUtils.makeValue(canonical);
				}
				catch (final InvalidLiteralException e)
				{
					_logger.warning(format("hasType called with nominal using invalid literal ('%s'), returning false", e.getMessage()));
					return false;
				}
				catch (final UnrecognizedDatatypeException e)
				{
					_logger.warning(format("hasType called with nominal using literal with unrecognized datatype ('%s'), returning false", e.getMessage()));
					return false;
				}
		}

		if (super.hasType(type))
			return true;
		else
			if (_hasValue)
				if (_atermValue.equals(type))
					return true;

		return false;
	}

	@Override
	public DependencySet getDifferenceDependency(final Node node)
	{
		DependencySet ds = null;
		if (isDifferent(node))
		{
			ds = _differents.get(node);
			if (ds == null)
				ds = DependencySet.INDEPENDENT;
		}

		return ds;
	}

	@Override
	public void addType(final ATermAppl c, final DependencySet d)
	{
		if (hasType(c))
			return;

		/*
		 * A negated nominal is turned into a different
		 */
		if (ATermUtils.isNot(c))
		{
			final ATermAppl arg = (ATermAppl) c.getArgument(0);
			if (ATermUtils.isNominal(arg))
			{
				final ATermAppl v = (ATermAppl) arg.getArgument(0);
				Literal other = _abox.getLiteral(v);
				if (other == null)
					other = _abox.addLiteral(v, d);
				super.setDifferent(other, d);
				return;
			}
		}
		super.addType(c, d);

		// TODO when two literals are being merged this is not efficient
		// if(_abox.isInitialized())
		checkClash();
	}

	public void addAllTypes(final Map<ATermAppl, DependencySet> types, final DependencySet ds)
	{
		for (final Entry<ATermAppl, DependencySet> entry : types.entrySet())
		{
			final ATermAppl c = entry.getKey();

			if (hasType(c))
				continue;

			final DependencySet depends = entry.getValue();

			super.addType(c, depends.union(ds, _abox.doExplanation()));
		}

		checkClash();
	}

	@Override
	public boolean hasSuccessor(final Node x)
	{
		return false;
	}

	@Override
	public final Literal getSame()
	{
		return (Literal) super.getSame();
	}

	@Override
	public ATermAppl getTerm()
	{
		return _hasValue ? (ATermAppl) _atermValue.getArgument(0) : null;
	}

	public String getLang()
	{
		return _hasValue ? ((ATermAppl) ((ATermAppl) _atermValue.getArgument(0)).getArgument(ATermUtils.LIT_LANG_INDEX)).getName() : "";
	}

	public String getLexicalValue()
	{
		if (_hasValue)
			return _value.toString();

		return null;
	}

	public void reportClash(final Clash clash)
	{
		_clashed = true;
		_abox.setClash(clash);
	}

	private void checkClash()
	{
		_clashed = false;

		if (_hasValue && _value == null)
		{
			reportClash(Clash.invalidLiteral(this, getDepends(_name), getTerm()));
			return;
		}

		if (hasType(ATermUtils.BOTTOM_LIT))
		{
			reportClash(Clash.emptyDatatype(this, getDepends(ATermUtils.BOTTOM_LIT)));
			if (_abox.doExplanation())
				System.out.println("1) Literal clash dependency = " + _abox.getClash());
			return;
		}

		final Set<ATermAppl> types = getTypes();
		final DatatypeReasoner dtReasoner = _abox.getDatatypeReasoner();

		try
		{
			if (_hasValue)
			{

				if (!dtReasoner.isSatisfiable(types, _value))
				{
					final ArrayList<ATermAppl> primitives = new ArrayList<>();
					for (final ATermAppl t : types)
						if (ATermUtils.TOP_LIT.equals(t))
							continue;
						else
							primitives.add(t);

					final ATermAppl dt[] = primitives.toArray(new ATermAppl[primitives.size() - 1]);

					DependencySet ds = DependencySet.EMPTY;
					for (final ATermAppl element : dt)
					{
						ds = ds.union(getDepends(element), _abox.doExplanation());
						if (_abox.doExplanation())
						{
							final ATermAppl dtName = ATermUtils.isNot(element) ? (ATermAppl) element.getArgument(0) : element;
							final ATermAppl definition = dtReasoner.getDefinition(dtName);
							if (definition != null)
								ds = ds.union(Collections.singleton(ATermUtils.makeDatatypeDefinition(dtName, definition)), true);
						}
					}

					reportClash(Clash.valueDatatype(this, ds, (ATermAppl) _atermValue.getArgument(0), dt[0]));
				}
			}
			else
				if (dtReasoner.isSatisfiable(types))
				{
					if (!dtReasoner.containsAtLeast(2, types))
					{
						/*
						 * This literal is a variable, but given _current ranges can only
						 * take on a single _value.  Merge with that _value.
						 */
						final Object value = dtReasoner.valueIterator(types).next();
						final ATermAppl valueTerm = dtReasoner.getLiteral(value);
						Literal valueLiteral = _abox.getLiteral(valueTerm);
						if (valueLiteral == null)
							/*
							 * No dependency set is used here because omitting it prevents the
							 * constant literal from being removed during backtrack
							 */
							valueLiteral = _abox.addLiteral(valueTerm);
						DependencySet mergeDs = DependencySet.INDEPENDENT;
						for (final DependencySet ds : _depends.values())
							mergeDs = mergeDs.union(ds, _abox.doExplanation());
						_merge = new NodeMerge(this, valueLiteral, mergeDs);
					}
				}
				else
				{
					final ArrayList<ATermAppl> primitives = new ArrayList<>();
					for (final ATermAppl t : types)
						if (ATermUtils.TOP_LIT.equals(t))
							continue;
						else
							primitives.add(t);

					final ATermAppl dt[] = primitives.toArray(new ATermAppl[primitives.size() - 1]);

					DependencySet ds = DependencySet.EMPTY;
					for (final ATermAppl element : dt)
					{
						ds = ds.union(getDepends(element), _abox.doExplanation());
						if (_abox.doExplanation())
						{
							final ATermAppl definition = dtReasoner.getDefinition(element);
							if (definition != null)
								ds = ds.union(Collections.singleton(ATermUtils.makeDatatypeDefinition(element, definition)), true);
						}
					}

					reportClash(Clash.emptyDatatype(this, ds, dt));
				}
		}
		catch (final DatatypeReasonerException e)
		{
			final String msg = "Unexcepted datatype reasoner exception: " + e.getMessage();
			_logger.severe(msg);
			throw new InternalReasonerException(msg, e);
		}
	}

	public Object getValue()
	{
		return _value;
	}

	@Override
	public boolean restore(final int branch)
	{
		final Boolean restorePruned = restorePruned(branch);
		if (Boolean.FALSE.equals(restorePruned))
			return restorePruned;

		boolean restored = Boolean.TRUE.equals(restorePruned);

		restored |= super.restore(branch);

		if (_clashed)
			checkClash();

		return restored;
	}

	@Override
	final public void prune(final DependencySet ds)
	{
		_pruned = ds;
	}

	@Override
	public void unprune(final int branch)
	{
		super.unprune(branch);

		checkClash();
	}

	//	public String debugString()
	//	{
	//		return _name + " = " + getTypes().toString();
	//	}

	public NodeMerge getMergeToConstant()
	{
		return _merge;
	}

	public void clearMergeToConstant()
	{
		_merge = null;
	}
}
