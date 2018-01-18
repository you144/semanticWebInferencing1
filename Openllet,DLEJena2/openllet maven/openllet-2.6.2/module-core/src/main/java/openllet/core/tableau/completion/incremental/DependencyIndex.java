// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.tableau.completion.incremental;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATermAppl;
import openllet.core.DependencySet;
import openllet.core.KnowledgeBase;
import openllet.core.boxes.abox.Clash;
import openllet.core.boxes.abox.Edge;
import openllet.core.tableau.branch.Branch;
import openllet.core.tableau.branch.DisjunctionBranch;
import openllet.core.utils.SetUtils;
import openllet.shared.tools.Log;

/**
 * This is the _index structure for maintaining the _dependencies between structures in an ABox and the syntactic assertions which caused them to be created.
 * This is used for incremental deletions.
 *
 * @author Christian Halaschek-Wiener
 */
public class DependencyIndex
{
	public final static Logger _logger = Log.getLogger(DependencyIndex.class);

	/**
	 * Map from assertions (ATermAppl) to Dependency entries
	 */
	private final Map<ATermAppl, DependencyEntry> _dependencies = new ConcurrentHashMap<>();

	/**
	 * Branch dependency index
	 */
	private final Map<Branch, Set<BranchDependency>> _branchIndex = new ConcurrentHashMap<>();

	/**
	 * Clash dependency - used for cleanup
	 */
	private final Set<ClashDependency> _clashIndex = SetUtils.create();

	/**
	 * KB object
	 */
	private final KnowledgeBase _kb;

	/**
	 * Default constructor
	 *
	 * @param kb
	 */
	public DependencyIndex(final KnowledgeBase kb)
	{
		_kb = kb;
	}

	/**
	 * Copy constructor
	 *
	 * @param kb
	 * @param oldIndex
	 */
	public DependencyIndex(final KnowledgeBase kb, final DependencyIndex oldIndex)
	{
		this(kb);
		for (final ATermAppl next : oldIndex.getDependencies().keySet()) //iterate over old _dependencies and copy
			_dependencies.put(next, new DependencyEntry(oldIndex.getDependencies(next)));
	}

	/**
	 * @param assertion
	 * @return the dependencies
	 */
	public DependencyEntry getDependencies(final ATermAppl assertion)
	{
		return _dependencies.get(assertion);
	}

	/**
	 * @return
	 */
	protected Map<ATermAppl, DependencyEntry> getDependencies()
	{
		return _dependencies;
	}

	/**
	 * Add a new type dependency
	 *
	 * @param ind
	 * @param type
	 * @param ds
	 */
	public void addTypeDependency(final ATermAppl ind, final ATermAppl type, final DependencySet ds)
	{
		for (final ATermAppl nextAtom : ds.getExplain()) //loop over ds
			if (_kb.getSyntacticAssertions().contains(nextAtom)) //check if this assertion exists
			{
				if (!_dependencies.containsKey(nextAtom)) //if this entry does not exist then create it
					_dependencies.put(nextAtom, new DependencyEntry());

				_dependencies.get(nextAtom).addTypeDependency(ind, type); //add the dependency
			}
	}

	/**
	 * Add a new merge dependency
	 *
	 * @param ind
	 * @param mergedTo
	 * @param ds
	 */
	public void addMergeDependency(final ATermAppl ind, final ATermAppl mergedTo, final DependencySet ds)
	{
		for (final ATermAppl nextAtom : ds.getExplain()) //loop over ds
			if (_kb.getSyntacticAssertions().contains(nextAtom)) //check if this assertion exists
			{
				if (!_dependencies.containsKey(nextAtom)) //if this entry does not exist then create it
					_dependencies.put(nextAtom, new DependencyEntry());

				_dependencies.get(nextAtom).addMergeDependency(ind, mergedTo); //add the dependency
			}
	}

	/**
	 * Add a new edge dependency
	 *
	 * @param edge
	 * @param ds
	 */
	public void addEdgeDependency(final Edge edge, final DependencySet ds)
	{
		for (final ATermAppl nextAtom : ds.getExplain()) //loop over ds
			if (_kb.getSyntacticAssertions().contains(nextAtom)) //check if this assertion exists
			{
				if (!_dependencies.containsKey(nextAtom)) //if this entry does not exist then create it
					_dependencies.put(nextAtom, new DependencyEntry());

				_dependencies.get(nextAtom).addEdgeDependency(edge); //add the dependency
			}
	}

	/**
	 * Add a new branch dependency
	 *
	 * @param branch
	 */
	public void addBranchAddDependency(final Branch branch)
	{
		for (final ATermAppl nextAtom : branch.getTermDepends().getExplain()) //loop over ds
			if (_kb.getSyntacticAssertions().contains(nextAtom)) //check if this assertion exists
			{
				if (!_dependencies.containsKey(nextAtom)) //if this entry does not exist then create it
					_dependencies.put(nextAtom, new DependencyEntry());

				_logger.fine(() -> "DependencyIndex- Adding _branch add dependency for assertion: " + nextAtom + " -  Branch id [" + branch.getBranchIndexInABox() + "]   ,  Branch [" + branch + "]");

				//add the dependency
				final BranchDependency newDep = _dependencies.get(nextAtom).addBranchAddDependency(nextAtom, branch);

				//add dependency to index so that backjumping can be supported (ie, we need a fast way to remove the branch dependencies
				if (!_branchIndex.containsKey(branch))
				{
					final Set<BranchDependency> newS = new HashSet<>();
					newS.add(newDep);
					_branchIndex.put(branch, newS);
				}
				else
					_branchIndex.get(branch).add(newDep);
			}
	}

	/**
	 * Add a new _branch ds removal dependency
	 *
	 * @param branch
	 * @param ds
	 */
	public void addCloseBranchDependency(final Branch branch, final DependencySet ds)
	{
		for (final ATermAppl nextAtom : ds.getExplain()) //loop over ds
			if (_kb.getSyntacticAssertions().contains(nextAtom)) //check if this assertion exists
			{
				if (!_dependencies.containsKey(nextAtom)) //if this entry does not exist then create it
					_dependencies.put(nextAtom, new DependencyEntry());

				ATermAppl label = null;
				if (branch instanceof DisjunctionBranch)
					label = ((DisjunctionBranch) branch).getDisjunct(branch.getTryNext());

				if (_logger.isLoggable(Level.FINE))
					_logger.fine("DependencyIndex- Adding _branch remove ds dependency for assertion: " + nextAtom + " -  Branch id [" + branch.getBranchIndexInABox() + "]   ,  Branch [" + branch + "]   on label [" + label + "]  ,    _tryNext [" + branch.getTryNext() + "]");

				final BranchDependency newDep = _dependencies.get(nextAtom).addCloseBranchDependency(nextAtom, branch); //add the dependency

				//add depedency to _index so that backjumping can be supported (ie, we need a fast way to remove the _branch _dependencies
				if (!_branchIndex.containsKey(branch))
				{
					final Set<BranchDependency> newS = new HashSet<>();
					newS.add(newDep);
					_branchIndex.put(branch, newS);
				}
				else
					_branchIndex.get(branch).add(newDep);
			}
	}

	/**
	 * Remove the _dependencies for a given assertion
	 *
	 * @param assertion
	 */
	public void removeDependencies(final ATermAppl assertion)
	{
		_dependencies.remove(assertion);
	}

	/**
	 * Remove _branch _dependencies - this is needed due to backjumping!
	 *
	 * @param b
	 */
	public void removeBranchDependencies(final Branch b)
	{
		final Set<BranchDependency> deps = _branchIndex.get(b);

		//TODO: why is this null? is this because of duplicate entries in the _index set?
		//This seems to creep up in WebOntTest-I5.8-Manifest004 and 5 among others...
		if (deps == null)
			return;

		for (final BranchDependency next : deps) //loop over depencies and remove them
		{
			if (_logger.isLoggable(Level.FINE))
				_logger.fine("DependencyIndex: RESTORE causing remove of _branch _index for assertion: " + next.getAssertion() + " _branch dep.: " + next);
			if (next instanceof AddBranchDependency)
				//remove the dependency
				_dependencies.get(next.getAssertion()).getBranchAdds().remove(next);
			else
			{
				//remove the dependency
				//((DependencyEntry)_dependencies.get(next.getAssertion())).getBranchRemoveDSs().remove(next);
			}

		}
	}

	/**
	 * Set clash dependencies
	 *
	 * @param clash
	 */
	public void setClashDependencies(final Clash clash)
	{
		//first remove old entry using clashindex
		for (final ClashDependency next : _clashIndex)
			//remove the dependency
			if (_dependencies.containsKey(next.getAssertion()))
				_dependencies.get(next.getAssertion()).setClash(null);

		_clashIndex.clear(); //clear the old _index

		if (clash == null)
			return;

		for (final ATermAppl nextAtom : clash.getDepends().getExplain()) //loop over ds
			if (_kb.getSyntacticAssertions().contains(nextAtom)) //check if this assertion exists
			{
				if (!_dependencies.containsKey(nextAtom)) //if this entry does not exist then create it
					_dependencies.put(nextAtom, new DependencyEntry());

				if (_logger.isLoggable(Level.FINE))
					_logger.fine("  DependencyIndex- Adding clash dependency: Axiom [" + nextAtom + "]   ,  Clash [" + clash + "]");

				final ClashDependency newDep = new ClashDependency(nextAtom, clash);

				_dependencies.get(nextAtom).setClash(newDep); //set the dependency

				_clashIndex.add(newDep); //update _index
			}
	}
}
