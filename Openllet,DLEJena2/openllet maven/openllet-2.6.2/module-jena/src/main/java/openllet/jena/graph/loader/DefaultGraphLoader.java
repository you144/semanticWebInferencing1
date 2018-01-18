// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC.
// <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms
// of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under
// the terms of the MIT License.
//
// The MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package openllet.jena.graph.loader;

import static openllet.jena.graph.loader.SimpleProperty.ANTI_SYM;
import static openllet.jena.graph.loader.SimpleProperty.CARDINALITY;
import static openllet.jena.graph.loader.SimpleProperty.DISJOINT;
import static openllet.jena.graph.loader.SimpleProperty.IRREFLEXIVE;
import static openllet.jena.graph.loader.SimpleProperty.SELF;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.KnowledgeBase;
import openllet.core.OpenlletOptions;
import openllet.core.PropertyType;
import openllet.core.boxes.rbox.Role;
import openllet.core.boxes.rbox.RoleImpl;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.exceptions.UnsupportedFeatureException;
import openllet.core.rules.model.AtomDConstant;
import openllet.core.rules.model.AtomDObject;
import openllet.core.rules.model.AtomDVariable;
import openllet.core.rules.model.AtomIConstant;
import openllet.core.rules.model.AtomIObject;
import openllet.core.rules.model.AtomIVariable;
import openllet.core.rules.model.BuiltInAtom;
import openllet.core.rules.model.ClassAtom;
import openllet.core.rules.model.DataRangeAtom;
import openllet.core.rules.model.DatavaluedPropertyAtom;
import openllet.core.rules.model.DifferentIndividualsAtom;
import openllet.core.rules.model.IndividualPropertyAtom;
import openllet.core.rules.model.Rule;
import openllet.core.rules.model.RuleAtom;
import openllet.core.rules.model.SameIndividualAtom;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.AnnotationClasses;
import openllet.core.utils.Bool;
import openllet.core.utils.QNameProvider;
import openllet.core.utils.SetUtils;
import openllet.core.utils.Timer;
import openllet.core.utils.progress.ProgressMonitor;
import openllet.core.utils.progress.SilentProgressMonitor;
import openllet.core.vocabulary.BuiltinNamespace;
import openllet.jena.BuiltinTerm;
import openllet.jena.JenaUtils;
import openllet.jena.vocabulary.OWL2;
import openllet.jena.vocabulary.SWRL;
import openllet.shared.tools.Log;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class DefaultGraphLoader implements GraphLoader
{

	public static final Logger _logger = Log.getLogger(DefaultGraphLoader.class);

	protected static final Node[] TBOX_TYPES;
	protected static final Node[] TBOX_PREDICATES;

	static
	{
		final ArrayList<Node> predicates = new ArrayList<>();
		final ArrayList<Node> types = new ArrayList<>();

		for (final BuiltinTerm builtinTerm : BuiltinTerm.values())
		{
			if (builtinTerm.isABox() || builtinTerm.isSyntax())
				continue;

			if (builtinTerm.isPredicate())
				predicates.add(builtinTerm.getNode());
			else
				types.add(builtinTerm.getNode());
		}

		TBOX_PREDICATES = predicates.toArray(new Node[predicates.size()]);
		TBOX_TYPES = types.toArray(new Node[types.size()]);
	}

	private static final EnumSet<BuiltinTerm> OWL_MEMBERS_TYPES = EnumSet.of(BuiltinTerm.OWL_AllDifferent, BuiltinTerm.OWL2_AllDisjointClasses, BuiltinTerm.OWL2_AllDisjointProperties);

	private static final Graph EMPTY_GRAPH = Factory.createGraphMem();

	public static QNameProvider _qnames = new QNameProvider();

	protected KnowledgeBase _kb;

	protected Graph _graph;

	protected Map<Node, ATermAppl> _terms;

	protected Map<Node, ATermList> _lists;

	protected Set<Node> _anonDatatypes;

	protected Map<Node, BuiltinTerm> _naryDisjoints;

	private Map<ATermAppl, SimpleProperty> _simpleProperties;

	private Set<String> _unsupportedFeatures;

	private boolean _loadABox = true;

	private boolean _loadTBox = true;

	private boolean _preprocessTypeTriples = true;

	protected ProgressMonitor _monitor = new SilentProgressMonitor();

	public DefaultGraphLoader()
	{
		clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProgressMonitor(final ProgressMonitor monitor)
	{
		if (monitor == null)
			_monitor = new SilentProgressMonitor();
		else
			_monitor = monitor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setGraph(final Graph graph)
	{
		_graph = graph;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Graph getGraph()
	{
		return _graph;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getUnpportedFeatures()
	{
		return _unsupportedFeatures;
	}

	protected void addSimpleProperty(final ATermAppl p, final SimpleProperty why)
	{
		_simpleProperties.put(p, why);
		final Role role = _kb.getRBox().getRole(p);
		role.setForceSimple(true);
	}

	protected void addUnsupportedFeature(final String msg)
	{
		if (!OpenlletOptions.IGNORE_UNSUPPORTED_AXIOMS)
			throw new UnsupportedFeatureException(msg);

		if (_unsupportedFeatures.add(msg))
			_logger.warning("Unsupported axiom: " + msg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear()
	{
		_terms = new HashMap<>();
		_terms.put(OWL.Thing.asNode(), ATermUtils.TOP);
		_terms.put(OWL.Nothing.asNode(), ATermUtils.BOTTOM);
		_terms.put(OWL2.topDataProperty.asNode(), ATermUtils.TOP_DATA_PROPERTY);
		_terms.put(OWL2.bottomDataProperty.asNode(), ATermUtils.BOTTOM_DATA_PROPERTY);
		_terms.put(OWL2.topObjectProperty.asNode(), ATermUtils.TOP_OBJECT_PROPERTY);
		_terms.put(OWL2.bottomObjectProperty.asNode(), ATermUtils.BOTTOM_OBJECT_PROPERTY);

		_lists = new HashMap<>();
		_lists.put(RDF.nil.asNode(), ATermUtils.EMPTY_LIST);

		_anonDatatypes = new HashSet<>();

		_simpleProperties = new HashMap<>();

		_unsupportedFeatures = new HashSet<>();

		_naryDisjoints = new HashMap<>();
	}

	private Node getObject(final Node subj, final Node pred)
	{
		final ClosableIterator<Triple> i = _graph.find(subj, pred, null);

		if (i.hasNext())
		{
			final Triple triple = i.next();
			i.close();
			return triple.getObject();
		}

		return null;
	}

	private boolean hasObject(final Node subj, final Node pred, final Node obj)
	{
		return _graph.contains(subj, pred, obj);
	}

	private class RDFListIterator implements Iterator<Node>
	{
		private Node _list;

		public RDFListIterator(final Node list)
		{
			_list = list;
		}

		@Override
		public boolean hasNext()
		{
			return !_list.equals(RDF.nil.asNode());
		}

		@Override
		public Node next()
		{
			final Node first = getFirst(_list);
			_monitor.incrementProgress();
			final Node rest = getRest(_list);
			_monitor.incrementProgress();

			if (first == null || rest == null)
			{
				addUnsupportedFeature("Invalid _list structure: List " + _list + " does not have a " + (first == null ? "rdf:first" : "rdf:rest") + " property. Ignoring rest of the _list.");
				return null;
			}

			_list = rest;

			return first;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	protected Node getFirst(final Node list)
	{
		return getObject(list, RDF.first.asNode());
	}

	protected Node getRest(final Node list)
	{
		return getObject(list, RDF.rest.asNode());
	}

	protected ATermList createList(final Node node)
	{
		if (_lists.containsKey(node))
			return _lists.get(node);

		final ATermList list = createList(new RDFListIterator(node));

		_lists.put(node, list);

		return list;
	}

	protected ATermList createList(final RDFListIterator i)
	{
		if (!i.hasNext())
			return ATermUtils.EMPTY_LIST;

		final Node node = i.next();
		if (node == null)
			return ATermUtils.EMPTY_LIST;

		final ATermAppl first = node2term(node);
		final ATermList rest = createList(i);
		final ATermList list = ATermUtils.makeList(first, rest);

		return list;
	}

	protected boolean isRestriction(final Node node)
	{
		return getObject(node, OWL.onProperty.asNode()) != null;
	}

	@SuppressWarnings("deprecation")
	// OWL2.SelfRestriction is deprecated but we have control over it.
	protected ATermAppl createRestriction(final Node node) throws UnsupportedFeatureException
	{
		Node restrictionType = null;
		Node p = null;
		Node filler = null;
		Node qualification = null;
		Bool isObjectRestriction = Bool.UNKNOWN;

		final ClosableIterator<Triple> i = _graph.find(node, null, null);
		while (i.hasNext())
		{
			_monitor.incrementProgress();

			final Triple t = i.next();
			final Node pred = t.getPredicate();
			final BuiltinTerm builtinTerm = BuiltinTerm.find(pred);
			if (builtinTerm == null)
				continue;

			switch (builtinTerm)
			{
				case OWL_someValuesFrom:
				case OWL_allValuesFrom:
				case OWL_cardinality:
				case OWL_minCardinality:
				case OWL_maxCardinality:
				case OWL_hasValue:
				case OWL2_hasSelf:
				case OWL2_qualifiedCardinality:
				case OWL2_minQualifiedCardinality:
				case OWL2_maxQualifiedCardinality:
					restrictionType = pred;
					filler = t.getObject();
					break;

				case OWL_onProperty:
					p = t.getObject();
					break;

				case RDF_type:
					if (t.getObject().equals(OWL2.SelfRestriction.asNode()))
					{
						restrictionType = OWL2.hasSelf.asNode();
						filler = JenaUtils.XSD_BOOLEAN_TRUE.asNode();
					}
					break;

				case OWL2_onClass:
					isObjectRestriction = Bool.TRUE;
					qualification = t.getObject();
					break;

				case OWL2_onDataRange:
					isObjectRestriction = Bool.FALSE;
					qualification = t.getObject();
					break;

				default:
					break;

			}
		}
		i.close();

		return createRestriction(restrictionType, p, filler, qualification, isObjectRestriction);
	}

	protected ATermAppl createRestriction(final Node restrictionType, final Node p, final Node filler, final Node qualification, final Bool isObjectRestriction) throws UnsupportedFeatureException
	{
		ATermAppl aTerm = ATermUtils.TOP;

		if (restrictionType == null || filler == null)
		{
			addUnsupportedFeature("Skipping invalid restriction");
			return aTerm;
		}

		final ATermAppl pt = node2term(p);

		if (restrictionType.equals(OWL2.hasSelf.asNode()))
		{
			Object value = null;
			try
			{
				value = _kb.getDatatypeReasoner().getValue(node2term(filler));
			}
			catch (final Exception e)
			{
				_logger.log(Level.FINE, "Invalid hasSelf value: " + filler, e);
			}

			if (Boolean.TRUE.equals(value))
			{
				aTerm = ATermUtils.makeSelf(pt);
				defineObjectProperty(pt);
				addSimpleProperty(pt, SELF);
			}
			else
				addUnsupportedFeature("Invalid value for " + OWL2.hasSelf.getLocalName() + " restriction. Expecting \"true\"^^xsd:boolean but found: " + filler);
		}
		else
			if (restrictionType.equals(OWL.hasValue.asNode()))
			{
				final ATermAppl ot = node2term(filler);

				if (filler.isLiteral())
					defineDatatypeProperty(pt);
				else
				{
					defineObjectProperty(pt);
					defineIndividual(ot);
				}

				aTerm = ATermUtils.makeHasValue(pt, ot);
			}
			else
				if (restrictionType.equals(OWL.allValuesFrom.asNode()))
				{
					final ATermAppl ot = node2term(filler);

					if (_kb.isClass(ot))
						defineObjectProperty(pt);
					else
						if (_kb.isDatatype(ot))
							defineDatatypeProperty(pt);

					aTerm = ATermUtils.makeAllValues(pt, ot);
				}
				else
					if (restrictionType.equals(OWL.someValuesFrom.asNode()))
					{
						final ATermAppl ot = node2term(filler);

						if (_kb.isClass(ot))
							defineObjectProperty(pt);
						else
							if (_kb.isDatatype(ot))
								defineDatatypeProperty(pt);

						aTerm = ATermUtils.makeSomeValues(pt, ot);
					}
					else
						if (restrictionType.equals(OWL.minCardinality.asNode()) || restrictionType.equals(OWL.maxCardinality.asNode()) || restrictionType.equals(OWL.cardinality.asNode()) || restrictionType.equals(OWL2.minQualifiedCardinality.asNode()) || restrictionType.equals(OWL2.maxQualifiedCardinality.asNode()) || restrictionType.equals(OWL2.qualifiedCardinality.asNode()))
							try
							{
								ATermAppl c = null;
								if (isObjectRestriction.isTrue())
								{
									c = node2term(qualification);
									defineObjectProperty(pt);
								}
								else
									if (isObjectRestriction.isFalse())
									{
										c = node2term(qualification);
										defineDatatypeProperty(pt);
									}
									else
									{
										final PropertyType propType = _kb.getPropertyType(pt);
										if (propType == PropertyType.OBJECT)
											c = ATermUtils.TOP;
										else
											if (propType == PropertyType.DATATYPE)
												c = ATermUtils.TOP_LIT;
											else
											{
												defineObjectProperty(pt);
												c = ATermUtils.TOP;
											}
									}

								final int cardinality = Integer.parseInt(filler.getLiteral().getLexicalForm().trim());

								if (restrictionType.equals(OWL.minCardinality.asNode()) || restrictionType.equals(OWL2.minQualifiedCardinality.asNode()))
									aTerm = ATermUtils.makeMin(pt, cardinality, c);
								else
									if (restrictionType.equals(OWL.maxCardinality.asNode()) || restrictionType.equals(OWL2.maxQualifiedCardinality.asNode()))
										aTerm = ATermUtils.makeMax(pt, cardinality, c);
									else
										aTerm = ATermUtils.makeCard(pt, cardinality, c);

								addSimpleProperty(pt, CARDINALITY);
							}
							catch (final Exception ex)
							{
								addUnsupportedFeature("Invalid value for the owl:" + restrictionType.getLocalName() + " restriction: " + filler);
								_logger.log(Level.WARNING, "Invalid cardinality", ex);
							}
						else
							addUnsupportedFeature("Ignoring invalid restriction on " + p);

		return aTerm;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	// OWL2.SelfRestriction is deprecated but we have control over it.
	@Override
	public ATermAppl node2term(final Node node)
	{
		ATermAppl aTerm = _terms.get(node);

		if (aTerm == null)
		{
			boolean canCache = true;
			if (isRestriction(node))
				aTerm = createRestriction(node);
			else
				if (node.isBlank())
				{
					final Triple expr = getExpression(node);
					if (expr != null)
					{
						final Node exprType = expr.getPredicate();
						final Node exprValue = expr.getObject();

						if (exprType.equals(OWL.intersectionOf.asNode()))
						{
							final ATermList list = createList(exprValue);
							aTerm = ATermUtils.makeAnd(list);
						}
						else
							if (exprType.equals(OWL.unionOf.asNode()))
							{
								final ATermList list = createList(exprValue);
								aTerm = ATermUtils.makeOr(list);
							}
							else
								if (exprType.equals(OWL.complementOf.asNode()) || exprType.equals(OWL2.datatypeComplementOf.asNode()))
								{
									final ATermAppl complement = node2term(exprValue);
									aTerm = ATermUtils.makeNot(complement);
								}
								else
									if (exprType.equals(OWL.inverseOf.asNode()))
									{
										final ATermAppl inverse = node2term(exprValue);
										aTerm = ATermUtils.makeInv(inverse);
									}
									else
										if (exprType.equals(OWL.oneOf.asNode()))
										{
											final ATermList list = createList(exprValue);
											ATermList result = ATermUtils.EMPTY_LIST;
											if (list.isEmpty())
												aTerm = ATermUtils.BOTTOM;
											else
											{
												for (ATermList l = list; !l.isEmpty(); l = l.getNext())
												{
													final ATermAppl c = (ATermAppl) l.getFirst();
													final ATermAppl nominal = ATermUtils.makeValue(c);
													result = result.insert(nominal);
												}

												aTerm = ATermUtils.makeOr(result);
											}
										}
										else
											if (exprType.equals(OWL2.onDatatype.asNode()))
												aTerm = parseDataRange(node, exprValue);
											else
												if (exprType.equals(OWL2.onDataRange.asNode()))
													aTerm = parseDataRangeLegacy(node, exprValue);
												else
													if (exprType.equals(OWL2.propertyChain.asNode()))
													{
														// do nothing because we cannot return an ATermList here
													}
													else
														addUnsupportedFeature("Unexpected bnode " + node + " " + expr);
					}
					else
					{
						canCache = false;
						aTerm = JenaUtils.makeATerm(node);
					}
				}
				else
					aTerm = JenaUtils.makeATerm(node);
			if (canCache)
				_terms.put(node, aTerm);
		}

		return aTerm;
	}

	protected Triple getExpression(final Node node)
	{
		for (final BuiltinTerm expressionPredicate : BuiltinTerm.EXPRESSION_PREDICATES)
		{
			final ClosableIterator<Triple> i = _graph.find(node, expressionPredicate.getNode(), null);
			if (i.hasNext())
			{
				_monitor.incrementProgress();

				final Triple t = i.next();
				i.close();
				return t;
			}
		}

		return null;
	}

	private ATermAppl parseDataRangeLegacy(final Node s, final Node definition)
	{
		if (!definition.isURI())
		{
			addUnsupportedFeature("Invalid datatype definition, _expected URI but found " + s);
			return ATermUtils.BOTTOM_LIT;
		}

		final ATermAppl baseDatatype = ATermUtils.makeTermAppl(definition.getURI());

		final Property[] datatypeFacets = new Property[] { OWL2.minInclusive, OWL2.maxInclusive, OWL2.minExclusive, OWL2.maxExclusive, OWL2.totalDigits, OWL2.fractionDigits, OWL2.pattern };

		final List<ATermAppl> restrictions = new ArrayList<>();
		for (final Property datatypeFacet : datatypeFacets)
		{
			final Node facetValue = getObject(s, datatypeFacet.asNode());
			if (facetValue != null)
			{
				final ATermAppl restriction = ATermUtils.makeFacetRestriction(ATermUtils.makeTermAppl(datatypeFacet.getURI()), JenaUtils.makeATerm(facetValue));
				restrictions.add(restriction);
			}
		}

		if (restrictions.isEmpty())
		{
			addUnsupportedFeature("A _data range is defined without XSD facet restrictions " + s);
			return ATermUtils.BOTTOM_LIT;
		}
		else
			return ATermUtils.makeRestrictedDatatype(baseDatatype, restrictions.toArray(new ATermAppl[restrictions.size()]));
	}

	private ATermAppl parseDataRange(final Node s, final Node definition)
	{
		if (!definition.isURI())
		{
			addUnsupportedFeature("Invalid datatype definition, _expected URI but found " + s);
			return ATermUtils.BOTTOM_LIT;
		}

		final ATermAppl baseDatatype = ATermUtils.makeTermAppl(definition.getURI());

		final Property[] datatypeFacets = new Property[] { OWL2.minInclusive, OWL2.maxInclusive, OWL2.minExclusive, OWL2.maxExclusive, OWL2.totalDigits, OWL2.fractionDigits, OWL2.pattern };

		final List<ATermAppl> restrictions = new ArrayList<>();
		final Node restrictionList = getObject(s, OWL2.withRestrictions.asNode());
		final RDFListIterator i = new RDFListIterator(restrictionList);
		while (i.hasNext())
		{
			final Node restrictionNode = i.next();
			if (restrictionNode != null)
				for (final Property datatypeFacet : datatypeFacets)
				{
					final Node facetValue = getObject(restrictionNode, datatypeFacet.asNode());
					if (facetValue != null)
					{
						final ATermAppl restriction = ATermUtils.makeFacetRestriction(ATermUtils.makeTermAppl(datatypeFacet.getURI()), JenaUtils.makeATerm(facetValue));
						restrictions.add(restriction);
					}
				}
		}

		if (restrictions.isEmpty())
		{
			addUnsupportedFeature("A _data range is defined without XSD facet restrictions " + s);
			return ATermUtils.BOTTOM_LIT;
		}
		else
			return ATermUtils.makeRestrictedDatatype(baseDatatype, restrictions.toArray(new ATermAppl[restrictions.size()]));
	}

	private void defineRule(final Node node)
	{
		final List<RuleAtom> head = parseAtomList(getObject(node, SWRL.head.asNode()));
		final List<RuleAtom> body = parseAtomList(getObject(node, SWRL.body.asNode()));

		if (head == null || body == null)
		{
			String whichPart = "head and body";
			if (head != null)
				whichPart = "body";
			else
				if (body != null)
					whichPart = "head";
			addUnsupportedFeature("Ignoring SWRL rule (unsupported " + whichPart + "): " + node);

			return;
		}

		final ATermAppl name = JenaUtils.makeATerm(node);
		final Rule rule = new Rule(name, head, body);
		_kb.addRule(rule);
	}

	private AtomDObject createRuleDObject(final Node node)
	{

		if (!node.isLiteral())
		{
			final ATermAppl name = node2term(node);
			if (!ATermUtils.isPrimitive(name))
			{
				addUnsupportedFeature("Cannot create rule _data variable out of " + node);
				return null;
			}
			return new AtomDVariable(name.toString());
		}
		else
			return new AtomDConstant(node2term(node));
	}

	private AtomIObject createRuleIObject(final Node node)
	{
		if (hasObject(node, RDF.type.asNode(), SWRL.Variable.asNode()))
			return new AtomIVariable(node.getURI());
		else
		{
			final ATermAppl term = node2term(node);
			if (defineIndividual(term))
				return new AtomIConstant(node2term(node));
			else
			{
				addUnsupportedFeature("Cannot create rule _individual object for _node " + node);
				return null;
			}
		}
	}

	private List<RuleAtom> parseAtomList(final Node atomListParam)
	{
		Node atomList = atomListParam;

		Node obj = null;
		final List<RuleAtom> atoms = new ArrayList<>();

		while (atomList != null && !atomList.equals(RDF.nil.asNode()))
		{
			String atomType = "unsupported atom";
			final Node atomNode = getObject(atomList, RDF.first.asNode());

			RuleAtom atom = null;
			if (hasObject(atomNode, RDF.type.asNode(), SWRL.ClassAtom.asNode()))
			{
				ATermAppl description = null;
				AtomIObject argument = null;
				atomType = "ClassAtom";

				if ((obj = getObject(atomNode, SWRL.classPredicate.asNode())) != null)
					description = node2term(obj);

				if ((obj = getObject(atomNode, SWRL.argument1.asNode())) != null)
					argument = createRuleIObject(obj);

				if (description == null)
					addUnsupportedFeature("Error on " + SWRL.classPredicate);
				else
					if (argument == null)
						addUnsupportedFeature("Error on" + SWRL.argument1);
					else
						atom = new ClassAtom(description, argument);
			}
			else
				if (hasObject(atomNode, RDF.type.asNode(), SWRL.IndividualPropertyAtom.asNode()))
				{
					ATermAppl pred = null;
					AtomIObject argument1 = null;
					AtomIObject argument2 = null;
					atomType = "IndividualPropertyAtom";

					if ((obj = getObject(atomNode, SWRL.propertyPredicate.asNode())) != null)
						pred = node2term(obj);

					if ((obj = getObject(atomNode, SWRL.argument1.asNode())) != null)
						argument1 = createRuleIObject(obj);

					if ((obj = getObject(atomNode, SWRL.argument2.asNode())) != null)
						argument2 = createRuleIObject(obj);

					if (pred == null || !defineObjectProperty(pred))
						addUnsupportedFeature("Cannot define datatype property " + pred);
					else
						if (argument1 == null)
							addUnsupportedFeature("Term not found: " + SWRL.argument1);
						else
							if (argument2 == null)
								addUnsupportedFeature("Term not found " + SWRL.argument2);
							else
								atom = new IndividualPropertyAtom(pred, argument1, argument2);
				}
				else
					if (hasObject(atomNode, RDF.type.asNode(), SWRL.DifferentIndividualsAtom.asNode()))
					{
						AtomIObject argument1 = null;
						AtomIObject argument2 = null;
						atomType = "DifferentIndividualsAtom";

						if ((obj = getObject(atomNode, SWRL.argument1.asNode())) != null)
							argument1 = createRuleIObject(obj);

						if ((obj = getObject(atomNode, SWRL.argument2.asNode())) != null)
							argument2 = createRuleIObject(obj);

						if (argument1 == null)
							addUnsupportedFeature("Term not found " + SWRL.argument1);
						else
							if (argument2 == null)
								addUnsupportedFeature("Term not found " + SWRL.argument2);
							else
								atom = new DifferentIndividualsAtom(argument1, argument2);
					}
					else
						if (hasObject(atomNode, RDF.type.asNode(), SWRL.SameIndividualAtom.asNode()))
						{
							AtomIObject argument1 = null;
							AtomIObject argument2 = null;
							atomType = "SameIndividualAtom";

							if ((obj = getObject(atomNode, SWRL.argument1.asNode())) != null)
								argument1 = createRuleIObject(obj);

							if ((obj = getObject(atomNode, SWRL.argument2.asNode())) != null)
								argument2 = createRuleIObject(obj);

							if (argument1 == null)
								addUnsupportedFeature("Term not found " + SWRL.argument1);
							else
								if (argument2 == null)
									addUnsupportedFeature("Term not found " + SWRL.argument2);
								else
									atom = new SameIndividualAtom(argument1, argument2);
						}
						else
							if (hasObject(atomNode, RDF.type.asNode(), SWRL.DatavaluedPropertyAtom.asNode()))
							{
								ATermAppl pred = null;
								AtomIObject argument1 = null;
								AtomDObject argument2 = null;
								atomType = "DatavaluedPropertyAtom";

								if ((obj = getObject(atomNode, SWRL.propertyPredicate.asNode())) != null)
									pred = node2term(obj);

								if ((obj = getObject(atomNode, SWRL.argument1.asNode())) != null)
									argument1 = createRuleIObject(obj);

								if ((obj = getObject(atomNode, SWRL.argument2.asNode())) != null)
									argument2 = createRuleDObject(obj);

								if (pred == null || !defineDatatypeProperty(pred))
									addUnsupportedFeature("Cannot define datatype property " + pred);
								else
									if (argument1 == null)
										addUnsupportedFeature("Term not found " + SWRL.argument1);
									else
										if (argument2 == null)
											addUnsupportedFeature("Term not found " + SWRL.argument2);
										else
											atom = new DatavaluedPropertyAtom(pred, argument1, argument2);
							}
							else
								if (hasObject(atomNode, RDF.type.asNode(), SWRL.BuiltinAtom.asNode()))
								{
									atomType = "BuiltinAtom";
									Node builtInNode = null;
									List<AtomDObject> arguments = null;

									if ((obj = getObject(atomNode, SWRL.arguments.asNode())) != null)
										arguments = parseArgumentList(obj);

									builtInNode = getObject(atomNode, SWRL.builtin.asNode());

									if (arguments == null)
										addUnsupportedFeature("Term not found " + SWRL.arguments);
									else
										if (builtInNode != null && builtInNode.isURI())
											atom = new BuiltInAtom(builtInNode.getURI(), arguments);
								}
								else
									if (hasObject(atomNode, RDF.type.asNode(), SWRL.DataRangeAtom.asNode()))
									{
										atomType = "DataRangeAtom";
										ATermAppl datatype = null;
										AtomDObject argument = null;

										if ((obj = getObject(atomNode, SWRL.dataRange.asNode())) != null)
											datatype = node2term(obj);

										if ((obj = getObject(atomNode, SWRL.argument1.asNode())) != null)
											argument = createRuleDObject(obj);

										if (datatype == null)
											addUnsupportedFeature("Term not found " + SWRL.dataRange);
										else
											if (argument == null)
												addUnsupportedFeature("Term not found " + SWRL.argument1);
											else
												atom = new DataRangeAtom(datatype, argument);
									}

			if (atom == null)
			{
				addUnsupportedFeature("Ignoring SWRL " + atomType + ": " + atomNode);
				return null;
			}

			atoms.add(atom);

			atomList = getObject(atomList, RDF.rest.asNode());

		}

		if (atomList == null)
		{
			addUnsupportedFeature("Not nil-terminated _list in atom _list! (Seen " + atoms + " )");
			return null;
		}

		return atoms;
	}

	private List<AtomDObject> parseArgumentList(final Node argumentListParam)
	{
		Node argumentList = argumentListParam;
		final List<AtomDObject> arguments = new ArrayList<>();

		while (argumentList != null && !argumentList.equals(RDF.nil.asNode()))
		{
			final Node argumentNode = getObject(argumentList, RDF.first.asNode());

			if (argumentNode == null)
				addUnsupportedFeature("Term in _list not found " + RDF.first);
			else
			{
				arguments.add(createRuleDObject(argumentNode));

				argumentList = getObject(argumentList, RDF.rest.asNode());
			}
		}

		return arguments;
	}

	private boolean addNegatedAssertion(final Node stmt)
	{
		final Node s = getObject(stmt, OWL2.sourceIndividual.asNode());
		if (s == null)
		{
			addUnsupportedFeature("Negated property value is missing owl:sourceIndividual value");
			return false;
		}

		final Node p = getObject(stmt, OWL2.assertionProperty.asNode());
		if (p == null)
		{
			addUnsupportedFeature("Negated property value is missing owl:assertionProperty value");
			return false;
		}

		final Node oi = getObject(stmt, OWL2.targetIndividual.asNode());
		final Node ov = getObject(stmt, OWL2.targetValue.asNode());
		if (oi == null && ov == null)
		{
			addUnsupportedFeature("Negated property value is missing owl:targetIndividual or owl:targetValue value");
			return false;
		}
		if (oi != null && ov != null)
		{
			addUnsupportedFeature("Negated property value must not have owl:targetIndividual and owl:targetValue value");
			return false;
		}

		final ATermAppl st = node2term(s);
		final ATermAppl pt = node2term(p);
		ATermAppl ot;

		defineIndividual(st);
		if (oi != null)
		{
			ot = node2term(oi);
			if (oi.isURI() || oi.isBlank())
			{
				defineObjectProperty(pt);
				defineIndividual(ot);
			}
			else
			{
				addUnsupportedFeature("Invalid negated property target _individual " + stmt);
				return false;
			}
		}
		else
		{
			ot = node2term(ov);
			if (ov != null && ov.isLiteral())
				defineDatatypeProperty(pt);
			else
			{
				addUnsupportedFeature("Invalid negated property target value " + stmt);
				return false;
			}
		}

		if (!_kb.addNegatedPropertyValue(pt, st, ot))
		{
			addUnsupportedFeature("Skipping invalid negated property value " + stmt);
			return false;
		}

		return true;
	}

	protected boolean defineClass(final ATermAppl c)
	{
		if (ATermUtils.isPrimitive(c))
		{
			_kb.addClass(c);
			return true;
		}
		else
			return ATermUtils.isComplexClass(c);
	}

	protected boolean defineDatatype(final ATermAppl dt)
	{
		if (ATermUtils.isPrimitive(dt))
		{
			_kb.addDatatype(dt);
			return true;
		}
		else
			return _kb.isDatatype(dt);
	}

	/**
	 * There are two properties that are used in a subPropertyOf or equivalentProperty axiom. If one of them is defined as an Object (or Data) Property the
	 * other should also be defined as an Object (or Data) Property
	 *
	 * @param p1
	 * @param p2
	 * @return
	 */
	private boolean defineProperties(final ATermAppl p1, final ATermAppl p2)
	{
		final PropertyType type1 = _kb.getPropertyType(p1);
		final PropertyType type2 = _kb.getPropertyType(p2);
		if (type1 != type2)
		{
			if (type1 == PropertyType.UNTYPED)
			{
				if (type2 == PropertyType.OBJECT)
					defineObjectProperty(p1);
				else
					if (type2 == PropertyType.DATATYPE)
						defineDatatypeProperty(p1);
			}
			else
				if (type2 == PropertyType.UNTYPED)
				{
					if (type1 == PropertyType.OBJECT)
						defineObjectProperty(p2);
					else
						if (type1 == PropertyType.DATATYPE)
							defineDatatypeProperty(p2);
				}
				else
					// addWarning("Properties " + p1 + ", " + p2
					// + " are related but first is " + PropertyType.TYPES[type1]
					// + "Property and second is " + PropertyType.TYPES[type2]);
					return false;
		}
		else
			if (type1 == PropertyType.UNTYPED)
			{
				defineProperty(p1);
				defineProperty(p2);
			}

		return true;
	}

	protected boolean defineObjectProperty(final ATermAppl c)
	{
		if (!ATermUtils.isPrimitive(c) && !ATermUtils.isInv(c))
			return false;

		return _kb.addObjectProperty(c);
	}

	protected boolean defineDatatypeProperty(final ATermAppl c)
	{
		if (!ATermUtils.isPrimitive(c))
			return false;

		return _kb.addDatatypeProperty(c);
	}

	private boolean defineAnnotationProperty(final ATermAppl c)
	{
		if (!ATermUtils.isPrimitive(c))
			return false;

		return _kb.addAnnotationProperty(c);
	}

	protected boolean defineProperty(final ATermAppl c)
	{
		if (ATermUtils.isInv(c))
		{
			_kb.addObjectProperty(c.getArgument(0));
			return true;
		}
		else
			if (!ATermUtils.isPrimitive(c))
				return false;

		_kb.addProperty(c);
		return true;
	}

	protected boolean defineIndividual(final ATermAppl c)
	{
		_kb.addIndividual(c);
		return true;
	}

	@SuppressWarnings("unused")
	private PropertyType guessPropertyType(final ATermAppl p, final Node prop)
	{
		final PropertyType roleType = _kb.getPropertyType(p);
		if (roleType != PropertyType.UNTYPED)
			return roleType;

		defineProperty(p);

		final Iterator<?> i = _graph.find(prop, RDF.type.asNode(), null);
		while (i.hasNext())
		{
			final Triple stmt = (Triple) i.next();
			final Node o = stmt.getObject();

			if (o.equals(OWL.ObjectProperty.asNode()))
				return PropertyType.OBJECT;
			else
				if (o.equals(OWL.DatatypeProperty.asNode()))
					return PropertyType.DATATYPE;
				else
					if (o.equals(OWL.AnnotationProperty.asNode()))
						return PropertyType.ANNOTATION;
					else
						if (o.equals(OWL.OntologyProperty.asNode()))
							return PropertyType.ANNOTATION;
		}

		return PropertyType.UNTYPED;
	}

	/**
	 * Process all triples with <code>rdf:type</code> predicate. If {@link PelletOptions#PREPROCESS_TYPE_TRIPLES} option is <code>true</code> this function is a
	 * noop.
	 */
	protected void processTypes()
	{
		if (_preprocessTypeTriples)
		{
			_logger.fine("processTypes");
			if (isLoadABox())
				processTypes(Node.ANY);
			else
				for (final Node type : TBOX_TYPES)
					processTypes(type);
		}
	}

	/**
	 * Process triples with <code>rdf:type</code> predicate and given object. Type can be {@link Node.ANY} to indicate all type triples should be processed.
	 *
	 * @param type the object of <code>rdf:type</code> triples to be processed
	 */
	protected void processTypes(final Node type)
	{
		final ClosableIterator<Triple> i = _graph.find(null, RDF.type.asNode(), type);
		while (i.hasNext())
		{
			final Triple stmt = i.next();
			processType(stmt);
		}
		i.close();
	}

	/**
	 * Process a single <code>rdf:type</code> triple. Type triples that are part of the OWL syntax, e.g. <code>_:x rdf:type owl:Restriction</code> will not be
	 * processed since they are handled by the {@link #node2term(Node)} function.
	 *
	 * @param triple Type triple that will be processed
	 */
	protected void processType(final Triple triple)
	{
		final Node s = triple.getSubject();
		final Node o = triple.getObject();

		final BuiltinTerm builtinTerm = BuiltinTerm.find(o);

		if (builtinTerm != null)
		{
			if (builtinTerm.isSyntax())
				return;

			// If we have a triple _:x rdf:type owl:Class then this is a noop
			// that would only _cache class expression for _:x. However, since
			// we did not complete process all type triples unqualified cardinality
			// restrictions would cause issues here since they require property
			// to be either _data or object property. Therefore, we _stop processing
			// this triple immediately before calling node2term function.
			if (s.isBlank() && builtinTerm.equals(BuiltinTerm.OWL_Class))
				return;
		}

		_monitor.incrementProgress();

		final ATermAppl st = node2term(s);

		if (builtinTerm == null)
		{
			if (OpenlletOptions.FREEZE_BUILTIN_NAMESPACES && o.isURI())
			{
				final String nameSpace = o.getNameSpace();
				if (nameSpace != null)
				{
					final BuiltinNamespace builtin = BuiltinNamespace.find(nameSpace);
					if (builtin != null)
					{
						addUnsupportedFeature("Ignoring triple with unknown term from " + builtin + " namespace: " + triple);
						return;
					}
				}
			}

			return;
		}

		switch (builtinTerm)
		{

			case RDF_Property:
				defineProperty(st);
				break;

			case RDFS_Class:
				defineClass(st);
				break;

			case RDFS_Datatype:
			case OWL_DataRange:
				if (s.isURI())
					defineDatatype(st);
				else
					_anonDatatypes.add(s);

				break;

			case OWL_Class:
				defineClass(st);
				break;

			case OWL_Thing:
			case OWL2_NamedIndividual:
				defineIndividual(st);
				break;

			case OWL_Nothing:
				defineIndividual(st);
				_kb.addType(st, ATermUtils.BOTTOM);
				break;

			case OWL_ObjectProperty:
				if (s.isURI() && !defineObjectProperty(st))
					addUnsupportedFeature("Property " + st + " is defined both as an ObjectProperty and a " + _kb.getPropertyType(st) + "Property");
				break;

			case OWL_DatatypeProperty:
				if (!defineDatatypeProperty(st))
					addUnsupportedFeature("Property " + st + " is defined both as a DatatypeProperty and a " + _kb.getPropertyType(st) + "Property");
				break;

			case OWL_FunctionalProperty:
				defineProperty(st);
				_kb.addFunctionalProperty(st);
				addSimpleProperty(st, CARDINALITY);
				break;

			case OWL_InverseFunctionalProperty:
				if (defineProperty(st))
				{
					_kb.addInverseFunctionalProperty(st);
					addSimpleProperty(st, CARDINALITY);
				}
				else
					addUnsupportedFeature("Ignoring InverseFunctionalProperty axiom for " + st + " (" + _kb.getPropertyType(st) + "Property)");
				break;

			case OWL_TransitiveProperty:
				if (defineObjectProperty(st))
					_kb.addTransitiveProperty(st);
				else
					addUnsupportedFeature("Ignoring TransitiveProperty axiom for " + st + " (" + _kb.getPropertyType(st) + "Property)");

				break;

			case OWL_SymmetricProperty:
				if (defineObjectProperty(st))
					_kb.addSymmetricProperty(st);
				else
					addUnsupportedFeature("Ignoring SymmetricProperty axiom for " + st + " (" + _kb.getPropertyType(st) + "Property)");
				break;

			case OWL_AnnotationProperty:
				if (!defineAnnotationProperty(st))
					addUnsupportedFeature("Property " + st + " is defined both as an AnnotationProperty and a " + _kb.getPropertyType(st) + "Property");
				break;

			case OWL2_ReflexiveProperty:
				if (defineObjectProperty(st))
					_kb.addReflexiveProperty(st);
				else
					addUnsupportedFeature("Ignoring ReflexiveProperty axiom for " + st + " (" + _kb.getPropertyType(st) + "Property)");
				break;

			case OWL2_IrreflexiveProperty:
				if (defineObjectProperty(st))
				{
					_kb.addIrreflexiveProperty(st);
					addSimpleProperty(st, IRREFLEXIVE);
				}
				else
					addUnsupportedFeature("Ignoring IrreflexiveProperty axiom for " + st + " (" + _kb.getPropertyType(st) + "Property)");
				break;

			case OWL2_AsymmetricProperty:
				if (defineObjectProperty(st))
				{
					_kb.addAsymmetricProperty(st);
					addSimpleProperty(st, ANTI_SYM);
				}
				else
					addUnsupportedFeature("Ignoring AntisymmetricProperty axiom for " + st + " (" + _kb.getPropertyType(st) + "Property)");
				break;

			case OWL2_NegativePropertyAssertion:
				addNegatedAssertion(s);
				break;

			case SWRL_Imp:
				if (OpenlletOptions.DL_SAFE_RULES)
					defineRule(s);
				break;

			case OWL_AllDifferent:
			case OWL2_AllDisjointClasses:
			case OWL2_AllDisjointProperties:
				_naryDisjoints.put(s, builtinTerm);
				break;

			default:
				throw new InternalReasonerException("Unexpected term: " + o);
		}
	}

	/**
	 * Process all the triples in the raw _graph. If {@link PelletOptions#PREPROCESS_TYPE_TRIPLES} option is <code>true</code> all <code>rdf:type</code> will be
	 * ignored since they have already been processed with {@link #processTypes()} function.
	 */
	protected void processTriples()
	{
		_logger.fine("processTriples");
		if (isLoadABox())
			processTriples(Node.ANY);
		else
			for (final Node predicate : TBOX_PREDICATES)
				processTriples(predicate);
	}

	/**
	 * Process triples with the given predicate. Predicate can be {@link Node.ANY} to indicate all triples should be processed.
	 *
	 * @param predicate Predicate of the triples that will be processed
	 */
	protected void processTriples(final Node predicate)
	{
		final ClosableIterator<Triple> i = _graph.find(null, predicate, null);
		while (i.hasNext())
		{
			final Triple triple = i.next();
			processTriple(triple);
		}
		i.close();
	}

	/**
	 * Process a single triple that corresponds to an axiom (or a fact). This means triples that are part of OWL syntax, e.g. a triple with
	 * <code>owl:onProperty</code> predicate, will not be processed since they are handled by the {@link #node2term(Node)} function. Also, if
	 * {@link PelletOptions#PREPROCESS_TYPE_TRIPLES} option is <code>true</code> any triple with <code>rdf:type</code> predicate will be ignored.
	 *
	 * @param triple Triple to be processed.
	 */
	@SuppressWarnings("deprecation")
	// OWL2.SelfRestriction is deprecated but we have control over it.
	protected void processTriple(final Triple triple)
	{
		final Node p = triple.getPredicate();
		final Node s = triple.getSubject();
		final Node o = triple.getObject();

		final BuiltinTerm builtinTerm = BuiltinTerm.find(p);

		if (builtinTerm != null)
		{
			if (builtinTerm.isSyntax())
				return;

			if (builtinTerm.equals(BuiltinTerm.RDF_type))
			{
				if (BuiltinTerm.find(o) == null)
				{
					if (isLoadABox())
					{
						final ATermAppl ot = node2term(o);

						if (!AnnotationClasses.contains(ot))
						{
							defineClass(ot);

							final ATermAppl st = node2term(s);
							defineIndividual(st);
							_kb.addType(st, ot);
						}
					}
				}
				else
					if (!_preprocessTypeTriples)
						processType(triple);

				return;
			}
		}

		_monitor.incrementProgress();

		final ATermAppl st = node2term(s);
		final ATermAppl ot = node2term(o);

		if (builtinTerm == null)
		{
			final ATermAppl pt = node2term(p);
			final Role role = _kb.getProperty(pt);
			final PropertyType type = (role == null) ? PropertyType.UNTYPED : role.getType();

			if (type == PropertyType.ANNOTATION)
			{
				// Skip ontology annotations
				if (_graph.contains(s, RDF.type.asNode(), OWL.Ontology.asNode()))
					return;

				if (defineAnnotationProperty(pt))
					_kb.addAnnotation(st, pt, ot);

				return;
			}

			if (OpenlletOptions.FREEZE_BUILTIN_NAMESPACES)
			{
				final String nameSpace = p.getNameSpace();
				if (nameSpace != null)
				{
					final BuiltinNamespace builtin = BuiltinNamespace.find(nameSpace);
					if (builtin != null)
					{
						addUnsupportedFeature("Ignoring triple with unknown property from " + builtin + " namespace: " + triple);
						return;
					}
				}
			}

			if (o.isLiteral())
			{
				if (defineDatatypeProperty(pt))
				{
					final String datatypeURI = ((ATermAppl) ot.getArgument(2)).getName();

					if (defineIndividual(st))
					{
						defineDatatypeProperty(pt);
						if (!"".equals(datatypeURI))
							defineDatatype(ATermUtils.makeTermAppl(datatypeURI));

						_kb.addPropertyValue(pt, st, ot);
					}
					else
						if (type == PropertyType.UNTYPED)
							defineAnnotationProperty(pt);
						else
							addUnsupportedFeature("Ignoring ObjectProperty used with a class expression: " + triple);
				}
				else
					addUnsupportedFeature("Ignoring literal value used with ObjectProperty : " + triple);
			}
			else
				if (!defineObjectProperty(pt))
					addUnsupportedFeature("Ignoring object value used with DatatypeProperty: " + triple);
				else
					if (!defineIndividual(st))
						addUnsupportedFeature("Ignoring class expression used in subject position: " + triple);
					else
						if (!defineIndividual(ot))
							addUnsupportedFeature("Ignoring class expression used in object position: " + triple);
						else
							_kb.addPropertyValue(pt, st, ot);
			return;
		}

		switch (builtinTerm)
		{

			case RDFS_subClassOf:
				if (!defineClass(st))
					addUnsupportedFeature("Ignoring subClassOf axiom because the subject is not a class " + st + " rdfs:subClassOf " + ot);
				else
					if (!defineClass(ot))
						addUnsupportedFeature("Ignoring subClassOf axiom because the object is not a class " + st + " rdfs:subClassOf " + ot);
					else
						_kb.addSubClass(st, ot);
				break;

			case RDFS_subPropertyOf:
				ATerm subProp = null;
				if (s.isBlank())
				{
					final Triple expr = getExpression(s);
					if (expr == null)
						addUnsupportedFeature("Bnode in rdfs:subProperty axioms is not a valid property expression");
					else
						if (expr.getPredicate().equals(OWL.inverseOf.asNode()))
						{
							if (defineObjectProperty((ATermAppl) st.getArgument(0)) && defineObjectProperty(ot))
								subProp = st;
						}
						else
							if (expr.getPredicate().equals(OWL2.propertyChain.asNode()))
							{
								subProp = createList(expr.getObject());
								ATermList list = (ATermList) subProp;
								while (!list.isEmpty())
								{
									if (!defineObjectProperty((ATermAppl) list.getFirst()))
										break;
									list = list.getNext();
								}
								if (!list.isEmpty() || !defineObjectProperty(ot))
									subProp = null;
							}
							else
								addUnsupportedFeature("Bnode in rdfs:subProperty axioms is not a valid property expression");
				}
				else
					if (defineProperties(st, ot))
						subProp = st;

				if (subProp != null)
					_kb.addSubProperty(subProp, ot);
				else
					addUnsupportedFeature("Ignoring subproperty axiom between " + st + " (" + _kb.getPropertyType(st) + "Property) and " + ot + " (" + _kb.getPropertyType(ot) + "Property)");

				break;

			case RDFS_domain:
				if (_kb.isAnnotationProperty(st))
					addUnsupportedFeature("Ignoring domain axiom for AnnotationProperty " + st);
				else
				{
					defineProperty(st);
					defineClass(ot);
					_kb.addDomain(st, ot);
				}
				break;

			case RDFS_range:
				if (_kb.isAnnotationProperty(st))
				{
					addUnsupportedFeature("Ignoring range axiom for AnnotationProperty " + st);
					break;
				}

				if (_kb.isDatatype(ot))
					defineDatatypeProperty(st);
				else
					if (_kb.isClass(ot))
						defineObjectProperty(st);
					else
						defineProperty(st);

				if (_kb.isDatatypeProperty(st))
					defineDatatype(ot);
				else
					if (_kb.isObjectProperty(st))
						defineClass(ot);

				_kb.addRange(st, ot);

				break;

			case OWL_intersectionOf:
				ATermList list = createList(o);

				defineClass(st);
				final ATermAppl conjunction = ATermUtils.makeAnd(list);

				_kb.addEquivalentClass(st, conjunction);
				break;

			case OWL_unionOf:
				list = createList(o);

				defineClass(st);
				ATermAppl disjunction = ATermUtils.makeOr(list);
				_kb.addEquivalentClass(st, disjunction);

				break;

			case OWL2_disjointUnionOf:
				list = createList(o);

				_kb.addDisjointClasses(list);

				defineClass(st);
				disjunction = ATermUtils.makeOr(list);
				_kb.addEquivalentClass(st, disjunction);

				break;

			case OWL_complementOf:
				if (!defineClass(st))
					addUnsupportedFeature("Ignoring complementOf axiom because the subject is not a class " + st + " owl:complementOf " + ot);
				else
					if (!defineClass(ot))
						addUnsupportedFeature("Ignoring complementOf axiom because the object is not a class " + st + " owl:complementOf " + ot);
					else
						_kb.addComplementClass(st, ot);
				break;

			case OWL_equivalentClass:
				if (_kb.isDatatype(ot) || _anonDatatypes.contains(o))
				{
					if (!defineDatatype(st))
						addUnsupportedFeature("Ignoring equivalentClass axiom because the subject is not a datatype " + st + " owl:equivalentClass " + ot);
					else
						_kb.addDatatypeDefinition(st, ot);
				}
				else
					if (!defineClass(st))
						addUnsupportedFeature("Ignoring equivalentClass axiom because the subject is not a class " + st + " owl:equivalentClass " + ot);
					else
						if (!defineClass(ot))
							addUnsupportedFeature("Ignoring equivalentClass axiom because the object is not a class " + st + " owl:equivalentClass " + ot);
						else
							_kb.addEquivalentClass(st, ot);

				break;

			case OWL_disjointWith:
				if (!defineClass(st))
					addUnsupportedFeature("Ignoring disjointWith axiom because the subject is not a class " + st + " owl:disjointWith " + ot);
				else
					if (!defineClass(ot))
						addUnsupportedFeature("Ignoring disjointWith axiom because the object is not a class " + st + " owl:disjointWith " + ot);
					else
						_kb.addDisjointClass(st, ot);
				break;

			case OWL2_propertyDisjointWith:
				if (defineProperties(st, ot))
				{
					_kb.addDisjointProperty(st, ot);

					addSimpleProperty(st, DISJOINT);
					addSimpleProperty(ot, DISJOINT);
				}
				else
					addUnsupportedFeature("Ignoring disjoint property axiom between " + st + " (" + _kb.getPropertyType(st) + "Property) and " + ot + " (" + _kb.getPropertyType(ot) + "Property)");
				break;

			case OWL2_propertyChainAxiom:
				ATermAppl superProp = null;
				if (s.isBlank())
				{
					final Triple expr = getExpression(s);
					if (expr == null)
						addUnsupportedFeature("Bnode in owl:propertyChainAxiom axiom is not a valid property expression");
					else
						if (expr.getPredicate().equals(OWL.inverseOf.asNode()))
						{
							if (defineObjectProperty((ATermAppl) st.getArgument(0)))
								superProp = st;
						}
						else
							addUnsupportedFeature("Bnode in owl:propertyChainAxiom axiom is not a valid property expression");
				}
				else
					if (defineObjectProperty(st))
						superProp = st;

				subProp = createList(o);
				list = (ATermList) subProp;
				while (!list.isEmpty())
				{
					if (!defineObjectProperty((ATermAppl) list.getFirst()))
						break;
					list = list.getNext();
				}
				if (!list.isEmpty())
					subProp = null;

				if (subProp != null && superProp != null)
					_kb.addSubProperty(subProp, superProp);
				else
					addUnsupportedFeature("Ignoring property chain axiom between " + st + " (" + _kb.getPropertyType(st) + "Property) and " + ot);
				break;

			case OWL_equivalentProperty:
				if (defineProperties(st, ot))
					_kb.addEquivalentProperty(st, ot);
				else
					addUnsupportedFeature("Ignoring equivalent property axiom between " + st + " (" + _kb.getPropertyType(st) + "Property) and " + ot + " (" + _kb.getPropertyType(ot) + "Property)");

				break;

			case OWL_inverseOf:
				if (defineObjectProperty(st) && defineObjectProperty(ot))
					_kb.addInverseProperty(st, ot);
				else
					addUnsupportedFeature("Ignoring inverseOf axiom between " + st + " (" + _kb.getPropertyType(st) + "Property) and " + ot + " (" + _kb.getPropertyType(ot) + "Property)");

				break;

			case OWL_sameAs:
				if (defineIndividual(st) && defineIndividual(ot))
					_kb.addSame(st, ot);
				else
					addUnsupportedFeature("Ignoring sameAs axiom between " + st + " and " + ot);
				break;

			case OWL_differentFrom:
				if (defineIndividual(st) && defineIndividual(ot))
					_kb.addDifferent(st, ot);
				else
					addUnsupportedFeature("Ignoring differentFrom axiom between " + st + " and " + ot);
				break;

			case OWL_distinctMembers:
				list = createList(o);
				for (ATermList l = list; !l.isEmpty(); l = l.getNext())
				{
					final ATermAppl c = (ATermAppl) l.getFirst();
					defineIndividual(c);
				}

				_kb.addAllDifferent(list);
				break;

			case OWL_members:
				BuiltinTerm entityType = null;
				if (_preprocessTypeTriples)
					entityType = _naryDisjoints.get(s);
				else
				{
					final Node type = getObject(s, RDF.type.asNode());
					if (type != null)
						entityType = BuiltinTerm.find(type);
				}

				if (entityType == null)
					addUnsupportedFeature("There is no valid rdf:type for an owl:members assertion: " + s);
				else
					if (!OWL_MEMBERS_TYPES.contains(entityType))
						addUnsupportedFeature("The rdf:type for an owl:members assertion is not recognized: " + entityType);
					else
					{
						list = createList(o);
						for (ATermList l = list; !l.isEmpty(); l = l.getNext())
						{
							final ATermAppl c = (ATermAppl) l.getFirst();
							switch (entityType)
							{
								case OWL_AllDifferent:
									defineIndividual(c);
									break;
								case OWL2_AllDisjointClasses:
									defineClass(c);
									break;
								case OWL2_AllDisjointProperties:
									defineProperty(c);
									break;
								default:
									_logger.severe("Unsupported entity type " + entityType);
							}
						}

						switch (entityType)
						{
							case OWL_AllDifferent:
								_kb.addAllDifferent(list);
								break;
							case OWL2_AllDisjointClasses:
								_kb.addDisjointClasses(list);
								break;
							case OWL2_AllDisjointProperties:
								_kb.addDisjointProperties(list);
								break;
							default:
								_logger.severe("Unsupported entity type " + entityType);
						}
					}
				break;

			case OWL_oneOf:
				ATermList resultList = ATermUtils.EMPTY_LIST;

				if (_kb.isDatatype(st))
					return;

				// assert the subject is a class
				defineClass(st);

				disjunction = null;
				list = createList(o);
				if (o.equals(RDF.nil.asNode()))
					disjunction = ATermUtils.BOTTOM;
				else
				{
					for (ATermList l = list; !l.isEmpty(); l = l.getNext())
					{
						final ATermAppl c = (ATermAppl) l.getFirst();

						if (OpenlletOptions.USE_PSEUDO_NOMINALS)
						{
							final ATermAppl nominal = ATermUtils.makeTermAppl(c.getName() + "_nominal");
							resultList = resultList.insert(nominal);

							defineClass(nominal);
							defineIndividual(c);
							_kb.addType(c, nominal);
						}
						else
						{
							defineIndividual(c);

							resultList = resultList.insert(ATermUtils.makeValue(c));
						}
					}
					disjunction = ATermUtils.makeOr(resultList);
				}
				_kb.addEquivalentClass(st, disjunction);
				break;

			case OWL2_hasKey:
				if (o.equals(RDF.nil.asNode()))
					return;

				final Set<ATermAppl> properties = new HashSet<>();
				// assert the subject is a class
				defineClass(st);
				list = createList(o);

				for (ATermList l = list; !l.isEmpty(); l = l.getNext())
				{
					final ATermAppl f = (ATermAppl) l.getFirst();
					defineProperty(f);
					properties.add(f);
				}

				_kb.addKey(st, properties);
				break;

			case OWL2_topDataProperty:
			case OWL2_bottomDataProperty:
			case OWL2_topObjectProperty:
			case OWL2_bottomObjectProperty:
				defineIndividual(st);
				_kb.addPropertyValue(node2term(p), st, ot);
				break;

			default:
				throw new InternalReasonerException("Unrecognized term: " + p);

		}
	}

	protected void processUntypedResources()
	{
		_logger.fine("processUntypedResource");

		// The copy into an array is here to avoid an ConcurrentModificationException
		for (final Role r : _kb.getRBox().getRoles().values().toArray(new RoleImpl[0]))
		{
			final SimpleProperty why = _simpleProperties.get(r.getName());
			if (why != null)
			{
				String msg = null;
				if (r.isTransitive())
					msg = "transitivity axiom";
				else
					if (r.hasComplexSubRole())
						msg = "complex sub property axiom";

				if (msg != null)
				{
					msg = "Ignoring " + msg + " due to an existing " + why + " for property " + r;
					addUnsupportedFeature(msg);
					r.removeSubRoleChains();
				}
			}

			if (r.isUntypedRole())
			{
				/*
				 * Untyped roles are made object properties unless they have
				 * datatype super or sub-roles
				 */
				boolean rangeToDatatype = false;
				final Set<Role> roles = SetUtils.union(r.getSubRoles(), r.getSuperRoles());
				for (final Role sub : roles)
					switch (sub.getType())
					{
						case OBJECT:
							defineObjectProperty(r.getName());
							break;
						case DATATYPE:
							defineDatatypeProperty(r.getName());
							rangeToDatatype = true;
							break;
						default:
							continue;
					}

				if (!rangeToDatatype)
					defineObjectProperty(r.getName());

				/*
				 * If a typing assumption has been made, carry over to any
				 * untyped range entity
				 */
				final Set<ATermAppl> ranges = r.getRanges();
				if (ranges != null)
					if (rangeToDatatype)
					{
						for (final ATermAppl range : ranges)
							if ((range.getAFun().getArity() == 0) && (!_kb.isDatatype(range)))
								defineDatatype(range);
					}
					else
						for (final ATermAppl range : ranges)
							if ((range.getAFun().getArity() == 0) && (!_kb.isClass(range)))
								defineClass(range);
			}
		}
	}

	@Override
	public void setKB(final KnowledgeBase kb)
	{
		_kb = kb;
	}

	private void defineBuiltinProperties()
	{
		defineAnnotationProperty(node2term(RDFS.label.asNode()));
		defineAnnotationProperty(node2term(RDFS.comment.asNode()));
		defineAnnotationProperty(node2term(RDFS.seeAlso.asNode()));
		defineAnnotationProperty(node2term(RDFS.isDefinedBy.asNode()));
		defineAnnotationProperty(node2term(OWL.versionInfo.asNode()));

		defineAnnotationProperty(node2term(OWL.backwardCompatibleWith.asNode()));
		defineAnnotationProperty(node2term(OWL.priorVersion.asNode()));
		defineAnnotationProperty(node2term(OWL.incompatibleWith.asNode()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void load(final Iterable<Graph> graphs) throws UnsupportedFeatureException
	{
		final Timer timer = _kb.getTimers().startTimer("load");

		_monitor.setProgressTitle("Loading");
		_monitor.taskStarted();

		_graph = EMPTY_GRAPH;
		preprocess();

		for (final Graph g : graphs)
		{
			_graph = g;
			processTypes();
		}

		for (final Graph g : graphs)
		{
			_graph = g;
			processTriples();
		}

		processUntypedResources();

		_monitor.taskFinished();

		timer.stop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void preprocess()
	{
		defineBuiltinProperties();
	}

	@Override
	public boolean isLoadABox()
	{
		return _loadABox;
	}

	@Override
	public void setLoadABox(final boolean loadABox)
	{
		_loadABox = loadABox;
	}

	@Override
	public boolean isLoadTBox()
	{
		return _loadTBox;
	}

	@Override
	public void setLoadTBox(final boolean loadTBox)
	{
		_loadTBox = loadTBox;
	}

	@Override
	public boolean isPreprocessTypeTriples()
	{
		return _preprocessTypeTriples;
	}

	@Override
	public void setPreprocessTypeTriples(final boolean preprocessTypeTriples)
	{
		_preprocessTypeTriples = preprocessTypeTriples;
	}
}
