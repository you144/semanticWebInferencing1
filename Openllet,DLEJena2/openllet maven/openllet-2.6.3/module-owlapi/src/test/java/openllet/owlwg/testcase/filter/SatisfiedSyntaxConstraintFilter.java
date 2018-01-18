package openllet.owlwg.testcase.filter;

import openllet.owlwg.testcase.SyntaxConstraint;
import openllet.owlwg.testcase.TestCase;

/**
 * <p>
 * Title: Satisfied Syntax Constraint Filter Condition
 * </p>
 * <p>
 * Description: Filter _condition to match tests for which a particular syntax _constraint is satisfied.
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
public class SatisfiedSyntaxConstraintFilter implements FilterCondition
{

	public static final SatisfiedSyntaxConstraintFilter DL, EL, QL, RL;

	static
	{
		DL = new SatisfiedSyntaxConstraintFilter(SyntaxConstraint.DL);
		EL = new SatisfiedSyntaxConstraintFilter(SyntaxConstraint.EL);
		QL = new SatisfiedSyntaxConstraintFilter(SyntaxConstraint.QL);
		RL = new SatisfiedSyntaxConstraintFilter(SyntaxConstraint.RL);
	}

	final private SyntaxConstraint _constraint;

	/**
	 * @throws NullPointerException if <code>_constraint == null</code>
	 */
	public SatisfiedSyntaxConstraintFilter(SyntaxConstraint constraint)
	{
		if (constraint == null)
			throw new NullPointerException();

		this._constraint = constraint;
	}

	@Override
	public boolean accepts(TestCase<?> testcase)
	{
		return testcase.getSatisfiedConstraints().contains(_constraint);
	}

	@Override
	public String toString()
	{
		return _constraint.toString();
	}

}
