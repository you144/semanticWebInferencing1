// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.pellint.rdfxml;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Harris Lin
 */
public class RDFModelReader
{
	public RDFModel read(final String uri, final boolean loadImports)
	{
		final RDFModel m = new RDFModel();

		final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		model.getDocumentManager().setProcessImports(loadImports);

		FileManager.get().readModel(model, uri);

		final StmtIterator stmtIter = model.listStatements();

		while (stmtIter.hasNext())
		{
			final Statement stmt = stmtIter.nextStatement();
			m.addStatement(stmt);
		}

		return m;
	}
}
