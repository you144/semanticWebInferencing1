// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Literal;
import openllet.core.boxes.rbox.Role;
import openllet.core.rules.model.AtomVariable;
import openllet.core.rules.model.DatavaluedPropertyAtom;

/**
 * <p>
 * Title: Datavalue Property Binding Helper
 * </p>
 * <p>
 * Description: Generates bindings based off the given _pattern. The predicate must be a datatype property. TODO: Rename to DatavaluedPropertyBindingHelper
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
public class DatavaluePropertyBindingHelper implements BindingHelper
{
	private final ABox _abox;
	private VariableBinding _binding;
	private Literal _object;
	private Iterator<Literal> _objectIterator;
	private final DatavaluedPropertyAtom _pattern;
	private Role _role;
	private Individual _subject;
	private Iterator<Individual> _subjectIterator;

	public DatavaluePropertyBindingHelper(final ABox abox, final DatavaluedPropertyAtom pattern)
	{
		_abox = abox;
		_pattern = pattern;
	}

	@Override
	public Collection<AtomVariable> getBindableVars(final Collection<AtomVariable> bound)
	{
		return VariableUtils.getVars(_pattern);
	}

	private Literal getObject()
	{
		return _binding.get(_pattern.getArgument2());
	}

	@Override
	public Collection<AtomVariable> getPrerequisiteVars(final Collection<AtomVariable> bound)
	{
		return Collections.emptySet();
	}

	private Role getRole()
	{
		if (_role == null)
			_role = _abox.getRole(_pattern.getPredicate());
		return _role;
	}

	private Individual getSubject()
	{
		return _binding.get(_pattern.getArgument1());
	}

	/**
	 * Checks to see if an object is set (either bound, or a constant)
	 * 
	 * @return
	 */
	private boolean isObjectSet()
	{
		return _binding.get(_pattern.getArgument2()) != null;
	}

	/**
	 * Set the incoming binding for this helper. This fixes any variables that are already bound by a preceding Binding Helper.
	 *
	 * @param newBinding Binding map. Copied on input.
	 */
	@Override
	public void rebind(final VariableBinding newBinding)
	{
		_binding = new VariableBinding(newBinding);

		if (getSubject() != null)
			_subjectIterator = Collections.singleton(getSubject()).iterator();
		else
			_subjectIterator = new AllNamedIndividualsIterator(_abox);

	}

	/**
	 * Selects the next binding.
	 *
	 * @return True if a binding was available for this _pattern given the initial binding. False otherwise. Will return if the binding is not set.
	 */
	@Override
	public boolean selectNextBinding()
	{
		if (_binding == null)
			return false;

		while (true)
		{
			if (_subject == null || isObjectSet())
			{
				// Check to see if there are any more subjects to try
				if (!_subjectIterator.hasNext())
					return false;
				_subject = _subjectIterator.next();

				if (!isObjectSet())
					_objectIterator = new LiteralFilter(_subject.getRNeighbors(getRole()).iterator());
			}

			if (isObjectSet())
			{
				// Object of _pattern is already set; just test the _pattern
				final boolean result = _subject.getRNeighbors(getRole()).contains(getObject());
				if (result)
					return true;
			}
			else
				// Cycle through possible object bindings
				if (_objectIterator.hasNext())
				{
					_object = _objectIterator.next();
					return true;
				}
				else
					// no more bindings - need a new subject
					_subject = null;
		}
	}

	/**
	 * Set the variables this _pattern uses in the given map.
	 * 
	 * @param currentBinding
	 */
	@Override
	public void setCurrentBinding(final VariableBinding currentBinding)
	{
		currentBinding.set(_pattern.getArgument1(), _subject);
		currentBinding.set(_pattern.getArgument2(), _object);
	}

	@Override
	public String toString()
	{
		return "edges(" + _pattern + ")";
	}

}
