// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.owlapi;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC.
 * </p>
 *
 * @author Evren Sirin
 */
public class OpenlletReasonerFactory implements OWLReasonerFactory
{
	private static final OpenlletReasonerFactory INSTANCE = new OpenlletReasonerFactory();

	/**
	 * Returns a static factory instance that can be used to create reasoners.
	 *
	 * @return a static factory instance
	 */
	public static OpenlletReasonerFactory getInstance()
	{
		return INSTANCE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getReasonerName()
	{
		return "Openllet";
	}

	@Override
	public String toString()
	{
		return getReasonerName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OpenlletReasoner createReasoner(final OWLOntology ontology)
	{
		return new PelletReasoner(ontology, BufferingMode.BUFFERING);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OpenlletReasoner createReasoner(final OWLOntology ontology, final OWLReasonerConfiguration config) throws IllegalConfigurationException
	{
		return new PelletReasoner(ontology, config, BufferingMode.BUFFERING);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OpenlletReasoner createNonBufferingReasoner(final OWLOntology ontology)
	{
		return new PelletReasoner(ontology, BufferingMode.NON_BUFFERING);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OpenlletReasoner createNonBufferingReasoner(final OWLOntology ontology, final OWLReasonerConfiguration config) throws IllegalConfigurationException
	{
		return new PelletReasoner(ontology, config, BufferingMode.NON_BUFFERING);
	}
}
