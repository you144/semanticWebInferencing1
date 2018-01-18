// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules;

import java.util.logging.Logger;
import openllet.core.boxes.abox.ABoxImpl;
import openllet.shared.tools.Log;

/**
 * <p>
 * Title: Binding Generator
 * </p>
 * <p>
 * Description:
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
public interface BindingGenerator extends Iterable<VariableBinding>
{
	public final static Logger _logger = Log.getLogger(ABoxImpl.class); // We took the logger of the abox; also the logger is public to allow rules to use it !
}
