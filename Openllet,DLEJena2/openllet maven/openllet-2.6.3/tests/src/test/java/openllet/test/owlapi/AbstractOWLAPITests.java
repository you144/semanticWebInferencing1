// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.owlapi;

import static openllet.owlapi.OWL.AnonymousIndividual;
import static openllet.owlapi.OWL.Class;
import static openllet.owlapi.OWL.DataProperty;
import static openllet.owlapi.OWL.Individual;
import static openllet.owlapi.OWL.ObjectProperty;
import static openllet.owlapi.OWL.constant;

import java.util.Collections;
import openllet.owlapi.OWL;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.test.PelletTestSuite;
import org.junit.After;
import org.junit.Before;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public abstract class AbstractOWLAPITests
{
	public static String _base = "file:" + PelletTestSuite.base + "misc/";

	protected static final OWLClass _A = Class("A");
	protected static final OWLClass _B = Class("B");
	protected static final OWLClass _C = Class("C");
	protected static final OWLClass _D = Class("D");
	protected static final OWLClass _E = Class("E");
	protected static final OWLClass _F = Class("F");
	protected static final OWLObjectProperty _p = ObjectProperty("p");
	protected static final OWLObjectProperty _q = ObjectProperty("q");
	protected static final OWLObjectProperty _r = ObjectProperty("r");
	protected static final OWLDataProperty _dp = DataProperty("dp");
	protected static final OWLDataProperty _dq = DataProperty("dq");
	protected static final OWLDataProperty _dr = DataProperty("dr");
	protected static final OWLNamedIndividual _a = Individual("a");
	protected static final OWLNamedIndividual _b = Individual("b");
	protected static final OWLNamedIndividual _c = Individual("c");
	protected static final OWLAnonymousIndividual _anon = AnonymousIndividual();
	protected static final OWLLiteral _lit = constant("lit");

	private final OWLOntologyManager _manager = OWLManager.createOWLOntologyManager();
	protected volatile OWLOntology _ontology;
	protected volatile OpenlletReasoner _reasoner;

	public void createReasoner(final OWLAxiom... axioms)
	{
		_manager.clearOntologies();
		_ontology = OWL.Ontology(_manager, axioms);
		_reasoner = OpenlletReasonerFactory.getInstance().createReasoner(_ontology);
	}

	@Before
	public void setUp()
	{
		//
	}

	@After
	public void after()
	{
		_manager.clearOntologies();
		_ontology = null;
		if (_reasoner != null)
			_reasoner.dispose();
	}

	protected boolean processAdd(final OWLAxiom axiom)
	{
		return processChange(new AddAxiom(_ontology, axiom));
	}

	protected boolean processRemove(final OWLAxiom axiom)
	{
		return processChange(new RemoveAxiom(_ontology, axiom));
	}

	protected boolean processChange(final OWLOntologyChange change)
	{
		return _reasoner.processChanges(Collections.singletonList(change));
	}

}
