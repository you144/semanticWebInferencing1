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

package openllet.jena;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import openllet.aterm.ATermAppl;
import openllet.core.datatypes.Datatypes;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.QNameProvider;
import openllet.jena.vocabulary.OWL2;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.OWL;

/**
 * Utility functions related to Jena structures. The functions here may have similar functionality to the ones in ATermUtils but they are provided here because
 * ATermUtils is supposed to be library-independent (it should NOT import Jena packages otherwise applications based on OWL-API would require Jena packages)
 *
 * @author Evren Sirin
 */
public class JenaUtils
{
	final public static Literal XSD_BOOLEAN_TRUE = ResourceFactory.createTypedLiteral(Boolean.TRUE.toString(), XSDDatatype.XSDboolean);

	public static Predicate<ATermAppl> _isGrapheNode = aTermAppl -> aTermAppl.getArity() == 0 || ATermUtils.isLiteral(aTermAppl) || ATermUtils.isBnode(aTermAppl) || aTermAppl.equals(ATermUtils.TOP) || aTermAppl.equals(ATermUtils.BOTTOM) || aTermAppl.equals(ATermUtils.TOP_DATA_PROPERTY) || aTermAppl.equals(ATermUtils.BOTTOM_DATA_PROPERTY) || aTermAppl.equals(ATermUtils.TOP_OBJECT_PROPERTY) || aTermAppl.equals(ATermUtils.BOTTOM_OBJECT_PROPERTY);

	static public ATermAppl makeLiteral(final LiteralLabel jenaLiteral)
	{
		final String lexicalValue = jenaLiteral.getLexicalForm();
		final String datatypeURI = jenaLiteral.getDatatypeURI();
		ATermAppl literalValue = null;

		if (datatypeURI != null)
			literalValue = ATermUtils.makeTypedLiteral(lexicalValue, datatypeURI);
		else
			if (jenaLiteral.language() != null)
				literalValue = ATermUtils.makePlainLiteral(lexicalValue, jenaLiteral.language());
			else
				literalValue = ATermUtils.makePlainLiteral(lexicalValue);

		return literalValue;
	}

	static public ATermAppl makeATerm(final RDFNode node)
	{
		return makeATerm(node.asNode());
	}

	static public ATermAppl makeATerm(final Node node)
	{
		if (node.isLiteral())
			return makeLiteral(node.getLiteral());
		else
			if (node.isBlank())
				return ATermUtils.makeBnode(node.getBlankNodeLabel());
			else
				if (node.isURI())
				{
					if (node.equals(OWL.Thing.asNode()))
						return ATermUtils.TOP;
					else
						if (node.equals(OWL.Nothing.asNode()))
							return ATermUtils.BOTTOM;
						else
							if (node.equals(OWL2.topDataProperty.asNode()))
								return ATermUtils.TOP_DATA_PROPERTY;
							else
								if (node.equals(OWL2.bottomDataProperty.asNode()))
									return ATermUtils.BOTTOM_DATA_PROPERTY;
								else
									if (node.equals(OWL2.topObjectProperty.asNode()))
										return ATermUtils.TOP_OBJECT_PROPERTY;
									else
										if (node.equals(OWL2.bottomObjectProperty.asNode()))
											return ATermUtils.BOTTOM_OBJECT_PROPERTY;
										else
											return ATermUtils.makeTermAppl(node.getURI());
				}
				else
					if (node.isVariable())
						return ATermUtils.makeVar(node.getName());

		return null;
	}

	static public Node makeGraphLiteral(final ATermAppl literal)
	{
		Node node;

		final String lexicalValue = ((ATermAppl) literal.getArgument(0)).getName();
		final ATermAppl lang = (ATermAppl) literal.getArgument(1);
		final ATermAppl datatype = (ATermAppl) literal.getArgument(2);

		if (datatype.equals(ATermUtils.PLAIN_LITERAL_DATATYPE))
		{
			if (lang.equals(ATermUtils.EMPTY))
				node = NodeFactory.createLiteral(lexicalValue);
			else
				node = NodeFactory.createLiteral(lexicalValue, lang.getName(), false);
		}
		else
			if (datatype.equals(Datatypes.XML_LITERAL))
				node = NodeFactory.createLiteral(lexicalValue, "", true);
			else
			{
				final RDFDatatype type = TypeMapper.getInstance().getTypeByName(datatype.getName());
				node = NodeFactory.createLiteral(lexicalValue, "", type);
			}

		return node;
	}

	static public Node makeGraphResource(final ATermAppl term)
	{
		if (ATermUtils.isBnode(term))
			return NodeFactory.createBlankNode(new BlankNodeId(((ATermAppl) term.getArgument(0)).getName()));
		else
			if (term.equals(ATermUtils.TOP))
				return OWL.Thing.asNode();
			else
				if (term.equals(ATermUtils.BOTTOM))
					return OWL.Nothing.asNode();
				else
					if (term.equals(ATermUtils.TOP_DATA_PROPERTY))
						return OWL2.topDataProperty.asNode();
					else
						if (term.equals(ATermUtils.BOTTOM_DATA_PROPERTY))
							return OWL2.bottomDataProperty.asNode();
						else
							if (term.equals(ATermUtils.TOP_OBJECT_PROPERTY))
								return OWL2.topObjectProperty.asNode();
							else
								if (term.equals(ATermUtils.BOTTOM_OBJECT_PROPERTY))
									return OWL2.bottomObjectProperty.asNode();
								else
									if (term.getArity() == 0)
										return NodeFactory.createURI(term.getName());

		//		if (term.getName().equals(ATermUtils.INVFUN.getName()))
		//			return OWL.inverseOf.asNode(); //	term.getArgument(0); // XXX Que devient le parametre ?

		PelletReasoner._logger.warning("Term " + term + " can't be convert into Node");
		return null;
	}

	static public Optional<Node> makeGraphNode(final ATermAppl value)
	{
		return Optional.ofNullable(ATermUtils.isLiteral(value) ? makeGraphLiteral(value) : makeGraphResource(value));
	}

	static public Literal makeLiteral(final ATermAppl literal, final Model model)
	{
		return (Literal) model.asRDFNode(makeGraphLiteral(literal));
	}

	static public Optional<Resource> makeResource(final ATermAppl term, final Model model)
	{
		return Optional.ofNullable(makeGraphResource(term)).map(model::asRDFNode).map(Resource.class::cast);
	}

	static public Optional<RDFNode> makeRDFNode(final ATermAppl term, final Model model)
	{
		return makeGraphNode(term).map(model::asRDFNode);
	}

	static public QNameProvider makeQNameProvider(final PrefixMapping mapping)
	{
		final QNameProvider qnames = new QNameProvider();

		final Iterator<Map.Entry<String, String>> entries = mapping.getNsPrefixMap().entrySet().iterator();
		while (entries.hasNext())
		{
			final Map.Entry<String, String> entry = entries.next();
			final String prefix = entry.getKey();
			final String uri = entry.getValue();

			qnames.setMapping(prefix, uri);
		}

		return qnames;
	}
}
