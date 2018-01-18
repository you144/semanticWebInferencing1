// Copyright (c) 2006 - 2009, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.sparqlowl.parser.test;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStreamReader;
import openllet.query.sparqlowl.parser.antlr.SparqlOwlLexer;
import openllet.query.sparqlowl.parser.antlr.SparqlOwlParser;
import openllet.query.sparqlowl.parser.antlr.SparqlOwlTreeARQ;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

/**
 * <p>
 * Title: TreeARQ Driver
 * </p>
 * <p>
 * Description: Stub driver that reads Terp query on stdin and writes the ARQ friendly version on stdout. Useful to exercise {@link SparqlOwlTreeARQ}.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Mike Smith <a href="mailto:msmith@clarkparsia.com">msmith@clarkparsia.com</a>
 */
public class TreeARQDriver
{

	public static void main(final String[] args) throws IOException, RecognitionException
	{
		final SparqlOwlLexer lexer = new SparqlOwlLexer(new ANTLRReaderStream(new InputStreamReader(System.in)));
		final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		final SparqlOwlParser parser = new SparqlOwlParser(tokenStream);
		SparqlOwlParser.query_return result;
		try
		{
			result = parser.query();
		}
		catch (final RecognitionException e)
		{
			throw new QueryParseException(format("%s %s", parser.getErrorHeader(e), parser.getErrorMessage(e, parser.getTokenNames())), e.line, e.charPositionInLine);
		}
		final CommonTree t = result.getTree();

		final CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
		nodes.setTokenStream(tokenStream);

		final SparqlOwlTreeARQ treeWalker = new SparqlOwlTreeARQ(nodes);
		final Query q = treeWalker.query(null);

		if (q.getPrefix("rdf") == null)
			q.setPrefix("rdf", RDF.getURI());
		if (q.getPrefix("rdfs") == null)
			q.setPrefix("rdfs", RDFS.getURI());
		if (q.getPrefix("owl") == null)
			q.setPrefix("owl", OWL.getURI());
		if (q.getPrefix("xsd") == null)
			q.setPrefix("xsd", XSD.getURI());

		System.out.print("\nARQ Query\n---------\n");
		q.serialize(System.out);
	}
}
