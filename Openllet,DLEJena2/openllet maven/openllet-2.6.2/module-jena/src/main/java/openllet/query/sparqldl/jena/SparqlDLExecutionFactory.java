// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.query.sparqldl.jena;

import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.core.KnowledgeBase;
import openllet.core.exceptions.UnsupportedQueryException;
import openllet.jena.PelletInfGraph;
import openllet.query.sparqldl.parser.ARQParser;
import openllet.shared.tools.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class SparqlDLExecutionFactory
{
	private static final Logger _logger = Log.getLogger(SparqlDLExecutionFactory.class);

	/**
	 * Different types of query engine that can be used for answering queries.
	 */
	public enum QueryEngineType
	{
		/**
		 * This is the standard ARQ query engine where all the query answering bits are handled by ARQ and Jena and the underlying Pellet model is queried with
		 * single triple patterns. For this reason, this query engine cannot handle complex class expressions in the query.
		 */
		ARQ,

		/**
		 * The mixed query engine uses ARQ to handle SPARQL algebra and the Pellet query engine is used to answer Basic Graph Patterns (BGPs). Unlike pure
		 * Pellet engine, this engine can answer any kind of SPARQL query however it might be slightly slower for queries supported by Pellet engine. On the
		 * other hand, this engine typically performs better than the ARQ engine since answering a BGP as a whole is faster than answering each triple in
		 * isolation.
		 */
		MIXED,

		/**
		 * This is the specialized Pellet query engine that will answer SPARQL-DL queries. This is the most efficient query engine to answer SPARQL queries with
		 * Pellet. However, some queries are not supported by this query engine and will cause QueryException to be thrown. Unsupported features are:
		 * <ul>
		 * <li>DESCRIBE queries</li>
		 * <li>Named graphs in queries</li>
		 * <li>OPTIONAL and UNION operators</li>
		 * </ul>
		 * All other SPARQL operators and query forms are supported.
		 */
		PELLET
	}

	/**
	 * Creates a QueryExecution by selecting the most appropriate {@link QueryEngineType} that can answer the given query.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param dataset the target of the query
	 * @param initialBinding initial binding that will be applied before query execution or <code>null</code> if there is no initial binding
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution create(final Query query, final Dataset dataset, final QuerySolution initialBinding)
	{
		QueryEngineType engineType = QueryEngineType.ARQ;

		final Graph graph = dataset.getDefaultModel().getGraph();
		// if underlying model is not Pellet then we'll use ARQ
		if (graph instanceof PelletInfGraph)
			// check for obvious things not supported by Pellet
			if (dataset.listNames().hasNext() || query.isDescribeType())
			engineType = QueryEngineType.MIXED;
			else
			{
			// try parsing the query and see if there are any problems
			final PelletInfGraph pelletInfGraph = (PelletInfGraph) graph;

			final KnowledgeBase kb = pelletInfGraph.getKB();
			pelletInfGraph.prepare();

			final ARQParser parser = new ARQParser();
			// The parser uses the query parameterization to resolve
			// parameters
			// (i.e. variables) in the query
			parser.setInitialBinding(initialBinding);

			try
			{
			parser.parse(query, kb);
			// parsing successful so we can use Pellet engine
			engineType = QueryEngineType.PELLET;
			}
			catch (final UnsupportedQueryException e)
			{
			_logger.log(Level.FINER, "", e);
			// parsing failed so we will use the mixed engine
			engineType = QueryEngineType.MIXED;
			}
			}

		return create(query, dataset, initialBinding, engineType);
	}

	/**
	 * Creates a QueryExecution with the given {@link QueryEngineType}. If the query engine cannot handle the given query a QueryException may be thrown during
	 * query execution. Users are recommended to use {@link #create(Query, Dataset, QuerySolution)}
	 *
	 * @param query the query
	 * @param dataset the target of the query
	 * @param initialBinding used for parametrized queries
	 * @param queryEngineType type of the query engine that will be used to answer the query
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution create(final Query query, final Dataset dataset, final QuerySolution initialBinding, final QueryEngineType queryEngineType)
	{
		return create(query, dataset, initialBinding, queryEngineType, true);
	}

	/**
	 * Creates a QueryExecution with the given {@link QueryEngineType}. If the query engine cannot handle the given query a QueryException may be thrown during
	 * query execution. Users are recommended to use {@link #create(Query, Dataset, QuerySolution)} User must use a " try () {} " to manage the AutoClosable
	 * QueryExecution.
	 *
	 * @param query the query
	 * @param dataset the target of the query
	 * @param initialBinding used for parametrized queries
	 * @param queryEngineType type of the query engine that will be used to answer the query
	 * @param handleVariableSPO If this variable is true then queries with variable SPO statements are not handled by the SPARQL-DL engine but fall back to ARQ
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution create(final Query query, final Dataset dataset, final QuerySolution initialBinding, final QueryEngineType queryEngineType, final boolean handleVariableSPO) throws QueryException
	{
		// the engine we will return
		QueryExecution queryExec = null;

		// create an engine based on the type
		switch (queryEngineType)
		{
			case PELLET:
				queryExec = new SparqlDLExecution(query, dataset, handleVariableSPO);
				((SparqlDLExecution) queryExec).setPurePelletQueryExec(true);
				break;
			case ARQ:
				queryExec = QueryExecutionFactory.create(query, dataset);
				break;
			case MIXED:
				queryExec = QueryExecutionFactory.create(query, dataset);
				queryExec.getContext().set(ARQ.stageGenerator, new SparqlDLStageGenerator(handleVariableSPO));
				break;
			default:
				throw new AssertionError();
		}

		// if given set the initial binding
		if (initialBinding != null)
			queryExec.setInitialBinding(initialBinding);

		// return it
		return queryExec;
	}

	/**
	 * Creates a QueryExecution object where the Basic Graph Patterns (BGPs) will be answered by native Pellet query engine whenever possible. The unsupported
	 * BGPs, i.e. the ones that are not in the SPARQL-DL fragment, will be answered by the Jena query engine. With this fall-back model all types of SPARQL
	 * queries are supported.
	 *
	 * @param query the query
	 * @param model the target of the query
	 * @return a <code>QueryExecution</code> that will answer the query with the given model
	 * @throws QueryException if the given model is not associated with Pellet reasoner
	 */
	public static QueryExecution create(final Query query, final Model model)
	{
		return create(query, model, null);
	}

	/**
	 * Creates a QueryExecution by selecting the most appropriate {@link QueryEngineType} that can answer the given query.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param dataset the target of the query
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution create(final Query query, final Dataset dataset)
	{
		return create(query, dataset, null);
	}

	/**
	 * Creates a QueryExecution by selecting the most appropriate {@link QueryEngineType} that can answer the given query.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param model the target of the query
	 * @param initialBinding initial binding that will be applied before query execution or <code>null</code> if there is no initial binding
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution create(final Query query, final Model model, final QuerySolution initialBinding)
	{
		return create(query, DatasetFactory.create(model), initialBinding);
	}

	/**
	 * Creates a Pellet query engine that will answer the given query. This function should be used only if it is known that Pellet query engine can handle the
	 * given query. Otherwise query execution will result in an exception. for arbitrary queries it is safer to use the <code>create</code> functions.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param model the target of the query
	 * @return a <code>QueryExecution</code> that will answer the query with the given model
	 */
	public static QueryExecution createPelletExecution(final Query query, final Model model)
	{
		return create(query, DatasetFactory.create(model), null, QueryEngineType.PELLET);
	}

	/**
	 * Creates a Pellet query engine that will answer the given query. This function should be used only if it is known that Pellet query engine can handle the
	 * given query. Otherwise query execution will result in an exception. for arbitrary queries it is safer to use the <code>create</code> functions.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param model the target of the query
	 * @param initialBinding initial binding that will be applied before query execution or <code>null</code> if there is no initial binding
	 * @return a <code>QueryExecution</code> that will answer the query with the given model
	 */
	public static QueryExecution createPelletExecution(final Query query, final Model model, final QuerySolution initialBinding)
	{
		return create(query, DatasetFactory.create(model), initialBinding, QueryEngineType.PELLET);
	}

	/**
	 * Creates a Pellet query engine that will answer the given query. This function should be used only if it is known that Pellet query engine can handle the
	 * given query. Otherwise query execution will result in an exception. for arbitrary queries it is safer to use the <code>create</code> functions.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param dataset the target of the query
	 * @param initialBinding initial binding that will be applied before query execution or <code>null</code> if there is no initial binding
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution createPelletExecution(final Query query, final Dataset dataset)
	{
		return create(query, dataset, null, QueryEngineType.PELLET);
	}

	/**
	 * Creates a Pellet query engine that will answer the given query. This function should be used only if it is known that Pellet query engine can handle the
	 * given query. Otherwise query execution will result in an exception. for arbitrary queries it is safer to use the <code>create</code> functions.
	 *
	 * @see QueryEngineType
	 * @param query the query
	 * @param dataset the target of the query
	 * @param initialBinding initial binding that will be applied before query execution or <code>null</code> if there is no initial binding
	 * @return a <code>QueryExecution</code> that will answer the query with the given dataset
	 */
	public static QueryExecution createPelletExecution(final Query query, final Dataset dataset, final QuerySolution initialBinding)
	{
		return create(query, dataset, initialBinding, QueryEngineType.PELLET);
	}

	/**
	 * @deprecated Use {@link #createPelletExecution(Query, Model)} instead
	 */
	@Deprecated
	public static QueryExecution createBasicExecution(final Query query, final Model model)
	{
		return createPelletExecution(query, model);
	}

	/**
	 * @deprecated Use {@link #createPelletExecution(Query, Dataset)} instead
	 */
	@Deprecated
	public static QueryExecution createBasicExecution(final Query query, final Dataset dataset)
	{
		return createPelletExecution(query, dataset);
	}

}
