// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com
//
// ---
// Portions Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
// Alford, Grove, Parsia, Sirin parts of this source code are available under the terms of the MIT License.
//
// The MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package openllet.core.tableau.blocking;

import java.util.Optional;
import java.util.logging.Logger;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.abox.Edge;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.abox.Node;
import openllet.core.utils.Timer;
import openllet.shared.tools.Log;

/**
 * <p>
 * Generic class to check if an _individual in an completion graph is _blocked by another _individual. Blocking prevents infinite models to be created and can
 * improve performance by limiting the size of the completion graph built.
 * </p>
 * <p>
 * This abstract class defines the basic functionality needed to check for blocking and leaves the actual check of blocking _condition between a pair of
 * individuals to its concrete subclasses that may do different things based on the expressivity of the _current kb.
 * </p>
 *
 * @author Evren Sirin
 */
public abstract class Blocking
{
	public final static Logger _logger = Log.getLogger(Blocking.class);

	protected static final BlockingCondition blockSet = new Block1Set();
	protected static final BlockingCondition blockAll = new Block2All();
	protected static final BlockingCondition block3Max = new Block3Max();
	protected static final BlockingCondition blockMin = new Block4Min();
	protected static final BlockingCondition blockMax = new Block5Max();
	protected static final BlockingCondition blockMinSome = new Block6MinSome();

	protected Blocking()
	{
		//
	}

	public boolean isDynamic()
	{
		return true;
	}

	public boolean isBlocked(final Individual blocked)
	{
		final Optional<Timer> timer = blocked.getABox().getKB().getTimers().startTimer("blocking");
		try
		{
			return !blocked.isRoot() && (isIndirectlyBlocked(blocked) || isDirectlyBlockedInt(blocked));
		}
		finally
		{
			timer.ifPresent(t -> t.stop());
		}
	}

	public boolean isIndirectlyBlocked(final Individual blocked)
	{
		final Individual parent = blocked.getParent();
		if (parent == null)
			return false;
		blocked.setBlocked(isBlocked(parent));
		return blocked.isBlocked();
	}

	public boolean isDirectlyBlocked(final Individual blocked)
	{
		final Optional<Timer> timer = blocked.getABox().getKB().getTimers().startTimer("dBlocking");
		try
		{
			return isDirectlyBlockedInt(blocked);
		}
		finally
		{
			timer.ifPresent(t -> t.stop());
		}
	}

	protected boolean isDirectlyBlockedInt(final Individual blocked)
	{
		final Individual parentBlocked = blocked.getParent();
		if (blocked.isRoot() || parentBlocked.isRoot())
			return false;

		final BlockingContext cxt = new BlockingContext(blocked);
		while (cxt.moveBlockerUp())
			if (isDirectlyBlockedBy(cxt))
			{
				blocked.setBlocked(true);
				_logger.finer(() -> blocked + " blocked by " + cxt._blocker);
				return true;
			}

		if (OpenlletOptions.USE_ANYWHERE_BLOCKING)
		{
			assert cxt._blocker.isRoot();

			return isDirectlyBlockedByDescendant(cxt);
		}

		return false;
	}

	protected boolean isDirectlyBlockedByDescendant(final BlockingContext cxt)
	{
		if (cxt._blocked.getParent().equals(cxt._blocker))
			return false;

		if (!cxt._blocker.isRoot() && isDirectlyBlockedBy(cxt))
		{
			cxt._blocked.setBlocked(true);
			_logger.finer(() -> cxt._blocked + " _blocked by " + cxt._blocker);
			return true;
		}

		final Individual blocker = cxt._blocker;
		for (final Edge e : blocker.getOutEdges())
		{
			final Node child = e.getTo();

			if (cxt.moveBlockerDown(child))
			{
				if (isDirectlyBlockedByDescendant(cxt))
					return true;
				cxt.moveBlockerUp();
			}
		}

		return false;
	}

	protected abstract boolean isDirectlyBlockedBy(BlockingContext cxt);
}
