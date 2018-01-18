package openllet.sparqlowl.parser.test;

import java.io.IOException;
import java.io.InputStreamReader;
import openllet.query.sparqlowl.parser.antlr.SparqlOwlLexer;
import openllet.query.sparqlowl.parser.antlr.SparqlOwlParser;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

/**
 * <p>
 * Title: Parser Driver
 * </p>
 * <p>
 * Description: Stub driver that reads Terp query on stdin and writes the AST on stdout. Useful to exercise {@link SparqlOwlParser}.
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
public class ParserDriver
{
	public static void main(final String[] args) throws IOException, RecognitionException
	{
		final SparqlOwlLexer lexer = new SparqlOwlLexer(new ANTLRReaderStream(new InputStreamReader(System.in)));
		final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		final SparqlOwlParser parser = new SparqlOwlParser(tokenStream);
		final SparqlOwlParser.query_return result = parser.query();
		final Tree t = result.getTree();
		System.out.println(t.toStringTree());
	}
}
