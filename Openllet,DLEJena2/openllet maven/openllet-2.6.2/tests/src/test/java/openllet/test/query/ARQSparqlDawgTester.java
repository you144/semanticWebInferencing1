// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.atom.OpenError;
import openllet.core.utils.URIUtils;
import openllet.query.sparqldl.jena.JenaIOUtils;
import openllet.shared.tools.Log;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;

/**
 * <p>
 * Title: Engine for processing DAWG test manifests
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
 * @author Petr Kremen
 */
public class ARQSparqlDawgTester implements SparqlDawgTester
{

	private static final Logger _logger = Log.getLogger(ARQSparqlDawgTester.class);

	private final List<String> _avoidList = Arrays.asList(new String[] {
			// FIXME with some effort some
			// of the following queries can
			// be handled

			// The following tests require [object/data]property punning in the
			// _data
			"open-eq-07", "open-eq-08", "open-eq-09", "open-eq-10", "open-eq-11", "open-eq-12",

			// not an approved test (and in clear conflict with
			// "dawg-optional-filter-005-simplified",
			"dawg-optional-filter-005-not-simplified",

			// fails due to bugs in ARQ filter handling
			"date-2", "date-3",

			// ?x p "+3"^^xsd:int does not match "3"^^xsd:int
			"unplus-1",

			// ?x p "01"^^xsd:int does not match "1"^^xsd:int
			"open-eq-03",

			// "1"^^xsd:int does not match different lexical forms
			"eq-1", "eq-2" });

	private String _queryURI = "";

	protected Set<String> _graphURIs = new HashSet<>();

	protected Set<String> _namedGraphURIs = new HashSet<>();

	protected Query _query = null;

	private String _resultURI = null;

	public ARQSparqlDawgTester()
	{
	}

	protected void afterExecution()
	{
		// do nothing
	}

	protected void beforeExecution()
	{
		// do nothing
	}

	protected Dataset createDataset()
	{
		if (_query.getGraphURIs().isEmpty() && _query.getNamedGraphURIs().isEmpty())
			return DatasetFactory.create(new ArrayList<>(_graphURIs), new ArrayList<>(_namedGraphURIs));
		else
			return DatasetFactory.create(_query.getGraphURIs(), _query.getNamedGraphURIs());

	}

	protected QueryExecution createQueryExecution()
	{
		return QueryExecutionFactory.create(_query, createDataset());
	}

	@Override
	public void setDatasetURIs(final Set<String> graphURIs, final Set<String> namedGraphURIs)
	{
		_graphURIs = graphURIs;
		_namedGraphURIs = namedGraphURIs;
	}

	@Override
	public void setQueryURI(final String queryURI)
	{
		if (_queryURI.equals(queryURI))
			return;

		_queryURI = queryURI;
		_query = QueryFactory.read(queryURI);
	}

	@Override
	public void setResult(final String resultURI)
	{
		_resultURI = resultURI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isParsable()
	{
		try
		{
			_query = QueryFactory.read(_queryURI);
			return true;
		}
		catch (final Exception e)
		{
			_logger.log(Level.INFO, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCorrectlyEvaluated()
	{
		try
		{
			beforeExecution();
			final QueryExecution exec = createQueryExecution();

			if (_resultURI == null)
			{
				_logger.log(Level.WARNING, "No result set associated with this test, assumuing success!");
				return true;
			}

			if (_query.isSelectType())
			{
				final ResultSetRewindable expected = ResultSetFactory.makeRewindable(JenaIOUtils.parseResultSet(_resultURI));
				final ResultSetRewindable real = ResultSetFactory.makeRewindable(exec.execSelect());

				final boolean correct = ResultSetUtils.assertEquals(expected, real);

				if (!correct)
				{
					logResults("Expected", expected);
					logResults("Real", real);
				}

				return correct;

			}
			else
				if (_query.isAskType())
				{
					final boolean askReal = exec.execAsk();
					final boolean askExpected = JenaIOUtils.parseAskResult(_resultURI);

					_logger.fine("Expected=" + askExpected);
					_logger.fine("Real=" + askReal);

					return askReal == askExpected;
				}
				else
					if (_query.isConstructType())
					{
						final Model real = exec.execConstruct();
						final Model expected = FileManager.get().loadModel(_resultURI);

						_logger.fine("Expected=" + real);
						_logger.fine("Real=" + expected);

						return real.isIsomorphicWith(expected);
					}
					else
						if (_query.isDescribeType())
						{
							final Model real = exec.execDescribe();
							final Model expected = FileManager.get().loadModel(_resultURI);

							_logger.fine("Expected=" + real);
							_logger.fine("Real=" + expected);

							return real.isIsomorphicWith(expected);
						}
						else
							throw new OpenError("The query has invalid type.");
		}
		catch (final IOException e)
		{
			_logger.log(Level.SEVERE, e.getMessage(), e);
			return false;
		}
		finally
		{
			afterExecution();
		}
	}

	private static void logResults(final String name, final ResultSetRewindable results)
	{
		if (_logger.isLoggable(Level.WARNING))
		{
			results.reset();
			final StringBuilder sb = new StringBuilder(name + " (" + results.size() + ")=");

			while (results.hasNext())
			{
				final QuerySolution result = results.nextSolution();
				sb.append(result);
			}

			_logger.warning(sb.toString());
		}

		if (_logger.isLoggable(Level.FINE))
		{
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			ResultSetFormatter.out(out, results);
			_logger.fine(out.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isApplicable(final String testURI)
	{
		return !_avoidList.contains(URIUtils.getLocalName(testURI));
	}

	@Override
	public String getName()
	{
		return "ARQ";
	}
}
