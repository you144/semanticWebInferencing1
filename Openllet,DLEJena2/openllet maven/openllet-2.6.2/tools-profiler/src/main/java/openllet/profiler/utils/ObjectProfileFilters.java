package openllet.profiler.utils;

// ----------------------------------------------------------------------------
/**
 * A Factory for a few stock _node filters. See the implementation for details.
 *
 * @author (C) <a href="http://www.javaworld.com/columns/jw-qna-_index.shtml">Vlad Roubtsov</a>, 2003
 */
public abstract class ObjectProfileFilters
{
	// public: ................................................................

	/**
	 * Factory method for creating a visitor that only accepts profile _nodes with sizes larger than a given threshold value.
	 * 
	 * @param threshold _node size in bytes
	 */
	public static ObjectProfileNode.INodeFilter newSizeFilter(final int threshold)
	{
		return new SizeFilter(threshold);
	}

	/**
	 * Factory method for creating a visitor that accepts a profile _node only if it is at least the k-th largest child of its parent for a given value of k.
	 * E.g., newRankFilter(1) will prune the profile tree so that only the largest child is visited for every _node.
	 * 
	 * @param rank acceptable size rank [must be >= 0]
	 */
	public static ObjectProfileNode.INodeFilter newRankFilter(final int rank)
	{
		return new RankFilter(rank);
	}

	/**
	 * Factory method for creating a visitor that accepts a profile _node only if its size is larger than a given threshold relative to the size of the root _node
	 * (i.e., size of the entire profile tree).
	 * 
	 * @param threshold size fraction threshold
	 */
	public static ObjectProfileNode.INodeFilter newSizeFractionFilter(final double threshold)
	{
		return new SizeFractionFilter(threshold);
	}

	/**
	 * Factory method for creating a visitor that accepts a profile _node only if its size is larger than a given threshold relative to the size of its parent
	 * _node. This is useful for pruning the profile tree to show the largest contributors at every tree level.
	 * 
	 * @param threshold size fraction threshold
	 */
	public static ObjectProfileNode.INodeFilter newParentSizeFractionFilter(final double threshold)
	{
		return new ParentSizeFractionFilter(threshold);
	}

	// protected: .............................................................

	// package: ...............................................................

	// private: ...............................................................

	private ObjectProfileFilters()
	{
	} // this class is not extendible

	private static final class SizeFilter implements IObjectProfileNode.INodeFilter
	{
		@Override
		public boolean accept(final IObjectProfileNode node)
		{
			return node.size() >= _threshold;
		}

		SizeFilter(final int threshold)
		{
			_threshold = threshold;
		}

		private final int _threshold;

	} // _end of nested class

	private static final class RankFilter implements IObjectProfileNode.INodeFilter
	{
		@Override
		public boolean accept(final IObjectProfileNode node)
		{
			final IObjectProfileNode parent = node.parent();
			if (parent == null)
				return true;

			final IObjectProfileNode[] siblings = parent.children();
			for (int r = 0, rLimit = Math.min(siblings.length, _threshold); r < rLimit; ++r)
				if (siblings[r] == node)
					return true;

			return false;
		}

		RankFilter(final int threshold)
		{
			_threshold = threshold;
		}

		private final int _threshold;

	} // _end of nested class

	private static final class SizeFractionFilter implements IObjectProfileNode.INodeFilter
	{
		@Override
		public boolean accept(final IObjectProfileNode node)
		{
			return (node.size() >= _threshold * node.root().size());
		}

		SizeFractionFilter(final double threshold)
		{
			_threshold = threshold;
		}

		private final double _threshold;

	} // _end of nested class

	private static final class ParentSizeFractionFilter implements IObjectProfileNode.INodeFilter
	{
		@Override
		public boolean accept(final IObjectProfileNode node)
		{
			final IObjectProfileNode parent = node.parent();
			if (parent == null)
				return true; // always accept root node
			else
				return (node.size() >= _threshold * parent.size());
		}

		ParentSizeFractionFilter(final double threshold)
		{
			_threshold = threshold;
		}

		private final double _threshold;

	} // _end of nested class

} // _end of class
// ----------------------------------------------------------------------------