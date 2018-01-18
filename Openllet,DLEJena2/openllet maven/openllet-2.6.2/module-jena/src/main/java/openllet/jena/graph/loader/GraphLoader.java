// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.jena.graph.loader;

import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.exceptions.UnsupportedFeatureException;
import openllet.core.utils.progress.ProgressMonitor;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

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
public interface GraphLoader
{
	/**
	 * Clear all internal caches (essentially used for mapping bnodes to ATerm structures)
	 */
	public void clear();

	/**
	 * Get the Jena graph used in the loader.
	 *
	 * @return
	 */
	public Graph getGraph();

	/**
	 * Returns the unsupported axioms ignored by the loader.
	 *
	 * @return
	 */
	public Set<String> getUnpportedFeatures();

	/**
	 * Load the axioms from the Jena graphs to the given KB.
	 *
	 * @throws UnsupportedFeatureException
	 */
	public void load(final Iterable<Graph> graphs) throws UnsupportedFeatureException;

	/**
	 * Translate the given graph _node into an ATerm object.
	 *
	 * @param _node
	 * @return
	 */
	public ATermAppl node2term(final Node node);

	/**
	 * Do the preprocessing steps necessary to _cache any information that will be used for loading.
	 */
	public void preprocess();

	/**
	 * Set the graph that will be used during loading.
	 *
	 * @param graph
	 */
	public void setGraph(final Graph graph);

	/**
	 * Returns if the loader will load the Abox triples.
	 *
	 * @return boolean value indicating if ABox triples will be loaded
	 * @see #setLoadABox(boolean)
	 */
	public boolean isLoadABox();

	/**
	 * Sets the flag that tells the loader to skip ABox (instance) statements. Only TBox (class) and RBox (property) axioms will be loaded. Improves loading
	 * performance even if there are no ABox statements because lets the loader to ignore annotations.
	 *
	 * @param loadABox
	 */
	public void setLoadABox(final boolean loadABox);

	public boolean isLoadTBox();

	public void setLoadTBox(final boolean loadTBox);

	/**
	 * Returns if the loader will preprocess rdf:type triples.
	 *
	 * @return boolean value indicating if rdf:type triples will be preprocessed
	 * @see #setPreprocessTypeTriples(boolean)
	 */
	public boolean isPreprocessTypeTriples();

	/**
	 * This option forces the loader to process type triples before processing other triples. Not preprocessing the type triples improves loading time 5% to 10%
	 * but might cause problems too. For example, without preprocessing the type triples a triple (s p "o") might be loaded as a datatype assertion (thinking s
	 * is an _individual and p is a datatype property) whereas (s rdf:type owl:Class) and (p rdf:type owl:AnnotiationProperty) triples have not yet been
	 * processed. These problems depend on the _order triples are processed and highly unpredictable. Loading the schema first with preprocessing and loading
	 * the instance _data without preprocessing would be a viable option if schema and instance _data are in separate files.
	 */
	public void setPreprocessTypeTriples(final boolean preprocessTypeTriples);

	/**
	 * Set the progress monitor that will show the load progress.
	 *
	 * @param monitor
	 */
	public void setProgressMonitor(final ProgressMonitor monitor);

	public void setKB(final KnowledgeBase kb);
}
