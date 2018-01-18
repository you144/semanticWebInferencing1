// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import openllet.owlapi.OWL;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import openllet.owlapi.explanation.io.manchester.ManchesterSyntaxExplanationRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * <p>
 * Title: ExplanationExample
 * </p>
 * <p>
 * Description: This program shows how to use Pellet's clashExplanation service
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 * @author Evren Sirin
 */
public class ExplanationExample
{

	private static final String file = "file:src/main/resources/data/people+pets.owl";
	private static final String NS = "http://cohse.semanticweb.org/ontologies/people#";

	public void run() throws OWLOntologyCreationException, OWLException, IOException
	{
		PelletExplanation.setup();

		// The renderer is used to pretty print clashExplanation
		final ManchesterSyntaxExplanationRenderer renderer = new ManchesterSyntaxExplanationRenderer();
		// The writer used for the clashExplanation rendered
		final PrintWriter out = new PrintWriter(System.out);
		renderer.startRendering(out);

		// Create an OWLAPI manager that allows to load an ontology file and
		// create OWLEntities
		final OWLOntologyManager manager = OWL._manager;
		final OWLOntology ontology = manager.loadOntology(IRI.create(file));

		// Create the reasoner and load the ontology
		final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

		// Create an clashExplanation generator
		final PelletExplanation expGen = new PelletExplanation(reasoner);

		// Create some concepts
		final OWLClass madCow = OWL.Class(NS + "mad+cow");
		final OWLClass animalLover = OWL.Class(NS + "animal+lover");
		final OWLClass petOwner = OWL.Class(NS + "pet+owner");

		// Explain why mad cow is an unsatisfiable concept
		Set<Set<OWLAxiom>> exp = expGen.getUnsatisfiableExplanations(madCow);
		out.println("Why is " + madCow + " concept unsatisfiable?");
		renderer.render(exp);

		// Now explain why animal lover is a sub class of pet owner
		exp = expGen.getSubClassExplanations(animalLover, petOwner);
		out.println("Why is " + animalLover + " subclass of " + petOwner + "?");
		renderer.render(exp);

		renderer.endRendering();
	}

	public static void main(final String[] args) throws OWLOntologyCreationException, OWLException, IOException
	{
		final ExplanationExample app = new ExplanationExample();

		app.run();
	}
}
