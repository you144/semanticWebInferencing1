package openllet.owlwg.cli;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.owlwg.testcase.Semantics;
import openllet.owlwg.testcase.SyntaxConstraint;
import openllet.owlwg.testcase.filter.ConjunctionFilter;
import openllet.owlwg.testcase.filter.DisjunctionFilter;
import openllet.owlwg.testcase.filter.FilterCondition;
import openllet.owlwg.testcase.filter.NegationFilter;
import openllet.owlwg.testcase.filter.SatisfiedSyntaxConstraintFilter;
import openllet.owlwg.testcase.filter.SemanticsFilter;
import openllet.owlwg.testcase.filter.StatusFilter;
import openllet.owlwg.testcase.filter.UnsatisfiedSyntaxConstraintFilter;
import openllet.shared.tools.Log;

/**
 * <p>
 * Title: Filter Condition Parser
 * </p>
 * <p>
 * Description: Create a filter _condition from a string
 * </p>
 * <p>
 * Copyright: Copyright &copy; 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <a href="http://clarkparsia.com/"/>http://clarkparsia.com/</a>
 * </p>
 * 
 * @author Mike Smith &lt;msmith@clarkparsia.com&gt;
 */
public class FilterConditionParser
{
	private static final Logger _logger = Log.getLogger(FilterConditionParser.class);

	public static FilterCondition parse(String filterString)
	{

		FilterCondition filter;
		final LinkedList<FilterCondition> filterStack = new LinkedList<>();
		final String[] splits = filterString.split("\\s");
		for (int i = 0; i < splits.length; i++)
		{
			if (splits[i].equalsIgnoreCase("and"))
			{
				final ConjunctionFilter and = ConjunctionFilter.and(filterStack);
				filterStack.clear();
				filterStack.add(and);
			}
			else
				if (splits[i].equalsIgnoreCase("approved"))
				{
					filterStack.add(StatusFilter.APPROVED);
				}
				else
					if (splits[i].equalsIgnoreCase("direct"))
					{
						filterStack.add(new SemanticsFilter(Semantics.DIRECT));
					}
					else
						if (splits[i].equalsIgnoreCase("dl"))
						{
							filterStack.add(SatisfiedSyntaxConstraintFilter.DL);
						}
						else
							if (splits[i].equalsIgnoreCase("!dl"))
							{
								filterStack.add(new UnsatisfiedSyntaxConstraintFilter(SyntaxConstraint.DL));
							}
							else
								if (splits[i].equalsIgnoreCase("el"))
								{
									filterStack.add(SatisfiedSyntaxConstraintFilter.EL);
								}
								else
									if (splits[i].equalsIgnoreCase("!el"))
									{
										filterStack.add(new UnsatisfiedSyntaxConstraintFilter(SyntaxConstraint.EL));
									}
									else
										if (splits[i].equalsIgnoreCase("extracredit"))
										{
											filterStack.add(StatusFilter.EXTRACREDIT);
										}
										else
											if (splits[i].equalsIgnoreCase("not"))
											{
												final FilterCondition a = filterStack.removeLast();
												filterStack.add(NegationFilter.not(a));
											}
											else
												if (splits[i].equalsIgnoreCase("or"))
												{
													final DisjunctionFilter or = DisjunctionFilter.or(filterStack);
													filterStack.clear();
													filterStack.add(or);
												}
												else
													if (splits[i].equalsIgnoreCase("proposed"))
													{
														filterStack.add(StatusFilter.PROPOSED);
													}
													else
														if (splits[i].equalsIgnoreCase("ql"))
														{
															filterStack.add(SatisfiedSyntaxConstraintFilter.QL);
														}
														else
															if (splits[i].equalsIgnoreCase("!ql"))
															{
																filterStack.add(new UnsatisfiedSyntaxConstraintFilter(SyntaxConstraint.QL));
															}
															else
																if (splits[i].equalsIgnoreCase("rdf"))
																{
																	filterStack.add(new SemanticsFilter(Semantics.RDF));
																}
																else
																	if (splits[i].equalsIgnoreCase("rejected"))
																	{
																		filterStack.add(StatusFilter.REJECTED);
																	}
																	else
																		if (splits[i].equalsIgnoreCase("rl"))
																		{
																			filterStack.add(SatisfiedSyntaxConstraintFilter.RL);
																		}
																		else
																			if (splits[i].equalsIgnoreCase("!rl"))
																			{
																				filterStack.add(new UnsatisfiedSyntaxConstraintFilter(SyntaxConstraint.RL));
																			}
																			else
																			{
																				final String msg = format("Unexpected filter _condition argument: \"%s\"", splits[i]);
																				_logger.severe(msg);
																				throw new IllegalArgumentException(msg);
																			}
		}
		if (filterStack.isEmpty())
		{
			final String msg = format("Missing valid filter _condition. Filter option argument: \"%s\"", filterString);
			_logger.severe(msg);
			throw new IllegalArgumentException(msg);
		}
		if (filterStack.size() > 1)
		{
			final String msg = format("Filter conditions do not parse to a single _condition. Final parse stack: \"%s\"", filterStack);
			_logger.severe(msg);
			throw new IllegalArgumentException(msg);
		}

		filter = filterStack.iterator().next();
		if (_logger.isLoggable(Level.FINE))
			_logger.fine(format("Filter _condition: \"%s\"", filter));
		return filter;
	}

}
