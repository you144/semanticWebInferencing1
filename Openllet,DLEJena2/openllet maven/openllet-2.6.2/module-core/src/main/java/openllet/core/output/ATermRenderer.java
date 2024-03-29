//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

package openllet.core.output;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * A special kind of visitor that is designed to render the terms to output. The output is sent to an OutputFormatter so that outputing HTML format is
 * supported.
 *
 * @author Evren Sirin
 */
public interface ATermRenderer extends ATermVisitor
{
	public void setWriter(PrintWriter out);

	public PrintWriter getWriter();

	public void setWriter(Writer out);
}
