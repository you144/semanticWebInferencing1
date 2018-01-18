// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.owlapi.explanation.io.rdfxml;

import com.clarkparsia.owlapi.explanation.io.ExplanationRenderer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import openllet.owlapi.OWL;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLRenderer;

/**
 * <p>
 * Title:
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
 * @author Evren Sirin
 */
public class RDFXMLExplanationRenderer implements ExplanationRenderer
{
	private Set<OWLAxiom> _axioms;

	private Writer _writer;

	@Override
	public void startRendering(final Writer writer)
	{
		this._writer = writer;
		_axioms = new HashSet<>();
	}

	@Override
	public void render(final OWLAxiom axiom, final Set<Set<OWLAxiom>> explanations)
	{
		_axioms.add(axiom);

		for (final Set<OWLAxiom> explanation : explanations)
			_axioms.addAll(explanation);
	}

	@Override
	public void endRendering() throws IOException
	{
		final OWLOntology ontology = OWL.Ontology(_axioms);
		final RDFXMLRenderer renderer = new RDFXMLRenderer(ontology, new PrintWriter(_writer));
		renderer.render();
	}
}
