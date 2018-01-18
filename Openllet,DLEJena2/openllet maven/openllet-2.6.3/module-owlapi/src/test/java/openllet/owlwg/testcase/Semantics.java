package openllet.owlwg.testcase;

import openllet.owlwg.testcase.TestVocabulary.Individual;
import org.semanticweb.owlapi.model.OWLIndividual;

/**
 * <p>
 * Title: Semantics
 * </p>
 * <p>
 * Description: See <a href="http://www.w3.org/TR/owl2-test/#Applicable_Semantics">OWL 2 Conformance: Applicable Semantics</a>.
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
public enum Semantics
{

	/**
	 * OWL 2 Direct Semantics
	 */
	DIRECT(Individual.DIRECT),
	/**
	 * OWL 2 RDF Based Semantics
	 */
	RDF(Individual.RDF_BASED);

	public static Semantics get(final OWLIndividual i)
	{
		for (final Semantics s : values())
			if (s.getOWLIndividual().equals(i))
				return s;
		return null;
	}

	private final TestVocabulary.Individual _i;

	private Semantics(final TestVocabulary.Individual i)
	{
		_i = i;
	}

	public OWLIndividual getOWLIndividual()
	{
		return _i.getOWLIndividual();
	}
}
