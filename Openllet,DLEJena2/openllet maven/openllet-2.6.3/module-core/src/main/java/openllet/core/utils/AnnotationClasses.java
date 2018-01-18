// Copyright (c) 2006 - 2010, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils;

import static openllet.core.utils.TermFactory.term;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.core.OpenlletOptions;

/**
 * In some ontologies, such as the ones from OBO, annotations may be nested and contain type assertions on annotation values. Such type assertions will be
 * treated as regular assertions and processed by the reasoner. This causes many superfluous logical axioms to be considered by the reasoner which might have
 * significant impact on performance. The {@link OpenlletOptions#IGNORE_ANNOTATION_CLASSES} option tells Pellet to ignore such annotations. The set of classes
 * that will be treated as annotation classes are stored in this class. This set can be modified by adding new classes or removing existing classes.
 * <p>
 * Annotation classes from OBO are included by default. These are <code>obo:DbXref, obo:Definition, obo:Subset, obo:Synonym, obo:SynonymType</code> where
 * <code>obo</code> namespace refers to <code>http://www.geneontology.org/formats/oboInOwl</code>.
 * </p>
 *
 * @author Evren Sirin
 */
public class AnnotationClasses
{
	private static final Set<ATermAppl> INSTANCE = new HashSet<>();
	private static final String _geneontologyBaseIriObo = "http://www.geneontology.org/formats/oboInOwl#";

	static
	{
		INSTANCE.add(term(_geneontologyBaseIriObo + "DbXref"));
		INSTANCE.add(term(_geneontologyBaseIriObo + "Definition"));
		INSTANCE.add(term(_geneontologyBaseIriObo + "Subset"));
		INSTANCE.add(term(_geneontologyBaseIriObo + "Synonym"));
		INSTANCE.add(term(_geneontologyBaseIriObo + "SynonymType"));
	}

	/**
	 * Adds the specified class to the set of annotation classes.
	 *
	 * @param cls class to add
	 */
	public static void add(final ATermAppl cls)
	{
		INSTANCE.add(cls);
	}

	/**
	 * @return <code>true</code> if the specified class is defined to be an annotation class AND {@link OpenlletOptions#IGNORE_ANNOTATION_CLASSES} option is set
	 *         to true. If the configuration option is <code>false</code> this function will return <code>false</code> for every class.
	 * @param cls class to check
	 */
	public static boolean contains(final ATermAppl cls)
	{
		return OpenlletOptions.IGNORE_ANNOTATION_CLASSES && INSTANCE.contains(cls);
	}

	/**
	 * Removes the specified class form set of annotation classes.
	 *
	 * @param cls class to remove.
	 */
	public static void remove(final ATermAppl cls)
	{
		INSTANCE.remove(cls);
	}

	/**
	 * @return an unmodifiable copy of the annotation classes regardless of the {@link OpenlletOptions#IGNORE_ANNOTATION_CLASSES} option.
	 */
	public static Set<ATermAppl> getAll()
	{
		return Collections.unmodifiableSet(INSTANCE);
	}
}
