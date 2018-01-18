// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet;

import static openllet.OpenlletCmdOptionArg.NONE;
import static openllet.OpenlletCmdOptionArg.REQUIRED;

import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;
import openllet.core.utils.FileUtils;
import openllet.owlapi.EntailmentChecker;
import openllet.owlapi.OWLAPILoader;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.explanation.io.manchester.ManchesterSyntaxObjectRenderer;
import openllet.owlapi.explanation.io.manchester.TextBlockWriter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * <p>
 * Description: Given an input ontology check if the axioms in the output ontology are all entailed. If not, report either the first non-entailment or all
 * non-entailments.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Markus Stocker
 */
public class OpenlletEntailment extends OpenlletCmdApp
{

	private String entailmentFile;
	private boolean findAll;

	public OpenlletEntailment()
	{
	}

	@Override
	public String getAppId()
	{
		return "OpenlletEntailment: Check if all axioms are entailed by the ontology";
	}

	@Override
	public String getAppCmd()
	{
		return "openllet entail " + getMandatoryOptions() + "[_options] <file URI>...";
	}

	@Override
	public OpenlletCmdOptions getOptions()
	{
		final OpenlletCmdOptions options = getGlobalOptions();

		options.add(getIgnoreImportsOption());

		OpenlletCmdOption option = new OpenlletCmdOption("entailment-file");
		option.setShortOption("e");
		option.setType("<file URI>");
		option.setDescription("Entailment ontology URI");
		option.setIsMandatory(true);
		option.setArg(REQUIRED);
		options.add(option);

		option = new OpenlletCmdOption("all");
		option.setShortOption("a");
		option.setDefaultValue(false);
		option.setDescription("Show all non-entailments");
		option.setDefaultValue(findAll);
		option.setIsMandatory(false);
		option.setArg(NONE);
		options.add(option);

		return options;
	}

	@Override
	public void run()
	{
		entailmentFile = _options.getOption("entailment-file").getValueAsString();
		findAll = _options.getOption("all").getValueAsBoolean();

		final OWLAPILoader loader = (OWLAPILoader) getLoader("OWLAPI");

		getKB();

		final OpenlletReasoner reasoner = loader.getReasoner();

		OWLOntology entailmentOntology = null;
		try
		{
			verbose("Loading entailment file: ");
			verbose(entailmentFile);
			final IRI entailmentFileURI = IRI.create(FileUtils.toURI(entailmentFile));
			entailmentOntology = loader.getManager().loadOntology(entailmentFileURI);
		}
		catch (final Exception e)
		{
			throw new OpenlletCmdException(e);
		}

		final EntailmentChecker checker = new EntailmentChecker(reasoner);
		final Set<OWLLogicalAxiom> axioms = entailmentOntology.logicalAxioms().collect(Collectors.toSet());

		verbose("Check entailments for (" + axioms.size() + ") axioms");
		startTask("Checking");
		final Set<OWLAxiom> nonEntailments = checker.findNonEntailments(axioms, findAll);
		finishTask("Checking");

		if (nonEntailments.isEmpty())
			output("All axioms are entailed.");
		else
		{
			output("Non-entailments (" + nonEntailments.size() + "): ");

			int index = 1;
			final TextBlockWriter writer = new TextBlockWriter(new PrintWriter(System.out));
			final ManchesterSyntaxObjectRenderer renderer = new ManchesterSyntaxObjectRenderer(writer);
			writer.println();
			for (final OWLAxiom axiom : nonEntailments)
			{
				writer.print(index++);
				writer.print(")");
				writer.printSpace();

				writer.startBlock();
				axiom.accept(renderer);
				writer.endBlock();
				writer.println();
			}
			writer.flush();
		}
	}

}
