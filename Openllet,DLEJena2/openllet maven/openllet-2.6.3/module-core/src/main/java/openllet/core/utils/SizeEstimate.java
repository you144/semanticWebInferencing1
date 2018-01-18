// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.core.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.OpenlletOptions;
import openllet.core.boxes.rbox.Role;
import openllet.core.exceptions.InternalReasonerException;
import openllet.core.taxonomy.Taxonomy;
import openllet.shared.tools.Log;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Petr Kremen
 */
public class SizeEstimate
{
	protected static final Logger _logger = Log.getLogger(SizeEstimate.class);

	public static double UNKNOWN_PROB = 0.5;

	public static boolean CHECK_CONCEPT_SAT = false;

	private long noSatCost;

	private long oneSatCost;

	private long classificationCost;

	private long realizationCost;

	private long instanceRetrievalCost;

	private long classRetrievalCost;

	private final KnowledgeBase _kb;

	private boolean computed = false;

	private int pCount;

	private int opCount;

	private int dpCount;

	private int fpCount;

	private int ifpCount;

	private int tpCount;

	private int spCount;

	private int cCount;

	private int iCount;

	private Map<ATermAppl, Integer> instancesPC;

	private Map<ATermAppl, Integer> directInstancesPC;

	private Map<ATermAppl, Integer> classesPI;

	private Map<ATermAppl, Integer> directClassesPI;

	private Map<ATermAppl, Integer> pairsPP;

	private Map<ATermAppl, Integer> sames;

	private Map<ATermAppl, Integer> differents;

	private Map<ATermAppl, Double> avgObjectsPP;

	private Map<ATermAppl, Integer> equivClasses;

	private Map<ATermAppl, Integer> subClasses;

	private Map<ATermAppl, Integer> directSubClasses;

	private Map<ATermAppl, Integer> superClasses;

	private Map<ATermAppl, Integer> directSuperClasses;

	private Map<ATermAppl, Integer> disjoints;

	private Map<ATermAppl, Integer> complements;

	private Map<ATermAppl, Integer> equivProperties;

	private Map<ATermAppl, Integer> subProperties;

	private Map<ATermAppl, Integer> directSubProperties;

	private Map<ATermAppl, Integer> superProperties;

	private Map<ATermAppl, Integer> directSuperProperties;

	private Map<ATermAppl, Integer> inverses;

	private double avgClassesPI;

	private double avgDirectClassesPI;

	private double avgSamesPI;

	private double avgDifferentsPI;

	private double avgSubClasses;

	private double avgDirectSubClasses;

	private double avgSuperClasses;

	private double avgDirectSuperClasses;

	private double avgEquivClasses;

	private double avgDisjoints;

	private double avgComplements;

	private double avgSubProperties;

	private double avgDirectSubProperties;

	private double avgSuperProperties;

	private double avgDirectSuperProperties;

	private double avgEquivProperties;

	private double avgInversesPP;

	private double avgPairsPP;

	private double avgSubjectsPerProperty;

	private double avgInstancesPC;

	private double avgDirectInstances;

	public SizeEstimate(final KnowledgeBase kb)
	{
		_kb = kb;

		init();
	}

	public boolean isKBComputed()
	{
		return computed;
	}

	private void init()
	{
		cCount = _kb.getClasses().size();
		iCount = _kb.getIndividuals().size();
		pCount = _kb.getProperties().size();

		opCount = _kb.getObjectProperties().size();
		dpCount = _kb.getDataProperties().size();
		fpCount = _kb.getFunctionalProperties().size();
		ifpCount = _kb.getInverseFunctionalProperties().size();
		tpCount = _kb.getTransitiveProperties().size();
		spCount = _kb.getSymmetricProperties().size();

		instancesPC = new HashMap<>();
		directInstancesPC = new HashMap<>();
		classesPI = new HashMap<>();
		directClassesPI = new HashMap<>();
		pairsPP = new HashMap<>();
		avgObjectsPP = new HashMap<>();
		sames = new HashMap<>();
		differents = new HashMap<>();

		subClasses = new HashMap<>();
		directSubClasses = new HashMap<>();
		superClasses = new HashMap<>();
		directSuperClasses = new HashMap<>();
		equivClasses = new HashMap<>();
		disjoints = new HashMap<>();
		complements = new HashMap<>();
		inverses = new HashMap<>();

		subProperties = new HashMap<>();
		directSubProperties = new HashMap<>();
		superProperties = new HashMap<>();
		directSuperProperties = new HashMap<>();
		equivProperties = new HashMap<>();

		instancesPC.put(ATermUtils.TOP, iCount);
		instancesPC.put(ATermUtils.BOTTOM, 0);

		subClasses.put(ATermUtils.TOP, cCount);
		directSubClasses.put(ATermUtils.TOP, cCount); // TODO
		subClasses.put(ATermUtils.BOTTOM, 1);
		directSubClasses.put(ATermUtils.BOTTOM, 0);

		superClasses.put(ATermUtils.TOP, 1);
		directSuperClasses.put(ATermUtils.TOP, 0);
		superClasses.put(ATermUtils.BOTTOM, cCount);
		directSuperClasses.put(ATermUtils.BOTTOM, cCount); // TODO

		equivClasses.put(ATermUtils.TOP, 1);
		equivClasses.put(ATermUtils.BOTTOM, 1);

		disjoints.put(ATermUtils.TOP, 1);
		disjoints.put(ATermUtils.BOTTOM, cCount);

		complements.put(ATermUtils.TOP, 1);
		complements.put(ATermUtils.BOTTOM, 1);

		computed = false;

		avgSubjectsPerProperty = 1;
		avgPairsPP = 1;

		computKBCosts();
	}

	public void computKBCosts()
	{
		final int classCount = _kb.getClasses().size();
		final int indCount = _kb.getClasses().size();

		// FIXME the following constants are chosen based on very limited
		// empirical analysis

		noSatCost = 1;

		oneSatCost = 2;

		// this is a very rough and pretty inaccurate estimate
		// of classification. the number of sat checks done during
		// classification varies widely but due to various optimizations
		// it is a relatively small percentage of the brute-force n^2
		classificationCost = _kb.isClassified() ? noSatCost : classCount * classCount * oneSatCost / 10;

		// the same arguments for classification applies here too
		realizationCost = _kb.isRealized() ? noSatCost : classificationCost + oneSatCost * classCount * indCount;

		// instance retrieval performs sat checks on only individuals that
		// are not ruled out by obvious (non-)instance checks thus it is
		// again a very small percentage
		instanceRetrievalCost = _kb.isRealized() ? noSatCost : indCount * oneSatCost / 100;

		// either KB is realized and this operation is pretty much free or
		// we perform realization and pay the cost
		// NOTE: the behavior to realize the KB at every type retrieval query
		// is subject to change and would require a change here too
		classRetrievalCost = _kb.isRealized() ? noSatCost : realizationCost;
	}

	public void computeAll() // Call from the jena's SparqlDLExecution
	{
		if (!computed)
		{
			computKBCosts();

			if (_logger.isLoggable(Level.FINE))
			{
				_logger.fine("   NoSat cost : " + noSatCost + " ms.");
				_logger.fine("  OneSat cost : " + oneSatCost + " ms.");
				_logger.fine("Classify cost : " + classificationCost + " ms.");
				_logger.fine(" Realize cost : " + realizationCost + " ms.");
				_logger.fine("      IR cost : " + instanceRetrievalCost + " ms.");
				_logger.fine("      CR cost : " + classRetrievalCost + " ms.");
			}

			compute(new HashSet<>(_kb.getClasses()), new HashSet<>(_kb.getProperties()));
			computed = true;
		}
	}

	public boolean isComputed(final ATermAppl term)
	{
		return instancesPC.containsKey(term) || pairsPP.containsKey(term) || classesPI.containsKey(term);
	}

	private static double average(final Collection<Integer> x)
	{
		int a = 0;

		for (final Integer integer : x)
			a += integer;

		return x.size() > 0 ? (double) a / x.size() : 1;
	}

	// Call from the jena's sparql querying system.
	// FIXME the 'synchronized' is a not a good deal because the true effect is indirect, we does'nt really
	// care of precision of computation here; What we care is to not have concurrency exception.
	// concurrency exception came from the change on the KB while doing computation here.
	// As this 'compute' method is call from the inner quering/update system, it's true effect is to create of sequence
	// of call in the KB; so individuals do not change in computation : This 'synchronize' slow all the reasoning processing.
	// A better solution could be to take of every loop in here and use concurrent resilient data structure (on day work).
	public synchronized void compute(final Collection<ATermAppl> cs, final Collection<ATermAppl> ps)
	{
		final Collection<ATermAppl> concepts = new HashSet<>(cs);
		final Collection<ATermAppl> properties = new HashSet<>(ps);

		concepts.removeAll(instancesPC.keySet());
		properties.removeAll(pairsPP.keySet());

		final Optional<Timer> timer = _kb.getTimers().startTimer("sizeEstimate");

		_logger.fine("Size estimation started");

		final Random randomGen = new Random();

		final Map<ATermAppl, Integer> pSubj = new HashMap<>();
		final Map<ATermAppl, Integer> pObj = new HashMap<>();

		final Taxonomy<ATermAppl> taxonomy;

		if (_kb.isClassified())
			taxonomy = _kb.getTaxonomy();
		else
			taxonomy = _kb.getToldTaxonomy();

		for (final Iterator<ATermAppl> i = concepts.iterator(); i.hasNext();)
		{
			final ATermAppl c = i.next();

			if (!_kb.isClass(c))
				continue;

			if (taxonomy.contains(c))
			{
				subClasses.put(c, taxonomy.getFlattenedSubs(c, false).size());
				directSubClasses.put(c, taxonomy.getFlattenedSubs(c, true).size());
				superClasses.put(c, taxonomy.getFlattenedSupers(c, false).size());
				directSuperClasses.put(c, taxonomy.getFlattenedSupers(c, true).size());
				equivClasses.put(c, taxonomy.getEquivalents(c).size() + 1);
			}
			else
			{
				subClasses.put(c, 1);
				directSubClasses.put(c, 1);
				superClasses.put(c, 1);
				directSuperClasses.put(c, 1);
				equivClasses.put(c, 1);
			}

			final Map<ATermAppl, Set<ATermAppl>> toldDisjoints = _kb.getToldDisjoints();

			if (toldDisjoints.containsKey(c))
			{
				disjoints.put(c, toldDisjoints.get(c).size());
				complements.put(c, toldDisjoints.get(c).size()); // TODO
			}
			else
			{
				disjoints.put(c, 1);
				complements.put(c, 1);
			}

			if (_kb.isRealized() && !ATermUtils.isComplexClass(c))
			{
				instancesPC.put(c, _kb.getInstances(c).size());
				directInstancesPC.put(c, _kb.getInstances(c, true).size());
			}
			else
			{
				instancesPC.put(c, 0);
				directInstancesPC.put(c, 0);

				if (CHECK_CONCEPT_SAT)
				{
					if (!_kb.isSatisfiable(c))
						i.remove();

					if (!_kb.isSatisfiable(ATermUtils.makeNot(c)))
					{
						i.remove();
						instancesPC.put(c, _kb.getIndividualsCount());
					}
				}
			}

			if (_logger.isLoggable(Level.FINE))
				_logger.fine("Initialize " + c + " = " + size(c));
		}

		for (final ATermAppl p : properties)
		{
			pairsPP.put(p, 0);
			pSubj.put(p, 0);
			pObj.put(p, 0);

			subProperties.put(p, _kb.getSubProperties(p).size());
			directSubProperties.put(p, _kb.getSubProperties(p, true).size());
			superProperties.put(p, _kb.getSuperProperties(p).size());
			directSuperProperties.put(p, _kb.getSuperProperties(p, true).size());
			equivProperties.put(p, ATermUtils.primitiveOrBottom(_kb.getEquivalentProperties(p)).size() + 1);
			inverses.put(p, _kb.getInverses(p).size());
		}

		for (final ATermAppl ind : _kb.getIndividuals())
		{
			if (!_kb.isIndividual(ind))
				continue;

			sames.put(ind, 1); // TODO
			differents.put(ind, iCount); // TODO

			final float random = randomGen.nextFloat();
			if (random > OpenlletOptions.SAMPLING_RATIO)
				continue;

			if (_kb.isRealized())
			{
				classesPI.put(ind, _kb.getTypes(ind).size());
				directClassesPI.put(ind, _kb.getTypes(ind, true).size());
			}
			else
			{
				classesPI.put(ind, 0);
				directClassesPI.put(ind, 0);

				for (final ATermAppl c : concepts)
				{
					// estimate for number of instances per given class

					final Bool isKnownType = _kb.getABox().isKnownType(ind, c);
					if (isKnownType.isTrue() || CHECK_CONCEPT_SAT && isKnownType.isUnknown() && randomGen.nextFloat() < UNKNOWN_PROB)
					{

						instancesPC.put(c, size(c) + 1);
						directInstancesPC.put(c, size(c) + 1); // TODO
						classesPI.put(ind, classesPerInstance(ind, false) + 1);
						directClassesPI.put(ind, classesPerInstance(ind, true) + 1); // TODO
					}
				}
			}

			for (final ATermAppl p : properties)
			{
				Role role = _kb.getRBox().getRole(p);

				int knownSize = 0;

				if (role.isObjectRole())
				{
					final Set<ATermAppl> knowns = new HashSet<>();
					final Set<ATermAppl> unknowns = new HashSet<>();

					_kb.getABox().getObjectPropertyValues(ind, role, knowns, unknowns, true);
					knownSize = knowns.size();
				}
				else
				{
					final List<ATermAppl> knowns = _kb.getABox().getObviousDataPropertyValues(ind, role, null);
					knownSize = knowns.size();
				}

				if (knownSize > 0)
				{
					if (_logger.isLoggable(Level.FINER))
						_logger.finer("Update " + p + " by " + knownSize);
					pairsPP.put(p, size(p) + knownSize);
					pSubj.put(p, pSubj.get(p) + 1);
				}

				if (role.isObjectRole())
				{
					role = role.getInverse();

					final Set<ATermAppl> knowns = new HashSet<>();
					final Set<ATermAppl> unknowns = new HashSet<>();

					_kb.getABox().getObjectPropertyValues(ind, role, knowns, unknowns, true);

					if (!knowns.isEmpty())
						pObj.put(p, pObj.get(p) + 1);
				}
			}
		}

		if (!computed)
		{
			avgClassesPI = average(classesPI.values());
			avgDirectClassesPI = average(directClassesPI.values());
		}

		if (!_kb.isRealized())
		{
			for (final ATermAppl c : concepts)
			{
				int size = instancesPC.get(c);
				// post processing in case of sampling
				if (size == 0)
					instancesPC.put(c, 1);
				else
					instancesPC.put(c, (int) (size / OpenlletOptions.SAMPLING_RATIO));

				size = directInstancesPC.get(c);

				// postprocessing in case of sampling
				if (size == 0)
					directInstancesPC.put(c, 1);
				else
					directInstancesPC.put(c, (int) (size / OpenlletOptions.SAMPLING_RATIO));
			}

			final int avgCPI = Double.valueOf(avgClassesPI).intValue();
			final int avgDCPI = Double.valueOf(avgDirectClassesPI).intValue();

			for (final ATermAppl i : _kb.getIndividuals())
			{
				Integer size = classesPI.get(i);

				if (size == null)
					size = avgCPI;

				// postprocessing in case of sampling
				if (size == 0)
					classesPI.put(i, 1);
				else
					classesPI.put(i, (int) (size / OpenlletOptions.SAMPLING_RATIO));

				size = directClassesPI.get(i);

				if (size == null)
					size = avgDCPI;

				// postprocessing in case of sampling
				if (size == 0)
					directClassesPI.put(i, 1);
				else
					directClassesPI.put(i, (int) (size / OpenlletOptions.SAMPLING_RATIO));
			}
		}

		for (final ATermAppl p : properties)
		{
			final int size = size(p);
			if (size == 0)
				pairsPP.put(p, 1);
			else
				pairsPP.put(p, (int) (size / OpenlletOptions.SAMPLING_RATIO));

			final Role role = _kb.getRBox().getRole(p);
			final ATermAppl invP = role.getInverse() != null ? role.getInverse().getName() : null;
			int subjCount = pSubj.get(p);
			if (subjCount == 0)
				subjCount = 1;
			int objCount = pObj.get(p);
			if (objCount == 0)
				objCount = 1;

			double avg = Double.valueOf((double) size / subjCount);
			avgObjectsPP.put(p, avg);
			// avgSubjectsPerProperty = Math
			// .max(avgSubjectsPerProperty, subjCount);
			avgSubjectsPerProperty += subjCount;
			if (invP != null)
			{
				avg = Double.valueOf((double) size / objCount);
				avgObjectsPP.put(invP, avg);
				// avgSubjectsPerProperty = Math.max(avgSubjectsPerProperty,
				// objCount);
				avgSubjectsPerProperty += objCount;
			}
		}

		if (properties.size() > 0)
		{
			avgSubjectsPerProperty = avgSubjectsPerProperty / (2 * properties.size());
			avgPairsPP = average(pairsPP.values());
		}
		else
		{
			avgSubjectsPerProperty = 1;
			avgPairsPP = 1;
		}

		avgInstancesPC = average(instancesPC.values());
		avgDirectInstances = average(directInstancesPC.values());
		avgSamesPI = average(sames.values());
		avgDifferentsPI = average(differents.values());

		avgSubClasses = average(subClasses.values());
		avgDirectSubClasses = average(directSubClasses.values());
		avgSuperClasses = average(superClasses.values());
		avgDirectSuperClasses = average(directSuperClasses.values());
		avgEquivClasses = average(equivClasses.values());
		avgDisjoints = average(disjoints.values());
		avgComplements = average(complements.values());
		avgSubProperties = average(subProperties.values());
		avgDirectSubProperties = average(directSubProperties.values());
		avgSuperProperties = average(superProperties.values());
		avgDirectSuperProperties = average(directSuperProperties.values());
		avgEquivProperties = average(equivProperties.values());
		avgInversesPP = average(inverses.values());

		timer.ifPresent(t -> t.stop());

		// printStatistics();

		if (_logger.isLoggable(Level.FINE) && timer.isPresent())
		{
			final NumberFormat nf = new DecimalFormat("0.00");
			_logger.fine("Size estimation finished in " + nf.format(timer.get().getLast() / 1000.0) + " sec");
		}
	}

	@SuppressWarnings("unused")
	private void printStatistics()
	{
		// final StatisticsTable<ATermAppl, String> instances = new
		// StatisticsTable<ATermAppl, String>();
		// instances.add("classes", classesPI);
		// instances.add("sames", sames);
		// instances.add("differents", differents);
		// System.out.println(instances.toString());

		System.out.println("Avg classes per instance:" + avgClassesPI);
		System.out.println("Avg sames per _individual:" + avgSamesPerInstance());
		System.out.println("Avg differents per _individual:" + avgDifferentsPerInstance());

		final StatisticsTable<ATermAppl, String> classes = new StatisticsTable<>();

		classes.add("size", instancesPC);
		classes.add("subs", subClasses);
		classes.add("supers", superClasses);
		classes.add("equivs", equivClasses);
		classes.add("complements", complements);
		classes.add("disjoints", disjoints);

		System.out.println(classes.toString());

		System.out.println("Avg individuals per class:" + avgInstancesPC);
		System.out.println("Avg subclasses:" + avgSubClasses(false));
		System.out.println("Avg direct subclasses:" + avgSubClasses(true));
		System.out.println("Avg superclasses:" + avgSuperClasses(false));
		System.out.println("Avg direct superclasses:" + avgSuperClasses(true));
		System.out.println("Avg equivalent classes:" + avgEquivClasses());
		System.out.println("Avg complement classes:" + avgComplementClasses());
		System.out.println("Avg disjoint classes:" + avgDisjointClasses());

		final StatisticsTable<ATermAppl, String> properties = new StatisticsTable<>();

		properties.add("size", pairsPP);
		properties.add("avgs", avgObjectsPP);
		properties.add("subs", subProperties);
		properties.add("supers", superProperties);
		properties.add("equivs", equivProperties);
		properties.add("inverses", inverses);

		System.out.println(properties.toString());

		System.out.println("Avg pairs per property:" + avgPairsPerProperty());
		System.out.println("Avg subjects per property:" + avgSubjectsPerProperty());
		System.out.println("Avg subproperties:" + avgSubProperties(false));
		System.out.println("Avg superproperties:" + avgSuperProperties(false));
		System.out.println("Avg equivalent properties:" + avgEquivProperties());
	}

	// TODO replace by object type dependent one
	public int size(final ATermAppl c)
	{
		if (instancesPC.containsKey(c))
			return instancesPC.get(c);
		else
			if (pairsPP.containsKey(c))
				return pairsPP.get(c);
			else
			{
				if (_kb.isProperty(c))
					compute(Collections.emptySet(), Collections.singleton(c));
				else
					compute(Collections.singleton(c), Collections.emptySet());
				return size(c);
			}
	}

	public int classesPerInstance(final ATermAppl i, final boolean direct)
	{
		final Map<ATermAppl, Integer> map = direct ? directClassesPI : classesPI;

		if (map.containsKey(i))
			return map.get(i);

		throw new InternalReasonerException("Instance number estimate : " + i + " is not found!");
	}

	public double avg(final ATermAppl pred)
	{
		if (!avgObjectsPP.containsKey(pred))
			compute(Collections.emptySet(), Collections.singleton(pred));
		return avgObjectsPP.get(pred);
	}

	public int getClassCount()
	{
		return cCount;
	}

	public int getInstanceCount()
	{
		return iCount;
	}

	public int getPropertyCount()
	{
		return pCount;
	}

	public int getObjectPropertyCount()
	{
		return opCount;
	}

	public int getDataPropertyCount()
	{
		return dpCount;
	}

	public int getFunctionalPropertyCount()
	{
		return fpCount;
	}

	public int getInverseFunctionalPropertyCount()
	{
		return ifpCount;
	}

	public int getTransitivePropertyCount()
	{
		return tpCount;
	}

	public int getSymmetricPropertyCount()
	{
		return spCount;
	}

	public double avgInstancesPerClass(final boolean direct)
	{
		return direct ? avgDirectInstances : avgInstancesPC;
	}

	public double avgDirectInstancesPerClass()
	{
		return avgDirectInstances;
	}

	public double avgPairsPerProperty()
	{
		return avgPairsPP;
	}

	public double avgSubjectsPerProperty()
	{
		return avgSubjectsPerProperty;
	}

	public double avgSubClasses(final boolean direct)
	{
		return direct ? avgDirectSubClasses : avgSubClasses;
	}

	public double avgSuperClasses(final boolean direct)
	{
		return direct ? avgDirectSuperClasses : avgSuperClasses;
	}

	public double avgEquivClasses()
	{
		return avgEquivClasses;
	}

	public double avgDisjointClasses()
	{
		return avgDisjoints;
	}

	public double avgComplementClasses()
	{
		return avgComplements;
	}

	public double avgSubProperties(final boolean direct)
	{
		return direct ? avgDirectSubProperties : avgSubProperties;
	}

	public double avgSuperProperties(final boolean direct)
	{
		return direct ? avgDirectSuperProperties : avgSuperProperties;
	}

	public double avgEquivProperties()
	{
		return avgEquivProperties;
	}

	public double avgInverseProperties()
	{
		return avgInversesPP;
	}

	public double avgSamesPerInstance()
	{
		return avgSamesPI;
	}

	public double avgDifferentsPerInstance()
	{
		return avgDifferentsPI;
	}

	public double avgClassesPerInstance(final boolean direct)
	{
		return direct ? avgDirectClassesPI : avgClassesPI;
	}

	public double subClasses(final ATermAppl sup, final boolean direct)
	{
		final Map<ATermAppl, Integer> map = direct ? directSubClasses : subClasses;

		if (!map.containsKey(sup))
		{
			compute(Collections.singleton(sup), Collections.emptySet());
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return map.get(sup);
		//
		// throw new InternalReasonerException("Sub estimate for " + sup
		// + " is not found!");
	}

	public double subProperties(final ATermAppl sup, final boolean direct)
	{
		final Map<ATermAppl, Integer> map = direct ? directSubProperties : subProperties;

		if (!map.containsKey(sup))
		{
			compute(Collections.emptySet(), Collections.singleton(sup));
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return map.get(sup);
	}

	public double superClasses(final ATermAppl sup, final boolean direct)
	{
		final Map<ATermAppl, Integer> map = direct ? directSuperClasses : superClasses;

		if (!map.containsKey(sup))
		{
			compute(Collections.singleton(sup), Collections.emptySet());
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return map.get(sup);
	}

	public double superProperties(final ATermAppl sup, final boolean direct)
	{
		final Map<ATermAppl, Integer> map = direct ? directSuperProperties : superProperties;

		if (!map.containsKey(sup))
		{
			compute(Collections.emptySet(), Collections.singleton(sup));
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return map.get(sup);
	}

	public double equivClasses(final ATermAppl sup)
	{
		if (!equivClasses.containsKey(sup))
		{
			compute(Collections.singleton(sup), Collections.emptySet());
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return equivClasses.get(sup);
	}

	public double equivProperties(final ATermAppl sup)
	{
		if (!equivProperties.containsKey(sup))
		{
			compute(Collections.emptySet(), Collections.singleton(sup));
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return equivProperties.get(sup);
	}

	public double sames(final ATermAppl sup)
	{
		if (sames.containsKey(sup))
			return sames.get(sup);

		throw new InternalReasonerException("Sames estimate for " + sup + " is not found!");
	}

	public double differents(final ATermAppl sup)
	{
		if (differents.containsKey(sup))
			return differents.get(sup);

		throw new InternalReasonerException("Sames estimate for " + sup + " is not found!");
	}

	public double disjoints(final ATermAppl sup)
	{
		if (!disjoints.containsKey(sup))
		{
			compute(Collections.singleton(sup), Collections.emptySet());
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return disjoints.get(sup);
	}

	public double complements(final ATermAppl sup)
	{
		if (!complements.containsKey(sup))
		{
			compute(Collections.singleton(sup), Collections.emptySet());
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return complements.get(sup);
	}

	public double inverses(final ATermAppl sup)
	{
		if (!inverses.containsKey(sup))
		{
			compute(Collections.emptySet(), Collections.singleton(sup));
			_logger.fine(() -> "Computing additionally " + sup);
		}
		return inverses.get(sup);
	}

	// _kb.getClasses().size();

	public long getCost(final KBOperation operation)
	{
		long cost;
		switch (operation)
		{

			// TODO
			case IS_DIRECT_TYPE:
				cost = getCost(KBOperation.IS_TYPE);
				break;

			// if realized trivial, oth. 1 sat (more frq than hpv, but less than sc)
			case IS_TYPE:
				cost = _kb.isRealized() ? noSatCost : oneSatCost;
				break;

			// rare sat (nonempty dependency set of an edge in Compl. G.)
			case HAS_PROPERTY_VALUE:
				cost = noSatCost;
				break;

			// use told taxonomy - to be provided by KB - not to classify the whole
			// KB
			// now triv. if classified, otherwise 1 sat
			case IS_SUBCLASS_OF:
			case IS_EQUIVALENT_CLASS:
				cost = oneSatCost;
				break;

			// 1 sat
			case IS_DISJOINT_WITH:
			case IS_COMPLEMENT_OF:
				cost = oneSatCost;
				break;

			// triv
			case IS_SUBPROPERTY_OF:
			case IS_EQUIVALENT_PROPERTY:
				cost = noSatCost;
				break;

			// triv
			case IS_OBJECT_PROPERTY:
			case IS_DATATYPE_PROPERTY:
				cost = noSatCost;
				break;

			// one sat. check if any
			case IS_FUNCTIONAL_PROPERTY:
			case IS_INVERSE_FUNCTIONAL_PROPERTY:
			case IS_TRANSITIVE_PROPERTY:
			case IS_DOMAIN:
			case IS_RANGE:
				cost = oneSatCost;
				break;

			// triv.
			case IS_INVERSE_OF:
				cost = noSatCost;
				break;
			case IS_SYMMETRIC_PROPERTY:
				cost = noSatCost;
				break;

			case IS_ASYMMETRIC_PROPERTY:
				cost = noSatCost;
				break;
			case IS_REFLEXIVE_PROPERTY:
				cost = noSatCost;
				break;
			case IS_IRREFLEXIVE_PROPERTY:
				cost = noSatCost;
				break;

			case GET_INVERSES:
				cost = noSatCost;
				break;

			case GET_INSTANCES:
				cost = instanceRetrievalCost;
				break;

			// TODO
			case GET_DIRECT_INSTANCES:
				cost = instanceRetrievalCost + classificationCost;
				break;

			// if realized triv, otherwise TODO
			// binary class retrieval. Currently, realization
			case GET_TYPES:
				cost = classRetrievalCost;
				break;

			// TODO
			case GET_DIRECT_TYPES:
				cost = getCost(KBOperation.GET_TYPES);
				break;

			// instance retrieval for a small set of instances, meanwhile as
			// instance retrieval.
			case GET_PROPERTY_VALUE:
				cost = noSatCost;// (long) (0.01 * instanceRetrievalCost);
				break;

			// 1 sat (rare)
			case IS_SAME_AS:
				cost = oneSatCost;
				break;

			case GET_SAMES:
				cost = oneSatCost;
				break;

			// 1 sat
			case IS_DIFFERENT_FROM:
				cost = oneSatCost;
				break;

			// meanwhile instance retrieval
			case GET_DIFFERENTS:
				cost = instanceRetrievalCost;
				break;

			// trivial
			case GET_OBJECT_PROPERTIES:
			case GET_DATATYPE_PROPERTIES:
				cost = noSatCost;
				break;

			// currently trivial - not complete impl.
			case GET_FUNCTIONAL_PROPERTIES:
			case GET_INVERSE_FUNCTIONAL_PROPERTIES:
			case GET_TRANSITIVE_PROPERTIES:
			case GET_SYMMETRIC_PROPERTIES:

			case GET_ASYMMETRIC_PROPERTIES:
			case GET_REFLEXIVE_PROPERTIES:
			case GET_IRREFLEXIVE_PROPERTIES:

			case GET_DOMAINS:
			case GET_RANGES:
				cost = noSatCost;
				break;

			// trivial if classified and named, otherwise classification
			case GET_SUB_OR_SUPERCLASSES:
			case GET_DIRECT_SUB_OR_SUPERCLASSES: // TODO
			case GET_EQUIVALENT_CLASSES:
				cost = classificationCost;
				break;

			// classification
			case GET_DISJOINT_CLASSES:
			case GET_COMPLEMENT_CLASSES:
				cost = classificationCost;
				break;

			// trivial
			case GET_SUB_OR_SUPERPROPERTIES:
			case GET_DIRECT_SUB_OR_SUPERPROPERTIES: // TODO
			case GET_EQUIVALENT_PROPERTIES:
				cost = noSatCost;
				break;

			default:
				throw new IllegalArgumentException("Unknown KB Operation type : " + operation);
		}

		return cost;
	}
}
