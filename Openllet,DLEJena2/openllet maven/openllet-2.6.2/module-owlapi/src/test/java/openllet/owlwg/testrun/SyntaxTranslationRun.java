package openllet.owlwg.testrun;

import openllet.owlwg.runner.TestRunner;
import openllet.owlwg.testcase.TestCase;

/**
 * <p>
 * Title: Syntax Translation Run
 * </p>
 * <p>
 * Description:
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
public class SyntaxTranslationRun extends AbstractRun
{

	public SyntaxTranslationRun(TestCase<?> testcase, RunResultType type, TestRunner<?> runner)
	{
		this(testcase, type, runner, null, null);
	}

	public SyntaxTranslationRun(TestCase<?> testcase, RunResultType type, TestRunner<?> runner, @SuppressWarnings("unused") String details)
	{
		this(testcase, type, runner, null, null);
	}

	public SyntaxTranslationRun(TestCase<?> testcase, RunResultType type, TestRunner<?> runner, String details, Throwable cause)
	{
		super(testcase, type, RunTestType.SYNTAX_TRANSLATION, runner, details, cause);
	}

	@Override
	public void accept(TestRunResultVisitor visitor)
	{
		visitor.visit(this);
	}
}
