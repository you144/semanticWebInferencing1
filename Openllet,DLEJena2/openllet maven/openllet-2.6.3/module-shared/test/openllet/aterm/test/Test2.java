/*
 * Copyright (c) 2002-2007, CWI and INRIA
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, Berkeley nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package openllet.aterm.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import openllet.aterm.AFun;
import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermFactory;
import openllet.aterm.ATermInt;
import openllet.aterm.ATermList;
import openllet.aterm.ATermReal;
import openllet.aterm.pure.PureFactory;
import openllet.atom.OpenError;

public class Test2
{
	private final ATermFactory _factory;
	private final String _srcdir;

	public final static void main(final String[] args) throws IOException
	{
		final Test2 pureSuite = new Test2(new PureFactory(), args[0]);
		pureSuite.testAll();
		//Test2 nativeSuite = new Test2(new NativeFactory());
		//nativeSuite.testAll();
	}

	public Test2(final ATermFactory factory, final String srcdir)
	{
		this._factory = factory;
		this._srcdir = srcdir;
	}

	public void test_assert(final boolean condition)
	{
		if (!condition)
			throw new OpenError("assertion failed.");
	}

	public void testMakeInt()
	{
		final ATermInt[] term = new ATermInt[2];

		term[0] = _factory.makeInt(3);
		term[1] = _factory.makeInt(3);

		test_assert(term[0].getType() == ATerm.INT);
		test_assert(term[0].getInt() == 3);
		test_assert(term[0] == term[1]);

		System.out.println("toString: " + term[0]);
		test_assert(term[0].toString().equals("3"));

		List<Object> result = term[0].match("3");
		test_assert(result != null && result.size() == 0);

		result = term[0].match("<int>");
		test_assert(result != null && result.size() == 1);

		//		System.out.println("pass: testMakeInt");
	}

	public void testMakeReal()
	{
		final ATermReal[] term = new ATermReal[2];

		term[0] = _factory.makeReal(Math.PI);
		term[1] = _factory.makeReal(Math.PI);

		test_assert(term[0].getType() == ATerm.REAL);
		test_assert(term[0].getReal() == Math.PI);
		test_assert(term[0] == term[1]);

		final List<Object> result = term[0].match("<real>");
		test_assert(result != null && result.size() == 1 && result.get(0).equals(new Double(Math.PI)));

		//		System.out.println("pass: testMakeReal");
	}

	public void testMakeAppl()
	{
		final AFun[] symmies = new AFun[2];
		final ATermAppl[] apples = new ATermAppl[16];

		symmies[0] = _factory.makeAFun("f0", 0, false);
		symmies[1] = _factory.makeAFun("f1", 1, false);
		//		symmies[2] = factory.makeAFun("f6", 6, false);
		//		symmies[3] = factory.makeAFun("f10", 10, false);

		apples[0] = _factory.makeAppl(symmies[0]);
		test_assert(_factory.makeAppl(symmies[0]) == apples[0]);

		apples[1] = _factory.makeAppl(symmies[1], apples[0]);
		test_assert(_factory.makeAppl(symmies[1], apples[0]) == apples[1]);

		//		apples[2] = factory.makeAppl(symmies[1], apples[1]);
		//		apples[3] = factory.makeAppl(symmies[1], apples[0]);
		//		apples[4] = factory.makeAppl(symmies[2], apples[0], apples[0], apples[1], apples[0], apples[0], apples[1]);
		//		final ATerm[] args = { apples[0], apples[1], apples[0], apples[1], apples[0], apples[1], apples[0], apples[1], apples[0], apples[1] };
		//		apples[5] = factory.makeAppl(symmies[3], args);
		apples[6] = apples[2].setArgument(apples[0], 0);

		test_assert(apples[6].equals(apples[1]));
		//		test_assert(apples[1].equals(apples[3]));
		//		test_assert(!apples[2].equals(apples[1]));
		//		test_assert(!apples[2].equals(apples[6]));
		//		test_assert(!apples[1].equals(apples[2]));
		//		test_assert(!apples[2].equals(apples[3]));
		test_assert(!apples[0].equals(apples[1]));

		System.out.println("pass: TestMakeAppl");
	}

	public void testDict()
	{
		ATermList dict = _factory.makeList();
		ATerm key, value;

		for (int i = 0; i < 5; i++)
		{
			key = _factory.parse("key" + i);
			value = _factory.parse("value" + i);
			dict = dict.dictPut(key, value);
		}

		key = _factory.parse("key3");
		value = _factory.parse("value3");
		test_assert(dict.dictGet(key).equals(value));
	}

	public void testParser()
	{
		_factory.parse("f");
		_factory.parse("f(1)");
		_factory.parse("f(1,2)");
		_factory.parse("[]");
		_factory.parse("[1]");
		_factory.parse("[1,2]");
		_factory.parse("<x>");
		_factory.parse("3.14");
		_factory.parse("f(\"x y z\"(),<abc(31)>,[])");
		_factory.parse("home([<name(\"\",String)>,<phone(\"\",PhoneNumber)>])");
		_factory.parse("[ a , b ]");
		_factory.parse("f(a){[x,y],[1,2]}");
		_factory.parse("[(),(a)]");
		System.out.println("parser tests ok.");
	}

	public void testList()
	{
		ATermList list = (ATermList) _factory.parse("[1,2,3]");
		ATermList result = list.remove(_factory.parse("2"));
		test_assert(result.equals(_factory.parse("[1,3]")));

		list = (ATermList) _factory.parse("[1,2,3]");
		result = list.replace(_factory.parse("99"), 1);
		test_assert(result.equals(_factory.parse("[1,99,3]")));

		list = _factory.makeList();
		result = list.append(_factory.parse("1"));
		test_assert(result.equals(_factory.parse("[1]")));

		list = (ATermList) _factory.parse("[]");
		result = _factory.makeList();
		test_assert(result.equals(list));

		//		System.out.println("pass: testList");
	}

	public void testFiles() throws IOException
	{
		final ATerm t1 = _factory.readFromFile(_srcdir + "/test.trm");
		//		System.out.println("done reading test.trm");
		final ATerm t2 = _factory.readFromFile(_srcdir + "/test.taf");
		//		System.out.println("done reading test.taf");

		try (PrintStream stream = new PrintStream(new FileOutputStream("test.trm2")))
		{
			t1.writeToTextFile(stream);
			stream.println();
			stream.close();
			//			System.out.println("done writing test.trm2");
		}

		try (PrintStream stream = new PrintStream(new FileOutputStream("test.taf2")))
		{
			t1.writeToSharedTextFile(stream);
			stream.close();
			//			System.out.println("done writing test.taf2");

			test_assert(t1.equals(t2));
		}
	}

	public void testMatch()
	{
		ATerm t = _factory.parse("node(\"Pico-eval\",box,182,21,62,26)");
		List<Object> result = t.match("node(<str>,<fun>,<int>,<int>,<int>,<int>)");
		test_assert(result != null);

		t = _factory.parse("f(1,2,3)");
		result = t.match("f(1,2,3)");
		test_assert(result != null);

		//		System.out.println("pass: testMatch");
	}

	public void testAll() throws IOException
	{
		testMakeInt();
		testMakeReal();
		testMakeAppl();
		testDict();
		testParser();
		testList();
		testFiles();
		testMatch();
		/*testMakeList();
		testMakePlaceholder();
		testMakeBlob();
		*/
	}

}
