// Copyright (_c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.EdgeList;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.boxes.rbox.Role;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.rules.model.AtomDObject;
import openllet.core.rules.model.AtomIObject;
import openllet.core.rules.model.AtomObject;
import openllet.core.rules.model.AtomVariable;
import openllet.core.rules.model.BuiltInAtom;
import openllet.core.rules.model.ClassAtom;
import openllet.core.rules.model.DataRangeAtom;
import openllet.core.rules.model.DatavaluedPropertyAtom;
import openllet.core.rules.model.DifferentIndividualsAtom;
import openllet.core.rules.model.IndividualPropertyAtom;
import openllet.core.rules.model.Rule;
import openllet.core.rules.model.RuleAtom;
import openllet.core.rules.model.RuleAtomVisitor;
import openllet.core.rules.model.SameIndividualAtom;
import openllet.core.utils.ATermUtils;

/**
 * <p>
 * Title: Trivial Satisfaction helper
 * </p>
 * <p>
 * Copyright: Copyright (_c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Ron Alford
 */
public class TrivialSatisfactionHelpers
{
	private static final BindingTester ALWAYS_TRUE = new BindingTester()
	{
		@Override
		public DependencySet check(final VariableBinding binding)
		{
			return DependencySet.INDEPENDENT;
		}

		@Override
		public String toString()
		{
			return "test(TRUE)";
		}
	};

	private abstract class BinaryBindingTester<R extends AtomObject, S extends AtomObject> implements BindingTester
	{
		public R _arg1;
		public S _arg2;

		public BinaryBindingTester(final R arg1, final S arg2)
		{
			this._arg1 = arg1;
			this._arg2 = arg2;
		}

		public R getArg1()
		{
			return _arg1;
		}

		public S getArg2()
		{
			return _arg2;
		}
	}

	private interface BindingTester
	{
		/**
		 * @return true if binding supports testing _condition
		 * @param binding
		 */
		public DependencySet check(VariableBinding binding);
	}

	/**
	 * Creates a helper for each body atom it can. If none can be made, getHelper() returns null
	 */
	private class BodyAtomVisitor implements RuleAtomVisitor
	{

		private BindingTester _tester = null;

		public BindingTester getTester()
		{
			return _tester;
		}

		@Override
		public void visit(final BuiltInAtom atom)
		{
			_tester = null;
		}

		@Override
		public void visit(final ClassAtom atom)
		{
			_tester = new TestClass(ATermUtils.normalize(ATermUtils.negate(atom.getPredicate())), atom.getArgument());
		}

		@Override
		public void visit(final DataRangeAtom atom)
		{
			_tester = null;
		}

		@Override
		public void visit(final DatavaluedPropertyAtom atom)
		{
			_tester = null;
		}

		@Override
		public void visit(final DifferentIndividualsAtom atom)
		{
			_tester = new TestSame(atom.getArgument1(), atom.getArgument2());
		}

		@Override
		public void visit(final IndividualPropertyAtom atom)
		{
			_tester = null;
		}

		@Override
		public void visit(final SameIndividualAtom atom)
		{
			_tester = new TestDifferent(atom.getArgument1(), atom.getArgument2());
		}

	}

	/**
	 * Takes a filter and a list of _vars to implement Binding helper.
	 */
	private static class FilterHelper implements BindingHelper
	{

		private boolean _result = false;
		private final BindingTester _tester;
		private final Collection<? extends AtomVariable> _vars;

		public FilterHelper(final BindingTester tester, final Collection<? extends AtomVariable> vars)
		{
			_tester = tester;
			_vars = vars;
		}

		@Override
		public Collection<? extends AtomVariable> getBindableVars(final Collection<AtomVariable> bound)
		{
			return Collections.emptySet();
		}

		@Override
		public Collection<? extends AtomVariable> getPrerequisiteVars(final Collection<AtomVariable> bound)
		{
			return _vars;
		}

		@Override
		public void rebind(final VariableBinding newBinding)
		{
			_result = _tester.check(newBinding) == null;
		}

		@Override
		public boolean selectNextBinding()
		{
			final boolean result = _result;
			_result = false;
			return result;
		}

		@Override
		public void setCurrentBinding(final VariableBinding currentBinding)
		{
			// nothing to do.
		}

		@Override
		public String toString()
		{
			return "Filter(" + _tester + ")";
		}

	}

	/**
	 * Creates a helper for each head atom it can. If none can be made, getHelper() returns null
	 */
	private class HeadAtomVisitor implements RuleAtomVisitor
	{

		private BindingTester _tester = null;

		public BindingTester getTester()
		{
			return _tester;
		}

		@Override
		public void visit(final BuiltInAtom atom)
		{
			_tester = ALWAYS_TRUE;
		}

		@Override
		public void visit(final ClassAtom atom)
		{
			_tester = new TestClass(ATermUtils.normalize(atom.getPredicate()), atom.getArgument());
		}

		@Override
		public void visit(final DataRangeAtom atom)
		{
			_tester = ALWAYS_TRUE;
		}

		@Override
		public void visit(final DatavaluedPropertyAtom atom)
		{
			_tester = new TestDataProperty(atom.getPredicate(), atom.getArgument1(), atom.getArgument2());
		}

		@Override
		public void visit(final DifferentIndividualsAtom atom)
		{
			_tester = new TestDifferent(atom.getArgument1(), atom.getArgument2());
		}

		@Override
		public void visit(final IndividualPropertyAtom atom)
		{
			_tester = new TestIndividualProperty(atom.getPredicate(), atom.getArgument1(), atom.getArgument2());
		}

		@Override
		public void visit(final SameIndividualAtom atom)
		{
			_tester = new TestSame(atom.getArgument1(), atom.getArgument2());
		}

	}

	/**
	 * Returns the dependency set if the given class assertion holds, <code>null</code> otherwise
	 */
	private class TestClass extends UnaryBindingTester<AtomIObject>
	{
		private final ATermAppl _c;

		public TestClass(final ATermAppl c, final AtomIObject arg)
		{
			super(arg);
			_c = c;
		}

		@Override
		public DependencySet check(final VariableBinding binding)
		{
			final Individual ind = binding.get(getArg());
			return ind.getDepends(_c);
		}

		@Override
		public String toString()
		{
			return "notClass(" + getArg() + ":" + _c + ")";
		}

	}

	/**
	 * Returns the dependency set if the given property assertion holds, <code>null</code> otherwise
	 */
	private class TestDataProperty extends TestProperty<AtomDObject>
	{

		public TestDataProperty(final ATermAppl p, final AtomIObject arg1, final AtomDObject arg2)
		{
			super(p, arg1, arg2);
		}

		@Override
		public DependencySet check(final VariableBinding binding)
		{
			return check(binding.get(getArg1()), binding.get(getArg2()));
		}

	}

	/**
	 * Returns the dependency set if the given individuals are different, <code>null</code> otherwise
	 */
	private class TestDifferent extends BinaryBindingTester<AtomIObject, AtomIObject>
	{

		public TestDifferent(final AtomIObject arg1, final AtomIObject arg2)
		{
			super(arg1, arg2);
		}

		@Override
		public DependencySet check(final VariableBinding binding)
		{
			return binding.get(_arg1).getDifferenceDependency(binding.get(_arg2));
		}

		@Override
		public String toString()
		{
			return "notDifferent(" + getArg1() + "," + getArg2() + ")";
		}

	}

	/**
	 * Returns the dependency set if the given property assertion holds, <code>null</code> otherwise
	 */
	private class TestIndividualProperty extends TestProperty<AtomIObject>
	{

		public TestIndividualProperty(final ATermAppl p, final AtomIObject arg1, final AtomIObject arg2)
		{
			super(p, arg1, arg2);
		}

		@Override
		public DependencySet check(final VariableBinding binding)
		{
			return check(binding.get(getArg1()), binding.get(getArg2()));
		}

	}

	/**
	 * Returns the dependency set if the given property assertion holds, <code>null</code> otherwise
	 */
	private abstract class TestProperty<S extends AtomObject> extends BinaryBindingTester<AtomIObject, S>
	{
		public Role _role;

		public TestProperty(final ATermAppl p, final AtomIObject arg1, final S arg2)
		{
			super(arg1, arg2);
			_role = _abox.getRole(p);
			if (_role == null)
				throw new InternalReasonerException("Cannot retreive role!: " + p);
		}

		public DependencySet check(final Individual node1, final Node node2)
		{
			final EdgeList list = node1.getRNeighborEdges(_role);
			for (int i = 0, n = list.size(); i < n; i++)
			{
				final Edge edge = list.get(i);
				if (edge.getNeighbor(node1).equals(node2))
					return edge.getDepends();
			}

			return null;
		}

	}

	/**
	 * Returns the dependency set if the given individuals ar same, <code>null</code> otherwise
	 */
	private class TestSame extends BinaryBindingTester<AtomIObject, AtomIObject>
	{

		public TestSame(final AtomIObject arg1, final AtomIObject arg2)
		{
			super(arg1, arg2);
		}

		@Override
		public DependencySet check(final VariableBinding binding)
		{
			final Individual ind1 = binding.get(_arg1);
			final Individual ind2 = binding.get(_arg2);
			if (ind1.isSame(ind2))
			{
				// we might be returning a super set of the actual dependency
				// set for the sameness since it is not straight-forward to come
				// up with the exact dependency set if there are other
				// individuals involved in this sameAs inference
				final DependencySet ds1 = ind1.getMergeDependency(true);
				final DependencySet ds2 = ind2.getMergeDependency(true);

				return ds1 == null ? ds2 : ds2 == null ? ds1 : ds1.union(ds2, true);
			}
			else
				return null;
		}

	}

	private abstract class UnaryBindingTester<R extends AtomObject> implements BindingTester
	{
		public R _arg;

		public UnaryBindingTester(final R arg)
		{
			this._arg = arg;
		}

		public R getArg()
		{
			return _arg;
		}
	}

	private final ABox _abox;

	public TrivialSatisfactionHelpers(final ABox abox)
	{
		_abox = abox;
	}

	public Collection<BindingHelper> getHelpers(final Rule rule)
	{
		final Collection<BindingHelper> helpers = new ArrayList<>();

		final BodyAtomVisitor bodyVisitor = new BodyAtomVisitor();
		for (final RuleAtom atom : rule.getBody())
		{
			atom.accept(bodyVisitor);
			if (bodyVisitor.getTester() != null)
				helpers.add(new FilterHelper(bodyVisitor.getTester(), VariableUtils.getVars(atom)));
		}

		return helpers;
	}

	public DependencySet isAtomTrue(final RuleAtom atom, final VariableBinding binding)
	{
		final HeadAtomVisitor visitor = new HeadAtomVisitor();
		atom.accept(visitor);
		final BindingTester tester = visitor.getTester();
		return tester == null ? null : tester.check(binding);
	}
}
