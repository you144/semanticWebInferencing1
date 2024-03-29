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

package openllet.core.taxonomy.printer;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import openllet.aterm.ATermAppl;
import openllet.core.taxonomy.TaxonomyUtils;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.QNameProvider;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: Specialized tree printer for class hierarchies that prints instaces for each class
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class ClassTreePrinter extends TreeTaxonomyPrinter<ATermAppl>
{
	private final QNameProvider _qnames = new QNameProvider();

	public ClassTreePrinter()
	{
	}

	@Override
	protected void printNode(final Set<ATermAppl> set)
	{
		super.printNode(set);

		final Set<ATermAppl> instances = TaxonomyUtils.getDirectInstances(_taxonomyImpl, set.iterator().next());
		if (instances.size() > 0)
		{
			_out.print(" - (");
			boolean printed = false;
			int anonCount = 0;
			final Iterator<ATermAppl> ins = instances.iterator();
			while (ins.hasNext())
			{
				final ATermAppl x = ins.next();

				if (ATermUtils.isBnode(x))
					anonCount++;
				else
				{
					if (printed)
						_out.print(", ");
					else
						printed = true;
					printURI(_out, x);
				}
			}
			if (anonCount > 0)
			{
				if (printed)
					_out.print(", ");
				_out.print(anonCount + " Anonymous Individual");
				if (anonCount > 1)
					_out.print("s");
			}
			_out.print(")");
		}
	}

	@Override
	protected void printURI(final PrintWriter out, final ATermAppl c)
	{
		String str = null;

		if (c.equals(ATermUtils.TOP))
			str = "owl:Thing";
		else
			if (c.equals(ATermUtils.BOTTOM))
				str = "owl:Nothing";
			else
				if (ATermUtils.isPrimitive(c))
					str = _qnames.shortForm(c.getName());
				else
					str = c.toString();

		out.print(str);
	}
}
