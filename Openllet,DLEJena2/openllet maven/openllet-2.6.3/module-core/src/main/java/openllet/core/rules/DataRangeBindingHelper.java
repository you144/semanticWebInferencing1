// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Literal;
import openllet.core.datatypes.DatatypeReasoner;
import openllet.core.datatypes.exceptions.DatatypeReasonerException;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.rules.model.AtomVariable;
import openllet.core.rules.model.DataRangeAtom;
import openllet.shared.tools.Log;

/**
 * <p>
 * Title: Data Range Binding Helper
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
public class DataRangeBindingHelper implements BindingHelper
{

	private static final Logger _logger = Log.getLogger(DataRangeBindingHelper.class);

	private final DatatypeReasoner _dtReasoner;
	private final DataRangeAtom _atom;
	private boolean _hasNext;

	public DataRangeBindingHelper(final ABox abox, final DataRangeAtom atom)
	{
		_dtReasoner = abox.getDatatypeReasoner();
		_atom = atom;
		_hasNext = false;
	}

	@Override
	public Collection<AtomVariable> getBindableVars(final Collection<AtomVariable> bound)
	{
		return Collections.emptySet();
	}

	@Override
	public Collection<AtomVariable> getPrerequisiteVars(final Collection<AtomVariable> bound)
	{
		return VariableUtils.getVars(_atom);
	}

	@Override
	public void rebind(final VariableBinding newBinding)
	{
		final Literal dValue = newBinding.get(_atom.getArgument());

		if (dValue == null)
			throw new InternalReasonerException("DataRangeBindingHelper cannot generate bindings for " + _atom);

		try
		{
			_hasNext = _dtReasoner.isSatisfiable(Collections.singleton(_atom.getPredicate()), dValue.getValue());
		}
		catch (final DatatypeReasonerException e)
		{
			final String msg = "Unexpected datatype reasoner exception: " + e.getMessage();
			_logger.severe(msg);
			throw new InternalReasonerException(e);
		}
	}

	@Override
	public boolean selectNextBinding()
	{
		if (_hasNext)
		{
			_hasNext = false;
			return true;
		}
		return false;
	}

	@Override
	public void setCurrentBinding(final VariableBinding currentBinding)
	{
		// This space left intentionally blank.
	}

	@Override
	public String toString()
	{
		return _atom.toString();
	}

}
