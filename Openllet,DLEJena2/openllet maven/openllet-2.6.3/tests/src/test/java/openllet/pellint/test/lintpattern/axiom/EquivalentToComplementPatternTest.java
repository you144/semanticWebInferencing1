package openllet.pellint.test.lintpattern.axiom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import openllet.owlapi.OWL;
import openllet.pellint.lintpattern.axiom.EquivalentToComplementPattern;
import openllet.pellint.model.Lint;
import openllet.pellint.model.LintFixer;
import openllet.pellint.test.PellintTestCase;
import openllet.pellint.util.CollectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Harris Lin
 */
public class EquivalentToComplementPatternTest extends PellintTestCase
{

	private EquivalentToComplementPattern _pattern;

	@Override
	@Before
	public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		_pattern = new EquivalentToComplementPattern();
	}

	@Test
	public void testNone()
	{
		assertTrue(_pattern.isFixable());

		final OWLClassExpression comp = OWL.not(_cls[0]);
		OWLAxiom axiom = OWL.subClassOf(_cls[0], comp);
		assertNull(_pattern.match(_ontology, axiom));

		axiom = OWL.equivalentClasses(_P0AllC0, comp);
		assertNull(_pattern.match(_ontology, axiom));

		axiom = OWL.equivalentClasses(CollectionUtil.asSet(_cls[0], _cls[1], comp));
		assertNull(_pattern.match(_ontology, axiom));

		axiom = OWL.equivalentClasses(OWL.Nothing, OWL.Thing);
		assertNull(_pattern.match(_ontology, axiom));
	}

	@Test
	public void testComplementOfItself()
	{
		final OWLClassExpression comp = OWL.not(_cls[0]);
		final OWLAxiom axiom = OWL.equivalentClasses(_cls[0], comp);

		final Lint lint = _pattern.match(_ontology, axiom);
		assertNotNull(lint);
		assertTrue(lint.getParticipatingClasses().contains(_cls[0]));

		final LintFixer fixer = lint.getLintFixer();
		assertTrue(fixer.getAxiomsToRemove().contains(axiom));
		final OWLAxiom expectedAxiom = OWL.subClassOf(_cls[0], comp);
		assertTrue(fixer.getAxiomsToAdd().contains(expectedAxiom));

		assertNull(lint.getSeverity());
		assertSame(_ontology, lint.getParticipatingOntology());
	}

	@Test
	public void testComplementOfOthers()
	{
		final OWLClassExpression comp = OWL.not(OWL.or(_cls[1], _cls[2]));
		final OWLAxiom axiom = OWL.equivalentClasses(_cls[0], comp);
		final Lint lint = _pattern.match(_ontology, axiom);
		assertNotNull(lint);
		assertTrue(lint.getParticipatingClasses().contains(_cls[0]));

		final LintFixer fixer = lint.getLintFixer();
		assertTrue(fixer.getAxiomsToRemove().contains(axiom));
		final OWLAxiom expectedAxiom = OWL.subClassOf(_cls[0], comp);
		assertTrue(fixer.getAxiomsToAdd().contains(expectedAxiom));
	}
}
