// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.modularity;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import openllet.core.expressivity.Expressivity;
import openllet.core.taxonomy.Taxonomy;
import openllet.core.utils.MultiValueMap;
import openllet.core.utils.Timers;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

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
public interface ModuleExtractor
{
	/**
	 * Adds an axiom to the extractor.
	 */
	public void addAxiom(final OWLAxiom axiom);

	/**
	 * Adds all the axioms to the extractor.
	 */
	public void addAxioms(final Stream<OWLAxiom> axioms);

	@Deprecated
	public void addAxioms(final Iterable<OWLAxiom> axioms);

	/**
	 * Returns if the extracted modules can be updated. Returns false if the initial module extraction has not been performed yet.
	 *
	 * @return
	 */
	public boolean canUpdate();

	/**
	 * Deletes an axiom from the extractor.
	 *
	 * @param axiom
	 */
	public void deleteAxiom(final OWLAxiom axiom);

	/**
	 * Extract modules for all classes from scratch
	 *
	 * @return
	 */
	public MultiValueMap<OWLEntity, OWLEntity> extractModules();

	public MultiValueMap<OWLEntity, OWLEntity> getModules();

	/**
	 * Returns all the axioms loaded in the extractor.
	 *
	 * @return an unmodifiable set of axioms
	 */
	public Stream<OWLAxiom> axioms();

	@Deprecated
	public Set<OWLAxiom> getAxioms();

	/**
	 * Return the axioms which references this entity
	 *
	 * @param entity
	 * @return
	 */
	public Stream<OWLAxiom> axioms(final OWLEntity entity);

	@Deprecated
	public Set<OWLAxiom> getAxioms(final OWLEntity entity);

	/**
	 * Returns all the entities referenced in loaded axioms.
	 *
	 * @return an unmodifiable set of entities
	 */
	public Set<OWLEntity> getEntities();

	public OWLOntology getModule(final OWLEntity entity);

	/**
	 * Returns a new ontology that contains the axioms that are in the module for given set of entities
	 *
	 * @param signature
	 * @return
	 * @throws OWLException
	 */
	public OWLOntology getModuleFromSignature(final Set<OWLEntity> signature);

	/**
	 * Returns the _timers used by this extractor to collect statistics about performance.
	 *
	 * @return
	 */
	public Timers getTimers();

	/**
	 * Checks if axioms have been added/removed and modules need to be updated
	 *
	 * @return <code>true</code> if axioms have been added/removed
	 */
	public boolean isChanged();

	/**
	 * Checks if the changes that has not yet been updated require re-classification
	 *
	 * @return true if classification is needed, false otherwise
	 */
	public boolean isClassificationNeeded(final Expressivity expressivity);

	/**
	 * Update the modules with the changes that have been put into the _queue so far.
	 *
	 * @return The set of entities whose modules are affected by the changes
	 * @throws UnsupportedOperationException if modules cannot be updated as reported by {@link #canUpdate()} function
	 */
	public Set<OWLEntity> applyChanges(final Taxonomy<OWLClass> taxonomy) throws UnsupportedOperationException;

	/**
	 * Extract the module for a given set of entities.
	 *
	 * @param signature set of entities
	 * @return module for the given signature
	 */
	public Set<OWLAxiom> extractModule(final Set<? extends OWLEntity> signature);

	/**
	 * Save the _current state of the ModuleExtractor. The output is saved to a ZipOutputStream to allow storage in multiple files in one stream.
	 *
	 * @param outputStream the zip output stream where the _data should be stored
	 * @throws IOException if an I/O error occurs during the saving
	 * @throws IllegalStateException if there are outstanding changes that have not yet been applied to the modules (e.g., via updateModules())
	 */
	public void save(final ZipOutputStream outputStream) throws IOException, IllegalStateException;

	/**
	 * Restores the previously saved state of the ModuleExtractor from a stream. The input is read from a ZipInputStream because the _data may potentially span
	 * multiple files. The method assumes that the zip file entries saved by the save() method are the immediately next ones in the stream.
	 *
	 * @param inputStream the zip input stream from which the _data should be read
	 * @throws IOException if an I/O error occurs during the read
	 * @throws IllegalArgumentException if the next zip file entry in the stream was not saved by a compatible ModuleExtractor
	 */
	public void load(final ZipInputStream inputStream) throws IOException, IllegalArgumentException;
}
