// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.pellint.lintpattern.axiom;

import openllet.pellint.lintpattern.LintPattern;
import openllet.pellint.model.Lint;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * <p>
 * Title: Axiom-based Lint Pattern Abstract Class
 * </p>
 * <p>
 * Description: Provides convenience (protected) methods to create and set {@link com.clarkparsia.pellint.model.Lint}, and methods to traverse an OWLAxiom
 * (through OWLAxiomVisitorAdapter).
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Harris Lin
 */
public abstract class AxiomLintPattern implements LintPattern, OWLAxiomVisitor
{
	private Lint _Lint;
	private OWLOntology _Ontology;

	protected Lint makeLint()
	{
		return new Lint(this, _Ontology);
	}

	protected void setLint(final Lint lint)
	{
		_Lint = lint;
	}

	/**
	 * Match an OWLAxiom and returns a {@link com.clarkparsia.pellint.model.Lint} for the axiom if found. Do not override this method. To create and return a
	 * {@link com.clarkparsia.pellint.model.Lint}, implementers of this class should call {@link #makeLint()} first, set any necessary information on the
	 * {@link com.clarkparsia.pellint.model.Lint}, and lastly call {@link #setLint(Lint)}.
	 *
	 * @return {@link com.clarkparsia.pellint.model.Lint} for the OWLAxiom, otherwise <code>null</code>.
	 * @see com.clarkparsia.pellint.model.Lint
	 */
	public final Lint match(final OWLOntology ontology, final OWLAxiom axiom)
	{
		_Lint = null;
		_Ontology = ontology;
		axiom.accept(this);
		return _Lint;
	}
}
