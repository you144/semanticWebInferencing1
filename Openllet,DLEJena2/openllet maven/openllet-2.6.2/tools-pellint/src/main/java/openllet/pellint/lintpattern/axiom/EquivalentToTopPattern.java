// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.pellint.lintpattern.axiom;

import java.util.Collections;
import java.util.HashSet;
import openllet.owlapi.OWL;
import openllet.pellint.format.CompactClassLintFormat;
import openllet.pellint.format.LintFormat;
import openllet.pellint.model.Lint;
import openllet.pellint.model.LintFixer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

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
 * @author Harris Lin
 */
public class EquivalentToTopPattern extends AxiomLintPattern
{
	private static final LintFormat DEFAULT_LINT_FORMAT = new CompactClassLintFormat();

	@Override
	public String getName()
	{
		return getClass().getSimpleName();
	}

	@Override
	public String getDescription()
	{
		return "Top is equivalent to some concept or is part of an equivalent classes axiom";
	}

	@Override
	public boolean isFixable()
	{
		return true;
	}

	@Override
	public LintFormat getDefaultLintFormat()
	{
		return DEFAULT_LINT_FORMAT;
	}

	@Override
	public void visit(final OWLEquivalentClassesAxiom axiom)
	{
		if (axiom.classExpressions().anyMatch(OWL.Thing::equals))
		{
			final Lint lint = makeLint();
			lint.addParticipatingAxiom(axiom);
			final LintFixer fixer = new LintFixer(Collections.singleton(axiom), new HashSet<OWLAxiom>());
			lint.setLintFixer(fixer);
			setLint(lint);
		}
	}
}
