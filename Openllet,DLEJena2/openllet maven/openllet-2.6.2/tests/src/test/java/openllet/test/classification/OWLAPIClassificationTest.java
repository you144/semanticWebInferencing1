// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.classification;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import openllet.owlapi.OWL;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * @author Evren Sirin
 */
public class OWLAPIClassificationTest extends AbstractClassificationTest
{
	@Override
	public void testClassification(final String inputOnt, final String classifiedOnt) throws OWLOntologyCreationException
	{
		final OWLOntology premise = OWL._manager.loadOntology(IRI.create(inputOnt));
		final OWLOntology conclusion = OWL._manager.loadOntology(IRI.create(classifiedOnt));

		try
		{
			final OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(premise);
			reasoner.getKB().classify();

			final List<OWLAxiom> nonEntailments = new ArrayList<>();

			{
				final Iterable<OWLSubClassOfAxiom> it = conclusion.axioms(AxiomType.SUBCLASS_OF)::iterator;
				for (final OWLSubClassOfAxiom axiom : it)
				{
					final boolean entailed = reasoner.getSubClasses(axiom.getSuperClass(), true).containsEntity((OWLClass) axiom.getSubClass());

					if (!entailed)
						if (AbstractClassificationTest.FAIL_AT_FIRST_ERROR)
							fail("Not entailed: " + axiom);
						else
							nonEntailments.add(axiom);
				}
			}

			{
				final Iterable<OWLEquivalentClassesAxiom> it = conclusion.axioms(AxiomType.EQUIVALENT_CLASSES)::iterator;
				for (final OWLEquivalentClassesAxiom axiom : it)
				{
					final boolean entailed = reasoner.isEntailed(axiom);

					if (!entailed)
						if (AbstractClassificationTest.FAIL_AT_FIRST_ERROR)
							fail("Not entailed: " + axiom);
						else
							nonEntailments.add(axiom);
				}
			}

			{
				final Iterable<OWLClassAssertionAxiom> it = conclusion.axioms(AxiomType.CLASS_ASSERTION)::iterator;
				for (final OWLClassAssertionAxiom axiom : it)
				{
					final boolean entailed = reasoner.getInstances(axiom.getClassExpression(), true).containsEntity((OWLNamedIndividual) axiom.getIndividual());

					if (!entailed)
						if (AbstractClassificationTest.FAIL_AT_FIRST_ERROR)
							fail("Not entailed: " + axiom);
						else
							nonEntailments.add(axiom);
				}
			}

			assertTrue(nonEntailments.size() + " " + nonEntailments.toString(), nonEntailments.isEmpty());
		}
		finally
		{
			OWL._manager.removeOntology(premise);
			OWL._manager.removeOntology(conclusion);
		}
	}

}
