// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.query.sparqldl.jena;

import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.main.StageGenerator;

/**
 * <p>
 * Description: A stage generator that generates one {@link SparqlDLStage} for each {@link BasicPattern}
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
class SparqlDLStageGenerator implements StageGenerator
{

	/*
	 * If this variable is true then queries with variable SPO statements are
	 * not handled by the SPARQL-DL engine but fall back to ARQ
	 */
	private boolean _handleVariableSPO = true;

	public SparqlDLStageGenerator()
	{
		this(true);
	}

	public SparqlDLStageGenerator(final boolean handleVariableSPO)
	{
		_handleVariableSPO = handleVariableSPO;
	}

	@Override
	public QueryIterator execute(final BasicPattern pattern, final QueryIterator input, final ExecutionContext execCxt)
	{
		return new SparqlDLStage(pattern, _handleVariableSPO).build(input, execCxt);
	}
}
