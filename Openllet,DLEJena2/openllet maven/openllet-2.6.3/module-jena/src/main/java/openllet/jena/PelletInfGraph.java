// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under the terms of the MIT License.
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

package openllet.jena;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.KnowledgeBaseImpl;
import openllet.core.OpenlletOptions;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.OntBuilder;
import openllet.jena.ModelExtractor.StatementType;
import openllet.jena.graph.converter.AxiomConverter;
import openllet.jena.graph.loader.DefaultGraphLoader;
import openllet.jena.graph.loader.GraphLoader;
import openllet.jena.graph.query.GraphQueryHandler;
import openllet.shared.tools.Log;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.BaseInfGraph;
import org.apache.jena.reasoner.Finder;
import org.apache.jena.reasoner.StandardValidityReport;
import org.apache.jena.reasoner.TriplePattern;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Implementation of Jena InfGraph interface which is backed by Pellet reasoner.
 *
 * @author Evren Sirin
 */
public class PelletInfGraph extends BaseInfGraph
{
	public final static Logger _logger = Log.getLogger(PelletInfGraph.class);

	private static final Triple INCONCISTENCY_TRIPLE = Triple.create(OWL.Thing.asNode(), RDFS.subClassOf.asNode(), OWL.Nothing.asNode());

	private final KnowledgeBase _kb;
	private final ModelExtractor _extractor;
	private final PelletGraphListener _graphListener;

	private volatile GraphLoader _loader;
	private volatile Graph _deductionsGraph;
	private boolean _autoDetectChanges;
	private boolean _skipBuiltinPredicates;

	public PelletInfGraph(final KnowledgeBase kb, final PelletReasoner pellet, final GraphLoader loader)
	{
		this(kb, Factory.createDefaultGraph(), pellet, loader);
	}

	public PelletInfGraph(final Graph graph, final PelletReasoner pellet, final GraphLoader loader)
	{
		this(new KnowledgeBaseImpl(), graph, pellet, loader);
	}

	private PelletInfGraph(final KnowledgeBase kb, final Graph graph, final PelletReasoner pellet, final GraphLoader loader)
	{
		super(graph, pellet);

		_kb = kb;
		_loader = loader;

		_extractor = new ModelExtractor(kb);
		_extractor.setSelector(StatementType.ALL_PROPERTY_STATEMENTS);

		_graphListener = new PelletGraphListener(graph, kb, _autoDetectChanges);

		loader.setKB(kb);

		if (pellet.isFixedSchema())
		{
			loader.load(Collections.singleton(getSchemaGraph()));
			loader.setLoadTBox(false);
		}

		rebind();
	}

	public GraphLoader attachTemporaryGraph(final Graph tempGraph)
	{
		final GraphLoader savedLoader = _loader;

		final SimpleUnion unionGraph = (SimpleUnion) savedLoader.getGraph();
		unionGraph.addGraph(tempGraph);

		_loader = new DefaultGraphLoader();
		_loader.setGraph(unionGraph);
		_loader.setKB(_kb);
		_loader.preprocess();

		return savedLoader;
	}

	public void detachTemporaryGraph(final Graph tempGraph, final GraphLoader savedLoader)
	{
		final SimpleUnion unionGraph = (SimpleUnion) _loader.getGraph();
		unionGraph.removeGraph(tempGraph);
		_loader = savedLoader;
	}

	@Override
	public ExtendedIterator<Triple> find(final Node subject, final Node property, final Node object, final Graph param)
	{
		prepare();

		final GraphLoader savedLoader = attachTemporaryGraph(param);

		final ExtendedIterator<Triple> result = graphBaseFind(subject, property, object);

		detachTemporaryGraph(param, savedLoader);

		return result;
	}

	@Override
	public ExtendedIterator<Triple> findWithContinuation(final TriplePattern pattern, final Finder finder)
	{
		prepare();

		final Node subject = pattern.getSubject();
		final Node predicate = pattern.getPredicate();
		final Node object = pattern.getObject();

		ExtendedIterator<Triple> i = GraphQueryHandler.findTriple(_kb, this, subject, predicate, object);

		final ATerm predicateTerm = predicate.isURI() ? ATermUtils.makeTermAppl(predicate.getURI()) : null;
		// look at asserted triples at the _end but only for annotation properties, other triples should be inferred
		if (finder != null && (predicateTerm == null || !_kb.isObjectProperty(predicateTerm) && !_kb.isDatatypeProperty(predicateTerm)))
		{
			final TriplePattern tp = new TriplePattern(subject, predicate, object);
			i = i.andThen(finder.find(tp));
		}

		// make sure we don't have duplicates
		return i.filterKeep(new UniqueFilter<Triple>());
	}

	@Override
	public Graph getSchemaGraph()
	{
		return ((PelletReasoner) getReasoner()).getSchema();
	}

	@Override
	public synchronized boolean isPrepared()
	{
		return super.isPrepared() && (!_autoDetectChanges || !_graphListener.isChanged());
	}

	private void load()
	{
		_logger.fine("Loading triples");

		final Set<Graph> changedGraphs = _graphListener.getChangedGraphs();

		if (changedGraphs == null)
			reload();
		else
			load(changedGraphs);
	}

	/**
	 * Reloads all the triple from the underlying models regardless of updates or _current state. KB will be cleared completely and recreated from scratch.
	 */
	public void reload()
	{
		_logger.fine("Clearing the KB and reloading");

		clear();

		Set<Graph> graphs = _graphListener.getLeafGraphs();

		if (_loader.isLoadTBox())
		{
			final Graph schema = getSchemaGraph();
			if (schema != null)
			{
				graphs = new HashSet<>(graphs);
				graphs.add(schema);
			}
		}

		load(graphs);
	}

	private void load(final Iterable<Graph> graphs)
	{
		_loader.load(graphs);

		_loader.setGraph(new SimpleUnion(_graphListener.getLeafGraphs()));

		_graphListener.reset();

		_deductionsGraph = null;
	}

	@Override
	public synchronized void prepare()
	{
		prepare(true);
	}

	public void prepare(final boolean doConsistencyCheck)
	{
		if (isPrepared())
			return;

		_logger.fine("Preparing PelletInfGraph...");

		load();

		_kb.prepare();

		if (doConsistencyCheck)
			_kb.isConsistent();

		_logger.fine("done.");

		super.prepare();
	}

	public boolean isConsistent()
	{
		prepare();

		return _kb.isConsistent();
	}

	public boolean isClassified()
	{
		return super.isPrepared() && _kb.isClassified();
	}

	public boolean isRealized()
	{
		return super.isPrepared() && _kb.isRealized();
	}

	public void classify()
	{
		prepare();
		_kb.classify();
	}

	public void realize()
	{
		prepare();
		_kb.realize();
	}

	@Override
	@SuppressWarnings("deprecation")
	public Graph getDeductionsGraph()
	{
		if (!OpenlletOptions.RETURN_DEDUCTIONS_GRAPH)
			return null;

		classify();

		if (_deductionsGraph == null)
		{
			_logger.fine("Realizing PelletInfGraph...");
			_kb.realize();

			_logger.fine("Extract model...");

			final Model extractedModel = _extractor.extractModel();
			_deductionsGraph = extractedModel.getGraph();

			_logger.fine("done.");
		}

		return _deductionsGraph;
	}

	@Override
	protected boolean graphBaseContains(final Triple pattern)
	{
		if (getRawGraph().contains(pattern))
			return true;

		return containsTriple(pattern);
	}

	public boolean entails(final Triple pattern)
	{
		prepare();

		if (isSyntaxTriple(pattern))
			return true;

		if (isBnodeTypeQuery(pattern))
			return !containsTriple(Triple.create(pattern.getObject(), RDFS.subClassOf.asNode(), OWL.Nothing.asNode()));
		else
			return containsTriple(pattern);
	}

	public Model explainInconsistency()
	{
		return explainTriple(INCONCISTENCY_TRIPLE);
	}

	public Model explain(final Statement stmt)
	{
		return explainTriple(stmt.asTriple());
	}

	public Model explain(final Resource s, final Property p, final RDFNode o)
	{
		return explainTriple(Triple.create(s.asNode(), p.asNode(), o.asNode()));
	}

	private Model explainTriple(final Triple triple)
	{
		final Graph explanation = explain(triple);
		return explanation == null ? null : ModelFactory.createModelForGraph(explanation);
	}

	public Graph explain(final Triple pattern)
	{
		if (!pattern.equals(INCONCISTENCY_TRIPLE))
		{
			if (!pattern.isConcrete())
			{
				_logger.warning(() -> "Triple patterns with variables cannot be explained: " + pattern);
				return null;
			}

			if (isSyntaxTriple(pattern))
			{
				_logger.warning(() -> "Syntax triples cannot be explained: " + pattern);
				return null;
			}
		}

		prepare();

		final Graph explanationGraph = Factory.createDefaultGraph();

		_logger.fine(() -> "Explain " + pattern);

		if (checkEntailment(this, pattern, true))
		{
			final Set<ATermAppl> explanation = _kb.getExplanationSet();

			_logger.finer(() -> "Explanation " + formatAxioms(explanation));

			final Set<ATermAppl> prunedExplanation = pruneExplanation(pattern, explanation);

			_logger.finer(() -> "Pruned " + formatAxioms(prunedExplanation));

			final AxiomConverter converter = new AxiomConverter(_kb, explanationGraph);
			for (final ATermAppl axiom : prunedExplanation)
				converter.convert(axiom);
		}

		_logger.fine(() -> "Explanation " + explanationGraph);

		return explanationGraph;
	}

	private Set<ATermAppl> pruneExplanation(final Triple pattern, final Set<ATermAppl> explanation)
	{
		final Set<ATermAppl> prunedExplanation = new HashSet<>(explanation);

		final OntBuilder builder = new OntBuilder(_kb);
		KnowledgeBase copyKB;
		PelletInfGraph copyGraph;

		final GraphLoader loader = new DefaultGraphLoader();
		for (final ATermAppl axiom : explanation)
		{
			prunedExplanation.remove(axiom);

			copyKB = builder.build(prunedExplanation);
			copyGraph = new PelletInfGraph(copyKB, (PelletReasoner) getReasoner(), loader);

			if (!checkEntailment(copyGraph, pattern, false))
				prunedExplanation.add(axiom);
			else
				_logger.finer(() -> "Prune from explanation " + ATermUtils.toString(axiom));
		}

		return prunedExplanation;
	}

	private static boolean checkEntailment(final PelletInfGraph pellet, final Triple pattern, final boolean withExplanation)
	{
		final boolean doExplanation = pellet.getKB().doExplanation();
		pellet.getKB().setDoExplanation(withExplanation);

		boolean entailed = false;
		if (pattern.equals(INCONCISTENCY_TRIPLE))
			entailed = !pellet.isConsistent();
		else
			entailed = pellet.containsTriple(pattern);

		pellet.getKB().setDoExplanation(doExplanation);

		return entailed;
	}

	private static String formatAxioms(final Set<ATermAppl> axioms)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (final ATermAppl axiom : axioms)
		{
			sb.append(ATermUtils.toString(axiom));
			sb.append(",");
		}
		if (axioms.isEmpty())
			sb.append(']');
		else
			sb.setCharAt(sb.length() - 1, ']');
		return sb.toString();
	}

	protected boolean containsTriple(final Triple pattern)
	{
		prepare();

		final Node subject = pattern.getSubject();
		final Node predicate = pattern.getPredicate();
		final Node object = pattern.getObject();

		return GraphQueryHandler.containsTriple(_kb, _loader, subject, predicate, object);
	}

	private static boolean isSyntaxTriple(final Triple t)
	{
		BuiltinTerm builtin = BuiltinTerm.find(t.getPredicate());

		if (builtin != null)
		{
			if (builtin.isSyntax())
				return true;

			if (BuiltinTerm.isExpression(builtin) && (t.getSubject().isBlank() || t.getObject().isBlank()))
				return true;

			if (builtin.equals(BuiltinTerm.RDF_type))
			{
				builtin = BuiltinTerm.find(t.getObject());
				return builtin != null && builtin.isSyntax();
			}
		}

		return false;
	}

	private static boolean isBnodeTypeQuery(final Triple t)
	{
		return t.getSubject().isBlank() && t.getPredicate().equals(RDF.type.asNode()) && (BuiltinTerm.find(t.getObject()) == null || t.getObject().equals(OWL.Thing.asNode()) || t.getObject().equals(OWL.Nothing.asNode()));
	}

	/**
	 * Returns the underlying Pellet KnowledgeBase. Before calling this function make sure the graph {@link #isPrepared()} or use {@link #getPreparedKB()}.
	 */
	public KnowledgeBase getKB()
	{
		return _kb;
	}

	/**
	 * Returns the underlying Pellet KnowledgeBase after calling {@link #prepare()}.
	 */
	public KnowledgeBase getPreparedKB()
	{
		prepare();

		return getKB();
	}

	/**
	 * <p>
	 * Add one triple to the _data graph, mark the graph not-prepared, but don't run prepare() just yet.
	 * </p>
	 *
	 * @param t A triple to add to the graph
	 */
	@Override
	public synchronized void performAdd(final Triple t)
	{
		fdata.getGraph().add(t);
		setPreparedState(false);
	}

	/**
	 * <p>
	 * Delete one triple from the _data graph, mark the graph not-prepared, but don't run prepare() just yet.
	 * </p>
	 *
	 * @param t A triple to remove from the graph
	 */
	@Override
	public void performDelete(final Triple t)
	{
		fdata.getGraph().delete(t);
		setPreparedState(false);
	}

	/**
	 * <p>
	 * Test the consistency of the model. This looks for overall inconsistency, and for any unsatisfiable classes.
	 * </p>
	 *
	 * @return a ValidityReport structure
	 */
	@Override
	public ValidityReport validate()
	{
		checkOpen();
		prepare();
		final StandardValidityReport report = new StandardValidityReport();

		_kb.setDoExplanation(true);
		final boolean consistent = _kb.isConsistent();
		_kb.setDoExplanation(false);

		if (!consistent)
			report.add(true, "KB is inconsistent!", _kb.getExplanation());
		else
			for (final ATermAppl c : _kb.getUnsatisfiableClasses())
			{
				final String name = JenaUtils.makeGraphNode(c).toString();
				report.add(false, "Unsatisfiable class", name);
			}

		return report;
	}

	@Override
	public void clear()
	{
		if (_loader.isLoadTBox())
			_kb.clear();
		else
			_kb.clearABox();
		_loader.clear();
	}

	@Override
	public void close()
	{
		close(true);
	}

	public void close(final boolean recursive)
	{
		if (closed)
			return;

		if (recursive)
			super.close();
		else
			closed = true;

		if (_deductionsGraph != null)
		{
			_deductionsGraph.close();
			_deductionsGraph = null;
		}
		clear();
		_graphListener.dispose();
		_kb.clear();
	}

	/**
	 * @return the _loader
	 */
	public GraphLoader getLoader()
	{
		return _loader;
	}

	public boolean isAutoDetectChanges()
	{
		return _autoDetectChanges;
	}

	/**
	 * Sets auto detection of changes in the subgraphs associated with this model. If a graph or subgraph associated with this inference graph is updated out of
	 * band there is no way to know for the reasoner that the underlying _data has changed and reasoning results will be stale. When this option is turned
	 * Pellet will attach listeners to all the subgraphs and will be notified of all changes. It will also detect addition and removal of new subgraphs.
	 */
	public void setAutoDetectChanges(final boolean autoDetectChanges)
	{
		_autoDetectChanges = autoDetectChanges;

		_graphListener.setEnabled(autoDetectChanges);
	}

	public boolean isSkipBuiltinPredicates()
	{
		return _skipBuiltinPredicates;
	}

	public void setSkipBuiltinPredicates(final boolean skipBuiltinPredicates)
	{
		_skipBuiltinPredicates = skipBuiltinPredicates;
	}
}
