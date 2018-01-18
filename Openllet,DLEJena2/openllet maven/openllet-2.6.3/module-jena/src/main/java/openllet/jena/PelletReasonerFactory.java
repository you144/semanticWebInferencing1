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

import java.util.Optional;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ReasonerFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.ReasonerVocabulary;

/**
 * @author Evren Sirin
 */
public class PelletReasonerFactory implements ReasonerFactory
{
	private static final String URI = "http://pellet.owldl.com";

	private static PelletReasonerFactory _theInstance = new PelletReasonerFactory();

	public static OntModelSpec THE_SPEC = new OntModelSpec(OntModelSpec.OWL_MEM);

	static
	{
		THE_SPEC.setReasonerFactory(_theInstance);

		ReasonerRegistry.theRegistry().register(PelletReasonerFactory.theInstance());
	}

	public static PelletReasonerFactory theInstance()
	{
		return _theInstance;
	}

	private volatile Optional<Model> _reasonerCapabilities = Optional.empty();

	private PelletReasonerFactory()
	{
		//
	}

	public PelletReasoner create()
	{
		return new PelletReasoner(getCapabilities());
	}

	@Override
	public PelletReasoner create(final Resource configuration)
	{
		return new PelletReasoner(null != configuration ? configuration.getModel() : getCapabilities());
	}

	@Override
	public Model getCapabilities()
	{
		if (!_reasonerCapabilities.isPresent())
		{
			_reasonerCapabilities = Optional.of(ModelFactory.createDefaultModel());
			final Resource base = _reasonerCapabilities.get().createResource(URI);
			base.addProperty(ReasonerVocabulary.nameP, "Openllet Reasoner")//
					.addProperty(ReasonerVocabulary.descriptionP, "Reasoner that is backed by the OWL DL reasoner Openllet.")//
					.addProperty(ReasonerVocabulary.supportsP, RDFS.subClassOf)//
					.addProperty(ReasonerVocabulary.supportsP, RDFS.subPropertyOf)//
					.addProperty(ReasonerVocabulary.supportsP, RDFS.member)//
					.addProperty(ReasonerVocabulary.supportsP, RDFS.range)//
					.addProperty(ReasonerVocabulary.supportsP, RDFS.domain)//
					.addProperty(ReasonerVocabulary.supportsP, ReasonerVocabulary.individualAsThingP)//
					.addProperty(ReasonerVocabulary.supportsP, ReasonerVocabulary.directSubClassOf)//
					.addProperty(ReasonerVocabulary.supportsP, ReasonerVocabulary.directSubPropertyOf)//
					.addProperty(ReasonerVocabulary.supportsP, ReasonerVocabulary.directRDFType);//
		}

		return _reasonerCapabilities.get();
	}

	@Override
	public String getURI()
	{
		return URI;
	}
}
