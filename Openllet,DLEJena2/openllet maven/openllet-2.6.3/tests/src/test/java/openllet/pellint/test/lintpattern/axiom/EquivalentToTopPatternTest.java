package openllet.pellint.test.lintpattern.axiom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import openllet.owlapi.OWL;
import openllet.pellint.lintpattern.axiom.EquivalentToTopPattern;
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
public class EquivalentToTopPatternTest extends PellintTestCase
{

	private EquivalentToTopPattern _pattern;

	@Override
	@Before
	public void setUp() throws OWLOntologyCreationException
	{
		super.setUp();
		_pattern = new EquivalentToTopPattern();
	}

	@Test
	public void testNone()
	{
		assertTrue(_pattern.isFixable());

		OWLAxiom axiom = OWL.equivalentClasses(CollectionUtil.<OWLClassExpression> asSet(OWL.Nothing, _cls[2], _cls[3]));
		assertNull(_pattern.match(_ontology, axiom));

		axiom = OWL.subClassOf(OWL.Thing, _cls[1]);
		assertNull(_pattern.match(_ontology, axiom));
	}

	@Test
	public void testSimple()
	{
		final OWLAxiom axiom = OWL.equivalentClasses(CollectionUtil.<OWLClassExpression> asSet(OWL.Thing, _cls[0], _cls[1]));
		final Lint lint = _pattern.match(_ontology, axiom);
		assertNotNull(lint);

		final LintFixer fixer = lint.getLintFixer();
		assertTrue(fixer.getAxiomsToRemove().contains(axiom));
		assertTrue(fixer.getAxiomsToAdd().isEmpty());

		assertNull(lint.getSeverity());
		assertSame(_ontology, lint.getParticipatingOntology());
	}

}
