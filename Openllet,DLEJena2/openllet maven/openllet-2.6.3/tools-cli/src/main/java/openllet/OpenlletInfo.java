package openllet;

import static openllet.OpenlletCmdOptionArg.NONE;
import static openllet.OpenlletCmdOptionArg.REQUIRED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import openllet.core.utils.FileUtils;
import openllet.owlapi.LimitedMapIRIMapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.util.DLExpressivityChecker;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class OpenlletInfo extends OpenlletCmdApp
{
	private final List<OWLProfile> _profiles = Arrays.asList(new OWL2ELProfile(), new OWL2QLProfile(), new OWL2RLProfile(), new OWL2DLProfile(), new OWL2Profile());

	@Override
	public String getAppCmd()
	{
		return "openllet info " + getMandatoryOptions() + "[options] <file URI>...";
	}

	@Override
	public String getAppId()
	{
		return "OpenlletInfo: Display information and statistics about 1 or more ontologies";
	}

	@Override
	public OpenlletCmdOptions getOptions()
	{
		final OpenlletCmdOptions options = new OpenlletCmdOptions();

		//Don't call getGlobalOptions(), since we override the behaviour of _verbose
		final OpenlletCmdOption helpOption = new OpenlletCmdOption("help");
		helpOption.setShortOption("h");
		helpOption.setDescription("Print this message");
		helpOption.setDefaultValue(false);
		helpOption.setIsMandatory(false);
		helpOption.setArg(NONE);
		options.add(helpOption);

		final OpenlletCmdOption verboseOption = new OpenlletCmdOption("verbose");
		verboseOption.setShortOption("v");
		verboseOption.setDescription("More _verbose output");
		verboseOption.setDefaultValue(false);
		verboseOption.setIsMandatory(false);
		verboseOption.setArg(NONE);
		options.add(verboseOption);

		final OpenlletCmdOption configOption = new OpenlletCmdOption("config");
		configOption.setShortOption("C");
		configOption.setDescription("Use the selected configuration file");
		configOption.setIsMandatory(false);
		configOption.setType("configuration file");
		configOption.setArg(REQUIRED);
		options.add(configOption);

		final OpenlletCmdOption option = new OpenlletCmdOption("merge");
		option.setShortOption("m");
		option.setDescription("Merge the ontologies");
		option.setDefaultValue(false);
		option.setIsMandatory(false);
		option.setArg(OpenlletCmdOptionArg.NONE);
		options.add(option);

		options.add(getIgnoreImportsOption());

		return options;
	}

	@Override
	public void run()
	{

		try
		{
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			final Collection<String> inputFiles = FileUtils.getFileURIs(getInputFiles());

			final LimitedMapIRIMapper iriMapper = new LimitedMapIRIMapper();
			final OWLOntology baseOntology = manager.createOntology();
			manager.getIRIMappers().clear();

			if (_options.getOption("ignore-imports").getValueAsBoolean())
			{
				manager.getIRIMappers().add(iriMapper);
				manager.setOntologyLoaderConfiguration(manager.getOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
			}
			else
			{
				manager.setOntologyLoaderConfiguration(manager.getOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
			}

			if (inputFiles.size() > 1)
				for (final String inputFile : inputFiles)
					addFile(inputFile, manager, iriMapper, baseOntology);
			else
				addSingleFile(inputFiles.iterator().next(), manager, iriMapper); //Prevent ugly OWLAPI messages

			manager.removeOntology(baseOntology);

			if (_options.getOption("merge").getValueAsBoolean())
				manager = mergeOntologiesInNewManager(manager);

			printStats(manager);

		}
		catch (final Exception e)
		{
			throw new OpenlletCmdException(e);
		}
	}

	private void addFile(final String inputFile, final OWLOntologyManager manager, final LimitedMapIRIMapper iriMapper, final OWLOntology baseOntology)
	{
		try
		{
			final IRI iri = IRI.create(inputFile);
			iriMapper.addAllowedIRI(iri);

			final OWLImportsDeclaration declaration = manager.getOWLDataFactory().getOWLImportsDeclaration(iri);
			manager.applyChange(new AddImport(baseOntology, declaration));
			manager.makeLoadImportRequest(declaration);
		}
		catch (final Exception e)
		{
			if (_verbose)
				System.err.println(e.getLocalizedMessage());
		}
	}

	private void addSingleFile(final String inputFile, final OWLOntologyManager manager, final LimitedMapIRIMapper iriMapper)
	{
		try
		{
			final IRI iri = IRI.create(inputFile);
			iriMapper.addAllowedIRI(iri);
			manager.loadOntologyFromOntologyDocument(iri);
		}
		catch (final Exception e)
		{
			if (_verbose)
				System.err.println(e.getLocalizedMessage());
		}
	}

	private OWLOntologyManager mergeOntologiesInNewManager(final OWLOntologyManager manager) throws OWLOntologyCreationException
	{
		final OWLOntologyManager newManager = OWLManager.createOWLOntologyManager();
		final OWLOntology merged = newManager.createOntology();
		final List<OWLOntologyChange> changes = new ArrayList<>();

		manager.ontologies().forEach(ontology -> ontology.axioms().forEach(ax -> changes.add(new AddAxiom(merged, ax))));
		newManager.applyChanges(changes);
		return newManager;
	}

	private void printStats(final OWLOntologyManager manager)
	{
		manager.ontologies().forEach(ontology ->
		{
			final String ontologyLocation = manager.getOntologyDocumentIRI(ontology) != null ? manager.getOntologyDocumentIRI(ontology).toString() : "ontology";
			final String ontologyBaseURI = ontology.getOntologyID().getOntologyIRI().isPresent() ? ontology.getOntologyID().getOntologyIRI().get().toQuotedString() : "";
			output("Information about " + ontologyLocation + " (" + ontologyBaseURI + ")");
			if (_verbose)
				printOntologyHeader(ontology);
			final DLExpressivityChecker expressivityChecker = new DLExpressivityChecker(Collections.singleton(ontology));
			output("OWL Profile = " + getProfile(ontology));
			output("DL Expressivity = " + expressivityChecker.getDescriptionLogicName());
			output("Axioms = " + ontology.getAxiomCount());
			output("Logical Axioms = " + ontology.getLogicalAxiomCount());
			output("GCI Axioms = " + ontology.classesInSignature().count());
			output("Individuals = " + ontology.individualsInSignature().count());
			output("Classes = " + ontology.classesInSignature().count());
			output("Object Properties = " + ontology.objectPropertiesInSignature().count());
			output("Data Properties = " + ontology.dataPropertiesInSignature().count());
			output("Annotation Properties = " + ontology.annotationPropertiesInSignature().count());
			output("Direct Imports:");
			ontology.importsDeclarations().sorted().forEach(imp -> output(imp.getIRI().toString()));
			output("");
		});
	}

	private String getProfile(final OWLOntology ontology)
	{
		for (final OWLProfile profile : _profiles)
			if (profile.checkOntology(ontology).isInProfile())
				return profile.getName();
		return "Unknown Profile";
	}

	private void printOntologyHeader(final OWLOntology ontology)
	{
		ontology.annotations().forEach(annotation ->
		{
			final IRI property = annotation.getProperty().getIRI();
			final OWLAnnotationValue value = annotation.getValue();

			if (property.equals(OWLRDFVocabulary.OWL_VERSION_INFO.getIRI()))
				verbose("Version Info = " + getString(value));
			else
				if (property.equals(OWLRDFVocabulary.OWL_PRIOR_VERSION.getIRI()))
					verbose("Prior Version Info = " + getString(value));
				else
					if (property.equals(OWLRDFVocabulary.OWL_BACKWARD_COMPATIBLE_WITH.getIRI()))
						verbose("Backward Compatible With = " + getString(value));
					else
						if (property.equals(OWLRDFVocabulary.OWL_INCOMPATIBLE_WITH.getIRI()))
							verbose("Incompatible With = " + getString(value));
		});
	}

	private String getString(final OWLAnnotationValue value)
	{
		if (value instanceof OWLLiteral)
			return ((OWLLiteral) value).getLiteral();
		else
			return value.toString();
	}
}
