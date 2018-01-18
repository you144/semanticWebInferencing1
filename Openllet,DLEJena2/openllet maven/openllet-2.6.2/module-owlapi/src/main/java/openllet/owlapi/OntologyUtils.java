// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.owlapi;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import openllet.atom.OpenError;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */

public class OntologyUtils
{
	@Deprecated
	private static final OWLOntologyManager _manager = OWL._manager;

	@Deprecated
	public static void addAxioms(final OWLOntology ontology, final Collection<? extends OWLAxiom> axioms)
	{
		addAxioms(ontology, axioms.stream());
	}

	@Deprecated
	public static void addAxioms(final OWLOntology ontology, final Stream<? extends OWLAxiom> axioms)
	{
		updateOntology(ontology, axioms, true);
	}

	@Deprecated
	public static void addAxioms(final OWLOntology ontology, final OWLAxiom... axioms)
	{
		addAxioms(ontology, Arrays.asList(axioms));
	}

	@Deprecated
	public static boolean containsClass(final Set<Set<OWLClass>> classes, final OWLClass cls)
	{
		for (final Set<OWLClass> set : classes)
			if (set.contains(cls))
				return true;

		return false;
	}

	/**
	 * Given an axiom, return its signature.
	 *
	 * @deprecated 2.5.1 use the stream version
	 */
	@Deprecated
	public static Set<OWLEntity> getSignature(final OWLAxiom axiom)
	{
		return axiom.getSignature();
	}

	@Deprecated
	public static Stream<OWLEntity> signature(final OWLAxiom axiom)
	{
		return axiom.signature();
	}

	@Deprecated
	public static OWLOntologyManager getOWLOntologyManager()
	{
		return _manager;
	}

	@Deprecated
	public static void clearOWLOntologyManager()
	{
		_manager.clearOntologies();
	}

	/**
	 * Loads the ontology with given URI.
	 *
	 * @param uri the ontology uri
	 * @return the ontology
	 */
	@Deprecated
	public static OWLOntology loadOntology(final String uri)
	{
		return loadOntology(_manager, uri);
	}

	public static OWLOntology loadOntology(final OWLOntologyManager manager, final String uri)
	{
		try
		{
			return manager.loadOntology(IRI.create(uri));
		}
		catch (final OWLOntologyCreationException e)
		{
			throw new OpenError(e);
		}
	}

	/**
	 * Loads the ontology with given URI.
	 *
	 * @param inputStream input ontology
	 * @return the ontology
	 */
	@Deprecated
	public static OWLOntology loadOntology(final InputStream inputStream)
	{
		return loadOntology(_manager, inputStream);
	}

	public static OWLOntology loadOntology(final OWLOntologyManager manager, final InputStream inputStream)
	{
		try
		{
			return manager.loadOntologyFromOntologyDocument(inputStream);
		}
		catch (final OWLOntologyCreationException e)
		{
			throw new OpenError(e);
		}
	}

	/**
	 * Loads the ontology with given URI and optionally removes all annotations leaving only logical axioms.
	 *
	 * @see #removeAllAnnotations(OWLOntology, OWLOntologyManager)
	 * @param uri the ontology uri
	 * @param withAnnotations if <code>false</code> removes all annotation axioms from the ontology, otherwise leaves the ontology intact
	 * @return the ontology
	 */
	@Deprecated
	public static OWLOntology loadOntology(final String uri, final boolean withAnnotations)
	{
		return loadOntology(_manager, uri, withAnnotations);
	}

	public static OWLOntology loadOntology(final OWLOntologyManager manager, final String uri, final boolean withAnnotations)
	{
		final OWLOntology ont = loadOntology(manager, uri);

		if (!withAnnotations)
			removeAllAnnotations(ont);

		return ont;
	}

	/**
	 * Loads the ontology with given URI and optionally removes all annotations leaving only logical axioms.
	 *
	 * @see #removeAllAnnotations(OWLOntology, OWLOntologyManager)
	 * @param inputStream input stream
	 * @param withAnnotations if <code>false</code> removes all annotation axioms from the ontology, otherwise leaves the ontology intact
	 * @return the ontology
	 */
	@Deprecated
	public static OWLOntology loadOntology(final InputStream inputStream, final boolean withAnnotations)
	{
		return loadOntology(_manager, inputStream, withAnnotations);
	}

	public static OWLOntology loadOntology(final OWLOntologyManager manager, final InputStream inputStream, final boolean withAnnotations)
	{
		final OWLOntology ont = loadOntology(manager, inputStream);

		if (!withAnnotations)
			removeAllAnnotations(ont);

		return ont;
	}

	/**
	 * Prints a set of axioms to console
	 *
	 * @Deprecated 2.5.1 because we don't want a System.out in the lib
	 */
	@Deprecated
	public static void printAxioms(final Collection<? extends OWLAxiom> axioms)
	{
		for (final OWLAxiom axiom : axioms)
			System.out.println(axiom);
	}

	/**
	 * Prints an ontology to console
	 *
	 * @Deprecated 2.5.1 because we don't want a System.out in the lib
	 */
	@Deprecated
	public static void printOntology(final OWLOntology ont)
	{
		ont.axioms().map(OWLAxiom::toString).sorted().forEach(System.out::println);
	}

	public static void removeAxioms(final OWLOntology ontology, final Collection<? extends OWLAxiom> axioms)
	{
		updateOntology(ontology, axioms, false);
	}

	@Deprecated
	public static void removeAxioms(final OWLOntology ontology, final OWLAxiom... axioms)
	{
		removeAxioms(ontology, Arrays.asList(axioms));
	}

	@Deprecated
	public static void save(final OWLOntology ont, final String path) throws OWLOntologyStorageException
	{
		_manager.saveOntology(ont, IRI.create(new File(path).toURI()));
	}

	/**
	 * Update the ontology by adding or removing the given set of axioms
	 *
	 * @param ontology target ontology
	 * @param axioms the axiom to add/remove
	 * @param add true - add; false - delete
	 */
	public static void updateOntology(final OWLOntology ontology, final Stream<? extends OWLAxiom> axioms, final boolean add)
	{
		if (add)
			ontology.addAxioms(axioms);
		else
			ontology.removeAxioms(axioms);
	}

	public static void updateOntology(final OWLOntology ontology, final Collection<? extends OWLAxiom> axioms, final boolean add)
	{
		updateOntology(ontology, axioms.stream(), add);
	}

	/**
	 * Determines if a class description contains any unreferenced entities with respect to the ontology that contains the entailments which are being
	 * explained.
	 *
	 * @param desc The description to be searched
	 * @return <code>true</code> if the description references entities that the ontology that contains entailments which are being explained, otherwise
	 *         <code>false</code>
	 */
	public static boolean containsUnreferencedEntity(final OWLOntology ontology, final OWLClassExpression desc)
	{
		for (final OWLEntity entity : desc.signature().collect(Collectors.toList()))
			if (!ontology.containsEntityInSignature(entity))
			{
				if (entity instanceof OWLClass && (((OWLClass) entity).isOWLThing() || ((OWLClass) entity).isOWLNothing()))
					continue;
				return true;
			}
		return false;
	}

	/**
	 * Removes an axiom from all the given ontologies that contains the axiom and returns those ontologies.
	 *
	 * @param axiom axiom being removed
	 * @param ontologies ontologies from which axiom is being removed
	 * @param _manager _manager to apply the actual change
	 * @return set of ontologies that have been affected
	 */
	public static Set<OWLOntology> removeAxiom(final OWLAxiom axiom, final Set<OWLOntology> ontologies, final OWLOntologyManager manager)
	{
		final Set<OWLOntology> modifiedOnts = new HashSet<>();

		for (final OWLOntology ont : ontologies)
			if (ont.containsAxiom(axiom))
			{
				modifiedOnts.add(ont);

				manager.applyChange(new RemoveAxiom(ont, axiom));
			}

		return modifiedOnts;
	}

	/**
	 * Add the axiom to all the given ontologies.
	 *
	 * @param axiom
	 * @param ontologies
	 * @param _manager
	 */
	public static void addAxiom(final OWLAxiom axiom, final Set<OWLOntology> ontologies, final OWLOntologyManager manager)
	{
		for (final OWLOntology ont : ontologies)
			manager.applyChange(new AddAxiom(ont, axiom));
	}

	/**
	 * Removes all annotations (non-logical axioms) from the ontology causing the ontology to be changed in an unreversible way. For any entity that is only
	 * referenced in an annotation but no logical axiom a declaration is added so that the referenced entities by the ontology remain same. Annotations have no
	 * semantic importance and can be ignored for reasoning purposes including generating explanations and computing modules. Removing them from the ontology
	 * completely reduces the memory requirements which is very high for large-scale annotation-heavy ontologies.
	 *
	 * @param ontology the ontology being changed
	 */
	public static void removeAllAnnotations(final OWLOntology ontology)
	{
		try
		{
			final Set<OWLEntity> referencedEntities = Stream.concat(//
					Stream.concat(ontology.classesInSignature(), ontology.objectPropertiesInSignature()), //
					Stream.concat(ontology.dataPropertiesInSignature(), ontology.individualsInSignature())//
			).collect(Collectors.toSet());

			// Remove every thing that is not logical.
			ontology.removeAxioms(ontology.axioms().filter(axiom -> !axiom.isLogicalAxiom()));

			final OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();

			// Add exactly once declaration per entity.
			ontology.addAxioms(referencedEntities.stream()//
					.filter(entity -> !ontology.containsEntityInSignature(entity))//
					.map(factory::getOWLDeclarationAxiom)//
			);
		}
		catch (final OWLOntologyChangeException e)
		{
			throw new OWLRuntimeException(e);
		}
	}

	/**
	 * Finds an entity (class, _individual, object or _data property) in the given set of ontologies that has the given local name or URI.
	 *
	 * @see #findEntity(String, OWLOntology)
	 * @param name URI or local name for an entity
	 * @param ontologies ontologies we are searching
	 * @return an entity referenced in the given ontology that has the given URI or local name
	 */
	public static OWLEntity findEntity(final String name, final Stream<OWLOntology> ontologies)
	{
		return ontologies//
				.map(ontology -> findEntity(name, ontology))//
				.filter(entity -> entity != null)//
				.findAny().orElse(null);
	}

	@Deprecated
	public static OWLEntity findEntity(final String name, final Set<OWLOntology> ontologies)
	{
		OWLEntity entity = null;
		for (final OWLOntology ontology : ontologies)
			if ((entity = findEntity(name, ontology)) != null)
				break;
		return entity;
	}

	/**
	 * Finds an entity (class, _individual, object or _data property) in the given ontology that has the given local name or URI. If the given name is not an
	 * absolute URI we use the logical URI of the ontology as the namespace and search for an entity with that URI. If the URI is punned in the ontology , e.g.
	 * used both as a class and as an _individual, any one of the punned entities may be returned.
	 *
	 * @param name URI or local name for an entity
	 * @param ontology ontology we are searching
	 * @return an entity referenced in the given ontology that has the given URI or local name
	 */
	public static OWLEntity findEntity(final String name, final OWLOntology ontology)
	{
		OWLEntity entity = null;

		if ("owl:Thing".equals(name))
			entity = OWL.Thing;
		else
			if ("owl:Nothing".equals(name))
				entity = OWL.Nothing;
			else
			{
				IRI iri = IRI.create(name);

				if (iri == null)
					throw new OpenError("Invalid IRI: " + iri);

				if (!iri.isAbsolute())
				{
					final IRI baseIRI = ontology.getOntologyID().getOntologyIRI().orElse(null);
					if (baseIRI != null)
						iri = baseIRI.resolve("#" + iri);
				}

				if (ontology.containsClassInSignature(iri, Imports.EXCLUDED))
					entity = OWL.Class(iri);
				else
					if (ontology.containsObjectPropertyInSignature(iri, Imports.EXCLUDED))
						entity = OWL.ObjectProperty(iri);
					else
						if (ontology.containsDataPropertyInSignature(iri, Imports.EXCLUDED))
							entity = OWL.DataProperty(iri);
						else
							if (ontology.containsIndividualInSignature(iri, Imports.EXCLUDED))
								entity = OWL.Individual(iri).asOWLNamedIndividual();
			}

		return entity;
	}
}
