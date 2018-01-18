package openllet.aterm.pure.owl;

import openllet.aterm.pure.PureFactory;

public class FunDisjointClasses extends AFunOwl
{
	public FunDisjointClasses(final PureFactory factory)
	{
		super(factory);
	}

	@Override
	public int getArity()
	{
		return 1;
	}
}
