package openllet.core.datatypes.types.text;

import openllet.aterm.ATermAppl;
import openllet.core.datatypes.AbstractBaseDatatype;
import openllet.core.datatypes.Datatype;
import openllet.core.datatypes.RestrictedDatatype;
import openllet.core.datatypes.exceptions.InvalidLiteralException;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.Namespaces;

/**
 * <p>
 * Copyright: Copyright (c) 2009
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Héctor Pérez-Urbina
 */
public class XSDLanguage extends AbstractBaseDatatype<ATermAppl>
{

	private static final XSDLanguage instance = new XSDLanguage();
	private static final RDFPlainLiteral RDF_PLAIN_LITERAL = RDFPlainLiteral.getInstance();

	static
	{
		RestrictedTextDatatype.addPermittedDatatype(instance.getName());
	}

	public static XSDLanguage getInstance()
	{
		return instance;
	}

	private final RestrictedDatatype<ATermAppl> dataRange;

	private XSDLanguage()
	{
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "language"));
		dataRange = new RestrictedTextDatatype(this, RestrictedTextDatatype.LANGUAGE);
	}

	@Override
	public RestrictedDatatype<ATermAppl> asDataRange()
	{
		return dataRange;
	}

	@Override
	public ATermAppl getCanonicalRepresentation(final ATermAppl input) throws InvalidLiteralException
	{
		return getValue(input);
	}

	@Override
	public ATermAppl getLiteral(final Object value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Datatype<?> getPrimitiveDatatype()
	{
		return RDF_PLAIN_LITERAL;
	}

	@Override
	public ATermAppl getValue(final ATermAppl literal) throws InvalidLiteralException
	{
		final String lexicalForm = getLexicalForm(literal);
		return RDF_PLAIN_LITERAL.getCanonicalRepresentation(ATermUtils.makePlainLiteral(lexicalForm));
	}

	@Override
	public boolean isPrimitive()
	{
		return false;
	}
}
