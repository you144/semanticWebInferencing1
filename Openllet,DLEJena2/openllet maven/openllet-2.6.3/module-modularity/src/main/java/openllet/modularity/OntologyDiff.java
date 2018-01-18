// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.modularity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

/**
 * <p>
 * Title: Computes differences between two ontologies, sets of ontologies or collections of axioms.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class OntologyDiff
{
	/**
	 * The list of axioms that were added in the second ontology (with respect to the first ontology).
	 */
	private final List<OWLAxiom> additions = new ArrayList<>();

	/**
	 * The list of axioms that were removed in the second ontology (with respect to the first ontology).
	 */
	private final List<OWLAxiom> deletions = new ArrayList<>();

	/**
	 * Private constructor to ensure that the objects of this class can only be created via the appropriate static methods.
	 */
	private OntologyDiff()
	{
		// Avoid instantiation.
	}

	/**
	 * Computes the difference between two ontologies.
	 *
	 * @param initialOnt the initial (first) ontology
	 * @param finalOnt the final (second or later) ontology
	 * @return the difference between the initial and final ontology
	 */
	public static OntologyDiff diffOntologies(final OWLOntology initialOnt, final OWLOntology finalOnt)
	{
		final OntologyDiff result = new OntologyDiff();
		initialOnt.axioms().filter(axiom -> !finalOnt.containsAxiom(axiom)).forEach(result.deletions::add);
		finalOnt.axioms().filter(axiom -> !initialOnt.containsAxiom(axiom)).forEach(result.additions::add);
		return result;
	}

	/**
	 * Computes the difference between a collection of ontologies and a collection of axioms
	 *
	 * @param initialOntologies the initial (first) ontologies
	 * @param finalAxioms the final set of axioms (the equivalent of second ontology)
	 * @return the difference in axioms
	 */
	public static OntologyDiff diffOntologiesWithAxioms(final Collection<OWLOntology> initialOntologies, final Collection<OWLAxiom> finalAxioms)
	{
		final OntologyDiff result = new OntologyDiff();

		for (final OWLOntology ontology : initialOntologies)
			ontology.axioms().filter(axiom -> !finalAxioms.contains(axiom)).forEach(result.deletions::add);

		for (final OWLAxiom axiom : finalAxioms)
			if (!containsAxiom(axiom, initialOntologies))
				result.additions.add(axiom);

		return result;
	}

	/**
	 * Computes the difference between a set of axioms and an ontology.
	 *
	 * @param initialAxioms the initial set of axioms (the equivalent of the first ontology)
	 * @param finalOntologies the final set of ontologies
	 * @return the difference in axioms
	 */
	public static OntologyDiff diffAxiomsWithOntologies(final Stream<OWLAxiom> initialAxioms, final Collection<OWLOntology> finalOntologies)
	{
		final OntologyDiff result = new OntologyDiff();

		final List<OWLAxiom> axioms = initialAxioms.collect(Collectors.toList());

		axioms.stream().filter(axiom -> !containsAxiom(axiom, finalOntologies)).forEach(result.deletions::add);

		for (final OWLOntology ontology : finalOntologies)
			ontology.axioms().filter(axiom -> !axioms.contains(axiom)).forEach(result.additions::add);

		return result;
	}

	public static OntologyDiff diffAxiomsWithOntologies(final Collection<OWLAxiom> initialAxioms, final Collection<OWLOntology> finalOntologies)
	{
		return diffAxiomsWithOntologies(initialAxioms.stream(), finalOntologies);
	}

	/**
	 * Checks whether a collection of ontologies contains a specific axiom
	 *
	 * @param axiom the axiom whose presence should be checked
	 * @param ontologies the collection of ontologies amongst which the presence of the axiom should be checked
	 * @return true if there is at least one ontology in the collection that contains this axiom, false otherwise
	 */
	private static boolean containsAxiom(final OWLAxiom axiom, final Collection<OWLOntology> ontologies)
	{
		for (final OWLOntology ontology : ontologies)
			if (ontology.containsAxiom(axiom))
				return true;

		return false;
	}

	/**
	 * Computes the difference between two sets of axioms.
	 *
	 * @param initialAxioms the first (initial) set of axioms
	 * @param finalAxioms the second (final) set of axioms
	 * @return the difference between the sets of axioms
	 */
	public static OntologyDiff diffAxioms(final Collection<OWLAxiom> initialAxioms, final Collection<OWLAxiom> finalAxioms)
	{
		final OntologyDiff result = new OntologyDiff();

		for (final OWLAxiom axiom : initialAxioms)
			if (!finalAxioms.contains(axiom))
				result.deletions.add(axiom);

		for (final OWLAxiom axiom : finalAxioms)
			if (!initialAxioms.contains(axiom))
				result.additions.add(axiom);

		return result;
	}

	public static OntologyDiff diffAxioms(final Stream<OWLAxiom> initialAxioms, final Collection<OWLAxiom> finalAxioms)
	{
		return diffAxioms(initialAxioms.collect(Collectors.toList()), finalAxioms);
	}

	/**
	 * Checks whether the two compared ontologies were the same (i.e., there are no differences).
	 *
	 * @return true if there were no differences between the compared ontologies, false otherwise
	 */
	public boolean areSame()
	{
		return additions.isEmpty() && deletions.isEmpty();
	}

	/**
	 * Gets the number of differences (in terms of numbers of axioms) between the compared ontologies.
	 *
	 * @return the number of axioms that were different (or more exactly the number of axioms that were added plus the number of axioms that were removed).
	 */
	public int getDiffCount()
	{
		return additions.size() + deletions.size();
	}

	/**
	 * Gets the list of axioms that were added to the second ontology with respect to the first ontology.
	 *
	 * @return a set of axioms that existed in the second ontology, and did not exist in the first one.
	 */
	public Collection<OWLAxiom> getAdditions()
	{
		return additions;
	}

	/**
	 * Produces a list of ontology change objects that if applied to the initial ontology, would convert that initial ontology into the final ontology.
	 *
	 * @param initialOnt the initial ontology (just for the purposes of creating OWLOntologyChange objects).
	 * @return a list of ontology change objects.
	 */
	public Collection<OWLOntologyChange> getChanges(final OWLOntology initialOnt)
	{
		final List<OWLOntologyChange> changes = new ArrayList<>();

		for (final OWLAxiom axiom : additions)
			changes.add(new AddAxiom(initialOnt, axiom));

		for (final OWLAxiom axiom : deletions)
			changes.add(new RemoveAxiom(initialOnt, axiom));

		return changes;
	}

	/**
	 * Identifies one ontology in a collection of ontologies, contains a given axiom. If more than one ontology contains that axiom, only the first analyzed
	 * ontology will be returned.
	 *
	 * @param axiom the axiom to be searched in the collection of ontologies
	 * @param ontologies the collection of ontologies
	 * @return an ontology that contains the axiom, or null if no ontology contains that axiom
	 */
	private static OWLOntology identifyAxiomOntology(final OWLAxiom axiom, final Collection<OWLOntology> ontologies)
	{
		for (final OWLOntology ontology : ontologies)
			if (ontology.containsAxiom(axiom))
				return ontology;

		return null;
	}

	/**
	 * Produces a list of ontology change objects that if applied to the set of initial ontologies, would convert that set of ontologies into the final
	 * ontology.
	 *
	 * @param ontologies the set of initial ontologies (just for the purposes of creating OWLOntologyChange objects)
	 * @return a list of ontology change objects.
	 */
	public Collection<OWLOntologyChange> getChanges(final Collection<OWLOntology> ontologies)
	{
		final List<OWLOntologyChange> changes = new ArrayList<>();

		for (final OWLAxiom axiom : additions)
		{
			final OWLOntology ontology = identifyAxiomOntology(axiom, ontologies);

			if (null == ontology)
				throw new IllegalArgumentException("None of the ontologies contain the added axiom");

			changes.add(new AddAxiom(ontology, axiom));
		}

		for (final OWLAxiom axiom : deletions)
		{
			OWLOntology ontology = null;

			if (ontologies.isEmpty())
				throw new IllegalArgumentException("There are no ontologies defined that could have contained the removed axiom");

			ontology = ontologies.iterator().next();

			changes.add(new RemoveAxiom(ontology, axiom));
		}

		return changes;
	}

	/**
	 * Gets the list of axioms that were deleted in the second ontology with respect to the first ontology.
	 *
	 * @return a set of axioms that existed in the first ontology, and do not exist in the second one.
	 */
	public Collection<OWLAxiom> getDeletions()
	{
		return deletions;
	}
}
