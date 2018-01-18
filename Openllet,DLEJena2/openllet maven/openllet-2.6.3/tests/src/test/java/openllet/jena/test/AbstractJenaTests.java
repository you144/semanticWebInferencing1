// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.jena.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasonerFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.After;
import org.junit.Before;

/**
 * @author Evren Sirin
 */

public abstract class AbstractJenaTests
{
	protected static final Resource _A = ResourceFactory.createResource("A");
	protected static final Resource _B = ResourceFactory.createResource("B");
	protected static final Resource _C = ResourceFactory.createResource("C");
	protected static final Resource _D = ResourceFactory.createResource("D");
	protected static final Resource _E = ResourceFactory.createResource("E");

	protected static final Resource _a = ResourceFactory.createResource("a");
	protected static final Resource _b = ResourceFactory.createResource("b");
	protected static final Resource _c = ResourceFactory.createResource("c");
	protected static final Resource _d = ResourceFactory.createResource("d");
	protected static final Resource _e = ResourceFactory.createResource("e");

	protected OntModel _model;
	protected OntModel _reasoner;

	@Before
	public void before()
	{
		_model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		_reasoner = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, _model);
		_reasoner.setStrictMode(false);
	}

	@After
	public void after()
	{
		_model.close();
	}

	protected void classes(final Resource... classes)
	{
		for (final Resource cls : classes)
			_model.add(cls, RDF.type, OWL.Class);
	}

	protected void objectProperties(final Resource... props)
	{
		for (final Resource p : props)
			_model.add(p, RDF.type, OWL.ObjectProperty);
	}

	protected void dataProperties(final Resource... props)
	{
		for (final Resource p : props)
			_model.add(p, RDF.type, OWL.DatatypeProperty);
	}

	protected void annotationProperties(final Resource... props)
	{
		for (final Resource p : props)
			_model.add(p, RDF.type, OWL.AnnotationProperty);
	}

	protected void individuals(final Resource... inds)
	{
		for (final Resource ind : inds)
			_model.add(ind, RDF.type, OWL.Thing);
	}

	public Resource oneOf(final Resource... terms)
	{
		return _model.createEnumeratedClass(null, _model.createList(terms));
	}

	public Resource not(final Resource cls)
	{
		return _model.createComplementClass(null, cls);
	}

	public void assertConsistent()
	{
		assertTrue(((PelletInfGraph) _reasoner.getGraph()).isConsistent());
	}

	public void assertInconsistent()
	{
		assertFalse(((PelletInfGraph) _reasoner.getGraph()).isConsistent());
	}
}
