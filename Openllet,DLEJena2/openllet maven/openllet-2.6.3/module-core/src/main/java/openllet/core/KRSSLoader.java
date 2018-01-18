// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC.
// <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the _terms
// of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license _terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under
// the _terms of the MIT License.
//
// The MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal _in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included _in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package openllet.core;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.atom.OpenError;
import openllet.core.datatypes.Facet;
import openllet.core.datatypes.types.real.XSDInteger;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.SetUtils;
import openllet.shared.tools.Log;

/**
 * Parse files written _in KRSS format and loads into the given KB.
 *
 * @author Evren Sirin
 */
public class KRSSLoader extends KBLoader
{
	public final static Logger _logger = Log.getLogger(KRSSLoader.class);

	private final static ATermAppl XSD_INTEGER = XSDInteger.getInstance().getName();

	private StreamTokenizer _in;

	private final KnowledgeBase _kb;

	private ArrayList<ATermAppl> _terms;

	private Map<ATermAppl, List<ATermAppl>> _disjoints;

	private boolean _forceUppercase;

	private static final int QUOTE = '|';

	public KRSSLoader()
	{
		this(new KnowledgeBaseImpl());
	}

	public KRSSLoader(final KnowledgeBase kb)
	{
		_kb = kb;
		_forceUppercase = false;
	}

	@Override
	public void clear()
	{
		_kb.clear();
	}

	public boolean isForceUppercase()
	{
		return _forceUppercase;
	}

	public void setForceUppercase(final boolean forceUppercase)
	{
		_forceUppercase = forceUppercase;
	}

	private void initTokenizer(final Reader reader)
	{
		_in = new StreamTokenizer(reader);
		_in.lowerCaseMode(false);
		_in.commentChar(';');
		_in.wordChars('/', '/');
		_in.wordChars('_', '_');
		_in.wordChars('*', '*');
		_in.wordChars('?', '?');
		_in.wordChars('%', '%');
		_in.wordChars('>', '>');
		_in.wordChars('<', '<');
		_in.wordChars('=', '=');
		_in.quoteChar(QUOTE);
	}

	private void skipNext() throws IOException
	{
		_in.nextToken();
	}

	private void skipNext(final int token) throws IOException
	{
		ATermUtils.assertTrue(token == _in.nextToken());
	}

	private void skipNext(final String token) throws IOException
	{
		_in.nextToken();
		ATermUtils.assertTrue(token.equals(_in.sval));
	}

	private boolean peekNext(final int token) throws IOException
	{
		final int next = _in.nextToken();
		_in.pushBack();
		return token == next;
	}

	private String nextString() throws IOException
	{
		_in.nextToken();

		switch (_in.ttype)
		{
			case StreamTokenizer.TT_WORD:
			case QUOTE:
				return _in.sval;
			case StreamTokenizer.TT_NUMBER:
				return String.valueOf(_in.nval);
			default:
				throw new OpenError("Expecting string found " + (char) _in.ttype);
		}
	}

	private int nextInt() throws IOException
	{
		_in.nextToken();

		return (int) _in.nval;
	}

	private String nextNumber() throws IOException
	{
		_in.nextToken();

		final String strVal = String.valueOf((long) _in.nval);
		return strVal;
	}

	private ATermAppl nextTerm() throws IOException
	{
		String token = nextString();
		if (_forceUppercase)
			token = token.toUpperCase();
		return ATermUtils.makeTermAppl(token);
	}

	private ATermAppl[] parseExprList() throws IOException
	{
		int count = 0;
		while (peekNext('('))
		{
			skipNext();
			count++;
		}

		final List<ATermAppl> terms = new ArrayList<>();
		while (true)
			if (peekNext(')'))
			{
				if (count == 0)
					break;
				skipNext();
				count--;
				if (count == 0)
					break;
			}
			else
				if (peekNext('('))
				{
					skipNext();
					count++;
				}
				else
					terms.add(parseExpr());

		return terms.toArray(new ATermAppl[terms.size()]);
	}

	private ATermAppl parseExpr() throws IOException
	{
		ATermAppl a = null;

		int token = _in.nextToken();
		String s = _in.sval;
		if (token == StreamTokenizer.TT_WORD || token == QUOTE)
		{
			if ("TOP".equalsIgnoreCase(s) || "*TOP*".equalsIgnoreCase(s) || ":TOP".equalsIgnoreCase(s))
				a = ATermUtils.TOP;
			else
				if ("BOTTOM".equalsIgnoreCase(s) || "*BOTTOM*".equalsIgnoreCase(s))
					a = ATermUtils.BOTTOM;
				else
				{
					if (_forceUppercase)
						s = s.toUpperCase();
					a = ATermUtils.makeTermAppl(s);
				}
		}
		else
			if (token == StreamTokenizer.TT_NUMBER)
				a = ATermUtils.makeTermAppl(String.valueOf(_in.nval));
			else
				if (token == ':')
				{
					s = nextString();
					if ("TOP".equalsIgnoreCase(s))
						a = ATermUtils.TOP;
					else
						if ("BOTTOM".equalsIgnoreCase(s))
							a = ATermUtils.BOTTOM;
						else
							throw new OpenError("Parse exception after ':' " + s);
				}
				else
					if (token == '(')
					{
						token = _in.nextToken();
						ATermUtils.assertTrue(token == StreamTokenizer.TT_WORD);

						s = _in.sval;
						if ("NOT".equalsIgnoreCase(s))
						{
							final ATermAppl c = parseExpr();
							a = ATermUtils.makeNot(c);

							if (ATermUtils.isPrimitive(c))
								_kb.addClass(c);
						}
						else
							if ("AND".equalsIgnoreCase(s))
							{
								ATermList list = ATermUtils.EMPTY_LIST;

								while (!peekNext(')'))
								{
									final ATermAppl c = parseExpr();

									if (ATermUtils.isPrimitive(c))
										_kb.addClass(c);
									list = list.insert(c);
								}
								a = ATermUtils.makeAnd(list);
							}
							else
								if ("OR".equalsIgnoreCase(s))
								{
									ATermList list = ATermUtils.EMPTY_LIST;

									while (!peekNext(')'))
									{
										final ATermAppl c = parseExpr();

										if (ATermUtils.isPrimitive(c))
											_kb.addClass(c);
										list = list.insert(c);
									}
									a = ATermUtils.makeOr(list);
								}
								else
									if ("ONE-OF".equalsIgnoreCase(s))
									{
										ATermList list = ATermUtils.EMPTY_LIST;

										while (!peekNext(')'))
										{
											final ATermAppl c = parseExpr();

											_kb.addIndividual(c);
											list = list.insert(ATermUtils.makeValue(c));
										}
										a = ATermUtils.makeOr(list);
									}
									else
										if ("ALL".equalsIgnoreCase(s))
										{
											final ATermAppl r = parseExpr();
											_kb.addObjectProperty(r);
											final ATermAppl c = parseExpr();
											if (ATermUtils.isPrimitive(c))
												_kb.addClass(c);

											a = ATermUtils.makeAllValues(r, c);
										}
										else
											if ("SOME".equalsIgnoreCase(s))
											{
												final ATermAppl r = parseExpr();
												_kb.addObjectProperty(r);
												final ATermAppl c = parseExpr();
												if (ATermUtils.isPrimitive(c))
													_kb.addClass(c);
												a = ATermUtils.makeSomeValues(r, c);
											}
											else
												if ("AT-LEAST".equalsIgnoreCase(s) || "ATLEAST".equalsIgnoreCase(s))
												{
													final int n = nextInt();
													final ATermAppl r = parseExpr();
													_kb.addObjectProperty(r);

													ATermAppl c = ATermUtils.TOP;
													if (!peekNext(')'))
														c = parseExpr();

													a = ATermUtils.makeMin(r, n, c);
												}
												else
													if ("AT-MOST".equalsIgnoreCase(s) || "ATMOST".equalsIgnoreCase(s))
													{
														final int n = nextInt();
														final ATermAppl r = parseExpr();
														_kb.addObjectProperty(r);

														ATermAppl c = ATermUtils.TOP;
														if (!peekNext(')'))
															c = parseExpr();

														a = ATermUtils.makeMax(r, n, c);
													}
													else
														if ("EXACTLY".equalsIgnoreCase(s))
														{
															final int n = nextInt();
															final ATermAppl r = parseExpr();
															_kb.addObjectProperty(r);

															ATermAppl c = ATermUtils.TOP;
															if (!peekNext(')'))
																c = parseExpr();

															a = ATermUtils.makeCard(r, n, c);
														}
														else
															if ("A".equalsIgnoreCase(s))
															{
																final ATermAppl r = nextTerm();
																// TODO what does term 'A' stand for
																_kb.addDatatypeProperty(r);
																_kb.addFunctionalProperty(r);
																a = ATermUtils.makeMin(r, 1, ATermUtils.TOP_LIT);
															}
															else
																if ("MIN".equalsIgnoreCase(s) || ">=".equals(s))
																{
																	final ATermAppl r = nextTerm();
																	_kb.addDatatypeProperty(r);
																	final String val = nextNumber();
																	final ATermAppl dr = ATermUtils.makeRestrictedDatatype(XSD_INTEGER, new ATermAppl[] { ATermUtils.makeFacetRestriction(Facet.XSD.MIN_INCLUSIVE.getName(), ATermUtils.makeTypedLiteral(val, XSD_INTEGER)) });
																	a = ATermUtils.makeAllValues(r, dr);
																}
																else
																	if ("MAX".equalsIgnoreCase(s) || "<=".equals(s))
																	{
																		final ATermAppl r = nextTerm();
																		_kb.addDatatypeProperty(r);
																		final String val = nextNumber();
																		final ATermAppl dr = ATermUtils.makeRestrictedDatatype(XSD_INTEGER, new ATermAppl[] { ATermUtils.makeFacetRestriction(Facet.XSD.MAX_INCLUSIVE.getName(), ATermUtils.makeTypedLiteral(val, XSD_INTEGER)) });
																		a = ATermUtils.makeAllValues(r, dr);
																	}
																	else
																		if ("=".equals(s))
																		{
																			final ATermAppl r = nextTerm();
																			_kb.addDatatypeProperty(r);
																			final String val = nextNumber();
																			final ATermAppl dr = ATermUtils.makeOr(ATermUtils.makeList(ATermUtils.makeValue(ATermUtils.makeTypedLiteral(val, XSD_INTEGER))));
																			a = ATermUtils.makeAllValues(r, dr);
																		}
																		else
																			if ("INV".equalsIgnoreCase(s))
																			{
																				final ATermAppl r = parseExpr();
																				_kb.addObjectProperty(r);
																				a = _kb.getProperty(r).getInverse().getName();
																			}
																			else
																				throw new OpenError("Unknown expression " + s);

						if (_in.nextToken() != ')')
							// if( s.equalsIgnoreCase( "AT-LEAST" ) || s.equalsIgnoreCase(
							// "AT-MOST" )
							// || s.equalsIgnoreCase( "ATLEAST" ) || s.equalsIgnoreCase(
							// "ATMOST" ) ) {
							// s = nextString();
							// if( s.equalsIgnoreCase( "TOP" ) || s.equalsIgnoreCase(
							// "*TOP*" )
							// || s.equalsIgnoreCase( ":TOP" ) )
							// skipNext( ')' );
							// else
							// throw new UnsupportedFeatureException( "Qualified cardinality
							// restrictions" );
							// }
							// else
							throw new OpenError("Parse exception at term " + s);
					}
					else
						if (token == '#')
						{
							final int n = nextInt();
							if (peekNext('#'))
							{
								skipNext();
								a = _terms.get(n);
								if (a == null)
									throw new OpenError("Parse exception: #" + n + "# is not defined");
							}
							else
							{
								skipNext("=");
								a = parseExpr();

								while (_terms.size() <= n)
									_terms.add(null);

								_terms.set(n, a);
							}
						}
						else
							if (token != StreamTokenizer.TT_EOF)
								throw new OpenError("Invalid token");

		return a;
	}

	@Override
	public void parseFile(final String fileURI)
	{
		try (InputStreamReader reader = new InputStreamReader(URI.create(fileURI).toURL().openStream()))
		{
			parse(reader);
		}
		catch (final Exception e)
		{
			throw new OpenError(e);
		}
	}

	public void parse(final Reader reader) throws IOException
	{
		initTokenizer(reader);

		_terms = new ArrayList<>();
		_disjoints = new HashMap<>();

		int token = _in.nextToken();
		while (token != StreamTokenizer.TT_EOF)
		{
			if (token == '#')
			{
				_in.ordinaryChar(QUOTE);
				token = _in.nextToken();
				while (token != '#')
					token = _in.nextToken();
				_in.quoteChar(QUOTE);
				token = _in.nextToken();
				if (token == StreamTokenizer.TT_EOF)
					break;
			}
			if (token != '(')
				throw new OpenError("Parsing error: Expecting '(' but found " + _in);

			final String str = nextString();
			if (str.equalsIgnoreCase("DEFINE-ROLE") || str.equalsIgnoreCase("DEFINE-PRIMITIVE-ROLE") || str.equalsIgnoreCase("DEFPRIMROLE") || str.equalsIgnoreCase("DEFINE-ATTRIBUTE") || str.equalsIgnoreCase("DEFINE-PRIMITIVE-ATTRIBUTE") || str.equalsIgnoreCase("DEFPRIMATTRIBUTE") || str.equalsIgnoreCase("DEFINE-DATATYPE-PROPERTY"))
			{
				final ATermAppl r = nextTerm();

				final boolean dataProp = str.equalsIgnoreCase("DEFINE-DATATYPE-PROPERTY");
				final boolean functional = str.equalsIgnoreCase("DEFINE-PRIMITIVE-ATTRIBUTE") || str.equalsIgnoreCase("DEFPRIMATTRIBUTE");
				final boolean primDef = str.indexOf("PRIm") != -1;

				if (dataProp)
				{
					_kb.addDatatypeProperty(r);
					if (_logger.isLoggable(Level.FINE))
						_logger.fine("DEFINE-DATATYPE-ROLE " + r);
				}
				else
				{
					_kb.addObjectProperty(r);
					if (functional)
					{
						_kb.addFunctionalProperty(r);
						if (_logger.isLoggable(Level.FINE))
							_logger.fine("DEFINE-PRIMITIVE-ATTRIBUTE " + r);
					}
					else
						if (_logger.isLoggable(Level.FINE))
							_logger.fine("DEFINE-PRIMITIVE-ROLE " + r);
				}

				while (!peekNext(')'))
					if (peekNext(':'))
					{
						skipNext(':');
						final String cmd = nextString();
						if (cmd.equalsIgnoreCase("parents"))
						{
							final boolean paren = peekNext('(');
							if (paren)
							{
								skipNext('(');
								while (!peekNext(')'))
								{
									final ATermAppl s = nextTerm();
									if (!s.getName().equals("NIL"))
									{
										_kb.addObjectProperty(s);
										_kb.addSubProperty(r, s);
										if (_logger.isLoggable(Level.FINE))
											_logger.fine("PARENT-ROLE " + r + " " + s);
									}
								}
								skipNext(')');
							}
							else
							{
								final ATermAppl s = nextTerm();
								if (!s.toString().equalsIgnoreCase("NIL"))
								{
									_kb.addObjectProperty(s);
									_kb.addSubProperty(r, s);
									if (_logger.isLoggable(Level.FINE))
										_logger.fine("PARENT-ROLE " + r + " " + s);
								}
							}
						}
						else
							if (cmd.equalsIgnoreCase("feature"))
							{
								ATermUtils.assertTrue(nextString().equalsIgnoreCase("T"));
								_kb.addFunctionalProperty(r);
								_logger.fine(() -> "FUNCTIONAL-ROLE " + r);
							}
							else
								if (cmd.equalsIgnoreCase("transitive"))
								{
									ATermUtils.assertTrue(nextString().equalsIgnoreCase("T"));
									_kb.addTransitiveProperty(r);
									_logger.fine(() -> "TRANSITIVE-ROLE " + r);
								}
								else
									if (cmd.equalsIgnoreCase("range"))
									{
										final ATermAppl range = parseExpr();
										_kb.addClass(range);
										_kb.addRange(r, range);
										_logger.fine(() -> "RANGE " + r + " " + range);
									}
									else
										if (cmd.equalsIgnoreCase("domain"))
										{
											final ATermAppl domain = parseExpr();
											_kb.addClass(domain);
											_kb.addDomain(r, domain);
											_logger.fine(() -> "DOMAIN " + r + " " + domain);
										}
										else
											if (cmd.equalsIgnoreCase("inverse"))
											{
												final ATermAppl inv = nextTerm();
												_kb.addInverseProperty(r, inv);
												_logger.fine(() -> "INVERSE " + r + " " + inv);
											}
											else
												throw new OpenError("Parsing error: Unrecognized keyword _in role definition " + cmd);
					}
					else
						if (peekNext('('))
						{
							skipNext('(');
							final String cmd = nextString();
							if (cmd.equalsIgnoreCase("domain-range"))
							{
								final ATermAppl domain = nextTerm();
								final ATermAppl range = nextTerm();

								_kb.addDomain(r, domain);
								_kb.addRange(r, range);
								_logger.fine(() -> "DOMAIN-RANGE " + r + " " + domain + " " + range);
							}
							else
								throw new OpenError("Parsing error: Unrecognized keyword _in role definition");
							skipNext(')');
						}
						else
						{
							final ATermAppl s = parseExpr();

							if (dataProp)
								_kb.addDatatypeProperty(s);
							else
								_kb.addObjectProperty(r);

							if (primDef)
								_kb.addSubProperty(r, s);
							else
								_kb.addEquivalentProperty(r, s);

							_logger.fine("PARENT-ROLE " + r + " " + s);
						}
			}
			else
				if ("DEFINE-PRIMITIVE-CONCEPT".equalsIgnoreCase(str) || "DEFPRIMCONCEPT".equalsIgnoreCase(str))
				{
					final ATermAppl c = nextTerm();
					_kb.addClass(c);

					ATermAppl expr = null;
					if (!peekNext(')'))
					{
						expr = parseExpr();

						if (!expr.getName().equals("NIL"))
						{
							_kb.addClass(expr);
							_kb.addSubClass(c, expr);
						}
					}

					if (_logger.isLoggable(Level.FINE))
						_logger.fine("DEFINE-PRIMITIVE-CONCEPT " + c + " " + (expr == null ? "" : expr.toString()));
				}
				else
					if (str.equalsIgnoreCase("DEFINE-DISJOINT-PRIMITIVE-CONCEPT"))
					{
						final ATermAppl c = nextTerm();
						_kb.addClass(c);

						skipNext('(');
						while (!peekNext(')'))
						{
							final ATermAppl expr = parseExpr();

							List<ATermAppl> prevDefinitions = _disjoints.get(expr);
							if (prevDefinitions == null)
								prevDefinitions = new ArrayList<>();
							for (final ATermAppl d : prevDefinitions)
							{
								_kb.addDisjointClass(c, d);
								_logger.fine(() -> "DEFINE-PRIMITIVE-DISJOINT " + c + " " + d);
							}
							prevDefinitions.add(c);
						}
						skipNext(')');

						final ATermAppl expr = parseExpr();
						_kb.addSubClass(c, expr);

						_logger.fine(() -> "DEFINE-PRIMITIVE-CONCEPT " + c + " " + expr);
					}
					else
						if (str.equalsIgnoreCase("DEFINE-CONCEPT") || str.equalsIgnoreCase("DEFCONCEPT") || str.equalsIgnoreCase("EQUAL_C"))
						{
							final ATermAppl c = nextTerm();
							_kb.addClass(c);

							final ATermAppl expr = parseExpr();
							_kb.addEquivalentClass(c, expr);

							_logger.fine(() -> "DEFINE-CONCEPT " + c + " " + expr);
						}
						else
							if (str.equalsIgnoreCase("IMPLIES") || str.equalsIgnoreCase("IMPLIES_C"))
							{
								final ATermAppl c1 = parseExpr();
								final ATermAppl c2 = parseExpr();
								_kb.addClass(c1);
								_kb.addClass(c2);
								_kb.addSubClass(c1, c2);

								_logger.fine(() -> "IMPLIES " + c1 + " " + c2);
							}
							else
								if (str.equalsIgnoreCase("IMPLIES_R"))
								{
									final ATermAppl p1 = parseExpr();
									final ATermAppl p2 = parseExpr();
									_kb.addProperty(p1);
									_kb.addProperty(p2);
									_kb.addSubProperty(p1, p2);

									_logger.fine(() -> "IMPLIES_R " + p1 + " " + p2);
								}
								else
									if (str.equalsIgnoreCase("EQUAL_R"))
									{
										final ATermAppl p1 = parseExpr();
										final ATermAppl p2 = parseExpr();
										_kb.addObjectProperty(p1);
										_kb.addObjectProperty(p2);
										_kb.addEquivalentProperty(p1, p2);

										_logger.fine(() -> "EQUAL_R " + p1 + " " + p2);
									}
									else
										if (str.equalsIgnoreCase("DOMAIN"))
										{
											final ATermAppl p = parseExpr();
											final ATermAppl c = parseExpr();
											_kb.addProperty(p);
											_kb.addClass(c);
											_kb.addDomain(p, c);

											_logger.fine(() -> "DOMAIN " + p + " " + c);
										}
										else
											if (str.equalsIgnoreCase("RANGE"))
											{
												final ATermAppl p = parseExpr();
												final ATermAppl c = parseExpr();
												_kb.addProperty(p);
												_kb.addClass(c);
												_kb.addRange(p, c);

												_logger.fine(() -> "RANGE " + p + " " + c);
											}
											else
												if (str.equalsIgnoreCase("FUNCTIONAL"))
												{
													final ATermAppl p = parseExpr();
													_kb.addProperty(p);
													_kb.addFunctionalProperty(p);

													_logger.fine(() -> "FUNCTIONAL " + p);
												}
												else
													if (str.equalsIgnoreCase("TRANSITIVE"))
													{
														final ATermAppl p = parseExpr();
														_kb.addObjectProperty(p);
														_kb.addTransitiveProperty(p);

														_logger.fine(() -> "TRANSITIVE " + p);
													}
													else
														if (str.equalsIgnoreCase("DISJOINT"))
														{
															final ATermAppl[] list = parseExprList();
															for (int i = 0; i < list.length - 1; i++)
															{
																final ATermAppl c1 = list[i];
																for (int j = i + 1; j < list.length; j++)
																{
																	final ATermAppl c2 = list[j];
																	_kb.addClass(c2);
																	_kb.addDisjointClass(c1, c2);
																	_logger.fine(() -> "DISJOINT " + c1 + " " + c2);
																}
															}
														}
														else
															if (str.equalsIgnoreCase("DEFINDIVIDUAL"))
															{
																final ATermAppl x = nextTerm();

																_kb.addIndividual(x);
																_logger.fine(() -> "DEFINDIVIDUAL " + x);
															}
															else
																if (str.equalsIgnoreCase("INSTANCE"))
																{
																	final ATermAppl x = nextTerm();
																	final ATermAppl c = parseExpr();

																	_kb.addIndividual(x);
																	_kb.addType(x, c);
																	_logger.fine(() -> "INSTANCE " + x + " " + c);
																}
																else
																	if (str.equalsIgnoreCase("RELATED"))
																	{
																		final ATermAppl x = nextTerm();
																		final ATermAppl y = nextTerm();
																		final ATermAppl r = nextTerm();

																		_kb.addIndividual(x);
																		_kb.addIndividual(y);
																		_kb.addPropertyValue(r, x, y);

																		_logger.fine(() -> "RELATED " + x + " - " + r + " -> " + y);
																	}
																	else
																		if (str.equalsIgnoreCase("DIFFERENT"))
																		{
																			final ATermAppl x = nextTerm();
																			final ATermAppl y = nextTerm();

																			_kb.addIndividual(x);
																			_kb.addIndividual(y);
																			_kb.addDifferent(x, y);

																			_logger.fine(() -> "DIFFERENT " + x + " " + y);
																		}
																		else
																			if (str.equalsIgnoreCase("DATATYPE-ROLE-FILLER"))
																			{
																				final ATermAppl x = nextTerm();
																				final ATermAppl y = ATermUtils.makePlainLiteral(nextString());
																				final ATermAppl r = nextTerm();

																				_kb.addIndividual(x);
																				_kb.addIndividual(y);
																				_kb.addPropertyValue(r, x, y);

																				_logger.fine(() -> "DATATYPE-ROLE-FILLER " + x + " - " + r + " -> " + y);
																			}
																			else
																				throw new OpenError("Parsing error: Unknown command " + str);
			skipNext(')');

			token = _in.nextToken();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIgnoreImports(final boolean ignoreImports)
	{
		// Nothing to do.
	}

	public void verifyTBox(final String file, final KnowledgeBase kb) throws Exception
	{
		try (FileReader reader = new FileReader(file))
		{

			initTokenizer(reader);

			boolean failed = false;
			int verifiedCount = 0;
			int token = _in.nextToken();
			while (token != ')' && token != StreamTokenizer.TT_EOF)
			{
				ATermUtils.assertTrue(token == '(');

				verifiedCount++;

				ATermAppl c = null;
				if (peekNext('('))
				{
					final ATermAppl[] list = parseExprList();
					c = list[0];
					final Set<ATermAppl> eqs = kb.getEquivalentClasses(c);
					for (int i = 1; i < list.length; i++)
					{
						final ATermAppl t = list[i];

						if (!eqs.contains(t))
						{
							_logger.severe(t + " is not equivalent to " + c);
							failed = true;
						}
					}
				}
				else
					c = parseExpr();

				final Set<ATermAppl> supers = SetUtils.union(kb.getSuperClasses(c, true));
				final Set<ATermAppl> subs = SetUtils.union(kb.getSubClasses(c, true));

				if (_logger.isLoggable(Level.FINE))
					_logger.fine("Verify (" + verifiedCount + ") " + c + " " + supers + " " + subs);

				if (peekNext('('))
				{
					final ATermAppl[] terms = parseExprList();
					for (final ATermAppl term : terms)
					{
						final ATerm t = term;

						if (!supers.contains(t))
						{
							_logger.severe(t + " is not a superclass of " + c + " ");
							failed = true;
						}
					}
				}
				else
					skipNext();

				if (peekNext('('))
				{
					final ATermAppl[] terms = parseExprList();
					for (final ATermAppl term : terms)
					{
						final ATermAppl t = term;

						if (!subs.contains(t))
						{
							final Set<ATermAppl> temp = new HashSet<>(subs);
							final Set<ATermAppl> sames = kb.getEquivalentClasses(t);
							temp.retainAll(sames);
							if (temp.size() == 0)
							{
								_logger.severe(t + " is not a subclass of " + c);
								failed = true;
							}
						}
					}
				}

				skipNext();

				token = _in.nextToken();
			}

			ATermUtils.assertTrue(_in.nextToken() == StreamTokenizer.TT_EOF);

			if (failed)
				throw new OpenError("Classification results are not correct!");
		}
	}

	public void verifyABox(final String file, final KnowledgeBase kb) throws Exception
	{
		try (FileReader reader = new FileReader(file))
		{
			initTokenizer(reader);

			final boolean longFormat = !peekNext('(');

			while (!peekNext(StreamTokenizer.TT_EOF))
			{
				if (longFormat)
				{
					skipNext("Command");
					skipNext('=');
				}

				skipNext('(');
				skipNext("INDIVIDUAL-INSTANCE?");

				final ATermAppl ind = nextTerm();
				final ATermAppl c = parseExpr();

				_logger.fine(() -> "INDIVIDUAL-INSTANCE? " + ind + " " + c);

				skipNext(')');

				boolean isType;
				if (longFormat)
				{
					skipNext('-');
					skipNext('>');
					final String result = nextString();
					if (result.equalsIgnoreCase("T"))
						isType = true;
					else
						if (result.equalsIgnoreCase("NIL"))
							isType = false;
						else
							throw new OpenError("Unknown result " + result);
				}
				else
					isType = true;

				_logger.fine(() -> " -> " + isType);

				if (kb.isType(ind, c) != isType)
					throw new OpenError("Individual " + ind + " is " + (isType ? "not" : "") + " an instance of " + c);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public KnowledgeBase getKB()
	{
		return _kb;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void load()
	{
		// nothing to do here since we load to the KB directly
	}
}
