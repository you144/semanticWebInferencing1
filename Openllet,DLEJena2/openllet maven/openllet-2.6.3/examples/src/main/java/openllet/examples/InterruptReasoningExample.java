package openllet.examples;

import java.util.logging.Logger;
import openllet.core.OpenlletOptions;
import openllet.core.exceptions.TimeoutException;
import openllet.core.utils.Timers;
import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasonerFactory;
import openllet.query.sparqldl.jena.SparqlDLExecutionFactory;
import openllet.shared.tools.Log;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: an example program that shows how different reasoning services provided by Pellet can be interrupted based on a user-defined timeout without
 * affecting subsequent query results. The program defines some constant values for timeout definitions to demonstrate various different things but results may
 * vary on different computers based on CPU speed, available memory, etc.
 * </p>
 * <p>
 * Sample output from this program looks like this:
 *
 * <pre>
 * Parsing the ontology...finished
 *
 * Consistency Timeout: 5000ms
 * Checking consistency...finished in 1965
 *
 * Classify Timeout: 50000ms
 * Classifying...finished in 12668ms
 * Classified: true
 *
 * Realize Timeout: 1000ms
 * Realizing...interrupted after 1545ms
 * Realized: false
 *
 * Query Timeout: 0ms
 * Retrieving instances of AmericanWine...completed in 484ms (24 results)
 * Running SPARQL query...completed in 11801ms (23 results)
 *
 * Query Timeout: 200ms
 * Retrieving instances of AmericanWine...interrupted after 201ms
 * Running SPARQL query...interrupted after 201ms
 *
 * Query Timeout: 2000ms
 * Retrieving instances of AmericanWine...completed in 417ms (24 results)
 * Running SPARQL query...interrupted after 2001ms
 *
 * Query Timeout: 20000ms
 * Retrieving instances of AmericanWine...completed in 426ms (24 results)
 * Running SPARQL query...completed in 11790ms (23 results)
 * </pre>
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class InterruptReasoningExample
{
	private static final Logger _logger = Log.getLogger(InterruptReasoningExample.class);

	// various different constants to control the timeout values. typically
	// it is desirable to set different timeouts for classification and realization
	// since they are done only once and take more time compared to answering
	// queries
	public static class Timeouts
	{
		// timeout for consistency checking
		public static int CONSISTENCY = 5000;

		// timeout for classification
		public static int CLASSIFY = 50000;

		// timeout for realization (this value is intentionally
		// set to a unrealistically small value for demo purposes)
		public static int REALIZE = 1000;
	}

	// some constants related to wine ontology including some
	// arbitrary
	public static class WINE
	{
		public static final String NS = "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#";

		public static final Resource AmericanWine = ResourceFactory.createResource(NS + "AmericanWine");

		public static final Query query = QueryFactory.create("PREFIX wine: <" + WINE.NS + ">\n" + "SELECT * WHERE {\n" + "   ?x a wine:RedWine ; \n" + "      wine:madeFromGrape ?y \n" + "}");
	}

	// the Jena model we are using
	private final OntModel model;

	// underlying openllet graph
	private final PelletInfGraph openllet;

	// the _timers associated with the Pellet KB
	private final Timers timers;

	public static void main(final String[] args) throws Exception
	{
		OpenlletOptions.USE_CLASSIFICATION_MONITOR = OpenlletOptions.MonitorType.NONE;

		final InterruptReasoningExample test = new InterruptReasoningExample();

		test.parse();

		test.consistency();

		test.classify();

		test.realize();

		test.query();
	}

	public InterruptReasoningExample()
	{
		// create the Jena model
		model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);

		openllet = (PelletInfGraph) model.getGraph();

		// get the underlying openllet timers
		timers = openllet.getKB().getTimers();

		// set the timeout for main reasoning tasks
		timers.createTimer("complete").setTimeout(Timeouts.CONSISTENCY);
		timers.createTimer("classify").setTimeout(Timeouts.CLASSIFY);
		timers.createTimer("realize").setTimeout(Timeouts.REALIZE);
	}

	// read the _data into a Jena model
	public void parse()
	{
		System.out.print("Parsing the ontology...");

		model.read("file:test/data/modularity/wine.owl");

		System.out.println("finished");
		System.out.println();
	}

	// check the consistency of _data. this function will throw a TimeoutException
	// we don't catch the exception here because there is no point in continuing
	// if the initial consistency check is not finished. Pellet will not be able
	// to perform any reasoning steps if it dannot check the consistency.
	public void consistency() throws TimeoutException
	{
		System.out.println("Consistency Timeout: " + Timeouts.CONSISTENCY + "ms");
		System.out.print("Checking consistency...");

		model.prepare();

		System.out.println("finished in " + timers.getTimer("isConsistent").get().getLast());
		System.out.println();
	}

	// classify the ontology
	public void classify()
	{
		System.out.println("Classify Timeout: " + Timeouts.CLASSIFY + "ms");
		System.out.print("Classifying...");

		try
		{
			((PelletInfGraph) model.getGraph()).classify();
			System.out.println("finished in " + timers.getTimer("classify").get().getLast() + "ms");
		}
		catch (final TimeoutException e)
		{
			Log.error(_logger, e);
			System.out.println("interrupted after " + timers.getTimer("classify").get().getElapsed() + "ms");
		}

		System.out.println("Classified: " + openllet.isClassified());
		System.out.println();
	}

	public void realize()
	{
		// realization can only be done if classification is completed
		if (!openllet.isClassified())
			return;

		System.out.println("Realize Timeout: " + Timeouts.REALIZE + "ms");
		System.out.print("Realizing...");

		try
		{
			openllet.realize();
			System.out.println("finished in " + timers.getTimer("realize").get().getLast() + "ms");
		}
		catch (final TimeoutException e)
		{
			Log.error(_logger, e);
			System.out.println("interrupted after " + timers.getTimer("realize").get().getElapsed() + "ms");
		}

		System.out.println("Realized: " + openllet.isRealized());
		System.out.println();
	}

	// run some sample queries with different timeouts
	public void query()
	{
		// different timeout values in ms for querying (0 means no timeout)
		final int[] timeouts = { 0, 200, 2000, 20000 };

		for (final int timeout : timeouts)
		{
			// update the timeout value
			timers._mainTimer.setTimeout(timeout);
			System.out.println("Query Timeout: " + timeout + "ms");

			// run the queries
			getInstances(WINE.AmericanWine);
			execQuery(WINE.query);

			System.out.println();
		}
	}

	public void getInstances(final Resource cls)
	{
		System.out.print("Retrieving instances of " + cls.getLocalName() + "...");

		// we need to restart the timer every time because timeouts are checked
		// w.r.t. the time a timer was started. not resetting the timer will
		// cause timeout exceptions nearly all the time
		timers._mainTimer.restart();

		try
		{
			// run a simple query using Jena interface
			final ExtendedIterator<?> results = model.listIndividuals(cls);

			// print if the query succeeded
			final int size = results.toList().size();
			System.out.print("completed in " + timers._mainTimer.getElapsed() + "ms");
			System.out.println(" (" + size + " results)");
		}
		catch (final TimeoutException e)
		{
			Log.error(_logger, e);
			System.out.println("interrupted after " + timers._mainTimer.getElapsed() + "ms");
		}
	}

	public void execQuery(final Query query)
	{
		System.out.print("Running SPARQL query...");

		// we need to restart the timer as above
		timers._mainTimer.restart();

		try
		{
			// run the SPARQL query
			final ResultSet results = SparqlDLExecutionFactory.create(query, model).execSelect();

			final int size = ResultSetFormatter.consume(results);
			System.out.print("completed in " + timers._mainTimer.getElapsed() + "ms");
			System.out.println(" (" + size + " results)");
		}
		catch (final TimeoutException e)
		{
			Log.error(_logger, e);
			System.out.println("interrupted after " + timers._mainTimer.getElapsed() + "ms");
		}
	}
}
