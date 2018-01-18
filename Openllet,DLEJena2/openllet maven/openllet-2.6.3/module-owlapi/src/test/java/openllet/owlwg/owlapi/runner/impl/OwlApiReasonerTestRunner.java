package openllet.owlwg.owlapi.runner.impl;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * <p>
 * Title: OWLAPI Reasoner Test Runner
 * </p>
 * <p>
 * Description: Wrapper to use any reasoner implementing the OWLAPI OWLReasoner interface to run reasoning test cases.
 * </p>
 * <p>
 * Copyright: Copyright &copy; 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <a href="http://clarkparsia.com/"/>http://clarkparsia.com/</a>
 * </p>
 * 
 * @author Mike Smith &lt;msmith@clarkparsia.com&gt;
 */
public class OwlApiReasonerTestRunner extends OwlApiAbstractRunner
{

	private final OWLReasonerFactory _reasonerFactory;
	private final IRI _iri;

	public OwlApiReasonerTestRunner(final OWLReasonerFactory reasonerFactory, final IRI runnerUri)
	{
		_reasonerFactory = reasonerFactory;
		_iri = runnerUri;
	}

	@Override
	public String getName()
	{
		return _reasonerFactory.getReasonerName();
	}

	@Override
	public IRI getIRI()
	{
		return _iri;
	}

	@Override
	protected boolean isConsistent(final OWLOntology o)
	{
		final OWLReasoner reasoner = _reasonerFactory.createReasoner(o);
		return reasoner.isConsistent();
	}

	@Override
	protected boolean isEntailed(final OWLOntology premise, final OWLOntology conclusion)
	{
		final OWLReasoner reasoner = _reasonerFactory.createReasoner(premise);

		return reasoner.isEntailed(conclusion.logicalAxioms());
	}

}
