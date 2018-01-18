package opensource.dlejena.utils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import opensource.dlejena.DLEJenaParameters;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 *
 * This class is responsible for extracting the TBox and the ABox from an OWL
 * ontology. It actually makes use of the convenient OWLAPI methods in order to
 * identify the class, role and instance axioms.
 *
 * @author George Meditskos
 */
public class OntologyManager {

    private OntModel tbox;
    private OntModel abox;

    /**
     * The constructor.
     *
     * @param ontologies The list of the ontology URIs.
     */
    public OntologyManager(List<IRI> ontologies) {
        try {
            tbox = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            abox = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            for (IRI ont : ontologies) {
                if (new File(ont.toString()).isFile()) {
                    manager.loadOntologyFromOntologyDocument(new File(ont.toString()));
                } else {
                    manager.loadOntology(ont);
                }
            }

            Set<OWLAxiom> aboxAxioms = new HashSet<OWLAxiom>();
            Set<OWLAxiom> tboxAxioms = new HashSet<OWLAxiom>();
            for (OWLOntology ontology : manager.getOntologies()) {
                Set<OWLAxiom> tempAbox = new HashSet<OWLAxiom>();
                tempAbox.addAll(ontology.getABoxAxioms(false));
                aboxAxioms.addAll(tempAbox);
                Set<OWLAxiom> tempAllAxioms = new HashSet<OWLAxiom>();
                tempAllAxioms.addAll(ontology.getAxioms());
                tempAllAxioms.removeAll(tempAbox);
                tboxAxioms.addAll(tempAllAxioms);
            }

            File aboxTempFile = File.createTempFile("abox", null);
            OWLOntology saveABox = manager.createOntology(IRI.create(DLEJenaParameters.ABOX_ONTOLOGY_BASE_URI));
            manager.addAxioms(saveABox, aboxAxioms);
            manager.saveOntology(saveABox, IRI.create(aboxTempFile.toURI()));

            File tboxTempFile = File.createTempFile("tbox", null);
            OWLOntology saveTBox = manager.createOntology(IRI.create(DLEJenaParameters.TBOX_ONTOLOGY_BASE_URI));
            manager.addAxioms(saveTBox, tboxAxioms);
            manager.saveOntology(saveTBox, IRI.create(tboxTempFile.toURI()));

            tbox.read(tboxTempFile.toURI().toString(), "RDF/XML");
            abox.read(aboxTempFile.toURI().toString(), "RDF/XML");

            aboxTempFile.delete();
            tboxTempFile.delete();
        } catch (OWLOntologyStorageException ex) {
            Logger.getLogger(OntologyManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OntologyManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(OntologyManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns the TBox of the ontology in the form of a Jena OntModel
     *
     * @return tbox The TBox model
     */
    public OntModel getTbox() {
        return tbox;
    }

    /**
     * Returns the ABox of the ontology in the form of a Jena OntModel
     *
     * @return abox The ABox model
     */
    public OntModel getAbox() {
        return cleanABox(abox);
    }

    /**
     * Currently, OWLAPI preserves some TBox references in the ABox ontology.
     * This method tries to remove such references relevant to owl:Class, Object
     * and Datatype properties.
     *
     * @param abox
     * @return
     */
    private OntModel cleanABox(OntModel abox) {
        OntModel temp = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        StmtIterator find = abox.listStatements(new SimpleSelector(null, null, (RDFNode) null) {

            @Override
            public boolean selects(Statement s) {
                if (!s.getPredicate().asNode().sameValueAs(RDF.type.asNode())) {
                    return true;
                } else {
                    if (s.getObject().asNode().sameValueAs(OWL.ObjectProperty.asNode())
                            || s.getObject().asNode().sameValueAs(OWL.DatatypeProperty.asNode())
                            || s.getObject().asNode().sameValueAs(OWL.Class.asNode())) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        });
        return (OntModel) temp.add(find.toList());
    }
}
