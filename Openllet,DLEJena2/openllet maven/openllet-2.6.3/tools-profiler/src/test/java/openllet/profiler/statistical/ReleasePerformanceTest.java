package openllet.profiler.statistical;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import openllet.profiler.ProfileKB;
import openllet.profiler.ProfileKB.LoaderType;
import openllet.profiler.ProfileKB.MemoryProfiling;
import openllet.profiler.ProfileKB.Task;
import openllet.profiler.Result;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Executes several performance tests on the current release, and compares the results with previous releases to see if there are statistically significant
 * performance regressions
 *
 * @author Pedro Oliveira <pedro@clarkparsia.com>
 */
@RunWith(Parameterized.class)
@Category(ReleaseTests.class)
@Ignore("Enable to test release performance (maybe not compatible with Travis) ")
public class ReleasePerformanceTest
{

	private static final int PARSE = Task.Parse.ordinal();
	private static final int LOAD = Task.Load.ordinal();
	private static final int CONSISTENCY = Task.Consistency.ordinal();
	private static final int CLASSIFY = Task.Classify.ordinal();
	private static final int REALIZE = Task.Realize.ordinal();
	private static final MathStatUtils math = new MathStatUtils();

	/**
	 * PARAMETERS
	 */
	private static String ONTOLOGIES = ""; //File with the location of the ontologies on which we want to perform the tests and compare with previous releases

	private static int[] RELEASES_TO_COMPARE = { 0, 1, 2 }; //Previous releases that we want to compare with. 0 means the latest one, etc...

	private static int ITERATIONS = 30; //Number of iterations (at least >2, so statistical significance tests can work)

	private static double ALPHA = 0.001; //Confidence Interval = 1-alpha. Bigger the alpha, bigger is the probability of statistical significant changes

	private static double MAX_PERFORMANCE_DECREASE = 0.05; //Maximum % of performance decrease

	private static String RELEASE_REPOSITORY = "profiler/releases"; //Directory with the previous releases

	private static Task TASK = Task.Realize; //Task to execute

	private static LoaderType LOADER = LoaderType.JENA; //Loader

	private static boolean WARMUP = true; //Should we perform JVM warmup?

	@Parameters
	public static List<Object[]> params() throws IOException
	{
		loadProperties("src/main/resources/releasetesting.properties");

		final List<Object[]> params = new ArrayList<>();

		final ReleaseManager manager = new ReleaseManager();
		manager.load(RELEASE_REPOSITORY);

		final Release current = getCurrentRelease();
		final List<Release> previousReleases = manager.getReleases();

		if (!previousReleases.isEmpty())
			for (final int revNumber : RELEASES_TO_COMPARE)
				if (revNumber >= 0 && revNumber < previousReleases.size()) //If they are available, try to compare
				{
					final Release previous = previousReleases.get(revNumber);
					for (final Entry<String, List<ReleaseStatistics>> currStats : current.getAllStatistics().entrySet()) //For all the ontologies in the _current test set
					{
						final List<ReleaseStatistics> previousStats = previous.getStatistics(currStats.getKey()); //If the previous release contains results about this ontology, compare. otherwise, ignore and continue
						if (previousStats == null)
							continue;

						System.out.println("Comparing with [" + revNumber + "]: " + previous.getVersion());
						params.add(new Object[] { currStats.getValue(), previousStats, previous.getVersion(), currStats.getKey() });
					}
				}

		try
		{
			ReleaseUtils.writeToFile(current, new File(RELEASE_REPOSITORY, current.getVersion() + "_" + current.getReleaseDate()).getAbsolutePath()); //Save the _current results
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		return params;
	}

	private static void loadProperties(final String filename)
	{
		try
		{
			final Properties properties = new Properties();
			properties.load(new FileInputStream(filename));

			RELEASE_REPOSITORY = properties.getProperty("REPOSITORY", "profiler/releases");
			ITERATIONS = Integer.parseInt(properties.getProperty("ITERATIONS", "30"));

			final double perfdec = Double.parseDouble(properties.getProperty("MAX_PERFORMANCE_DECREASE", "0.05"));
			if ((perfdec < 0) || (perfdec > 1))
				System.err.println("Invalid maximum performance decrease: " + perfdec);
			else
				MAX_PERFORMANCE_DECREASE = perfdec;

			final double _alpha = 1 - Double.parseDouble(properties.getProperty("CONFIDENCE_LEVEL", "0.999"));
			if ((_alpha <= 0) || (_alpha > 0.5))
				System.err.println("Invalid confidence level: " + (1 - _alpha));
			else
				ALPHA = _alpha;

			final String[] rels = properties.getProperty("RELEASES_TO_COMPARE", "0,5,10").split(",\\s*");
			RELEASES_TO_COMPARE = new int[rels.length];
			for (int i = 0; i < rels.length; i++)
				RELEASES_TO_COMPARE[i] = Integer.parseInt(rels[i]);

			ONTOLOGIES = properties.getProperty("ONTOLOGIES", "");

			final String tsk = properties.getProperty("TASK", "Realize");
			if (tsk.equalsIgnoreCase("Realize"))
				TASK = Task.Realize;
			else
				if (tsk.equalsIgnoreCase("Classify"))
					TASK = Task.Classify;
				else
					if (tsk.equalsIgnoreCase("Consistency"))
						TASK = Task.Consistency;
					else
						if (tsk.equalsIgnoreCase("Load"))
							TASK = Task.Load;
						else
							if (tsk.equalsIgnoreCase("Parse"))
								TASK = Task.Parse;
							else
								System.err.println("Invalid task: " + tsk);

			final String _loader = properties.getProperty("LOADER", "Jena");
			if (_loader.equalsIgnoreCase("Jena"))
				LOADER = LoaderType.JENA;
			else
				if (_loader.equalsIgnoreCase("OWLAPI"))
					LOADER = LoaderType.OWLAPI;
				else
					System.err.println("Invalid loader: " + _loader);

			WARMUP = Boolean.parseBoolean(properties.getProperty("WARMUP", "True"));

		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private static Release getCurrentRelease() throws IOException
	{
		final Release current = new Release();

		final ProfileKB pkb = new ProfileKB();
		pkb.setMemoryProfiling(MemoryProfiling.ALL_SIZE);
		pkb.setLoaderType(LOADER);
		pkb.setTask(TASK);

		//Get all the results for the _current release
		final Map<String, List<Result<Task>>> results = new LinkedHashMap<>();
		try (final BufferedReader reader = new BufferedReader(new FileReader(ONTOLOGIES)))
		{
			String line;

			while ((line = reader.readLine()) != null)
			{
				if (line.trim().length() == 0)
					continue;

				final String[] files = line.split(" ");
				final String name = new File(files[0]).getName();

				if (WARMUP)
				{
					//Get the estimated time for this test
					double estimatedTime = 0;
					for (final Result<Task> result : pkb.profile(files))
						estimatedTime += result.getAvgTime();

					//Get the number of warmup iterations to perform, based on the estimated time
					final int nWarmupIterations = getNumberOfWarmupIterations(estimatedTime) - 1;

					//Warmup
					for (int i = 0; i < nWarmupIterations; i++)
						pkb.profile(files);
				}

				for (int i = 0; i < ITERATIONS; i++)
				{
					final List<Result<Task>> res = new ArrayList<>(pkb.profile(files));
					final List<Result<Task>> previousRes = results.get(name);
					if (previousRes == null)
						results.put(name, res);
					else
						for (int j = 0; j < res.size() && j < previousRes.size(); j++)
							previousRes.get(j).addIteration(res.get(j));
				}

			}
		}

		//Extract the necessary statistics from the results
		for (final Entry<String, List<Result<Task>>> entry : results.entrySet())
		{
			final List<ReleaseStatistics> stats = new ArrayList<>();
			for (final Result<Task> task : entry.getValue())
				stats.add(new ReleaseStatistics(task));
			current.addStatistics(entry.getKey(), stats);
		}
		return current;
	}

	/**
	 * This logarithmic function behaves relatively well on estimating the number of warmup iterations. Goes from 85 iterations when estimatedTime=0.01s, to 1
	 * iteration when estimatedTime ~>= 25s The function can be tuned using Wolfram Alpha: fit {{0.01,100},{0.1,50},{1,30},{10,10},{20,5},{30,1}}
	 *
	 * @param estimatedTime
	 * @return
	 */
	private static int getNumberOfWarmupIterations(final double estimatedTime)
	{
		final int n = (int) Math.round(36 - 11 * Math.log(estimatedTime));
		return n > 0 ? n : 1;
	}

	private final List<ReleaseStatistics> _current;
	private final List<ReleaseStatistics> _previous;
	private final String _message;

	public ReleasePerformanceTest(final List<ReleaseStatistics> current, final List<ReleaseStatistics> previous, final String previousReleaseVersion, final String ontology)
	{
		_current = current;
		_previous = previous;
		_message = "\tOntology: " + ontology + "\tRelease: " + previousReleaseVersion;
	}

	@Test
	public void parseTimeTest()
	{
		timeIncrease(PARSE);
	}

	@Test
	public void parseMemoryTest()
	{
		memoryIncrease(PARSE);
	}

	@Test
	public void loadTimeTest()
	{
		timeIncrease(LOAD);
	}

	@Test
	public void loadMemoryTest()
	{
		memoryIncrease(LOAD);
	}

	@Test
	public void consistencyTimeTest()
	{
		timeIncrease(CONSISTENCY);
	}

	@Test
	public void consistencyMemoryTest()
	{
		memoryIncrease(CONSISTENCY);
	}

	@Test
	public void classificationTimeTest()
	{
		timeIncrease(CLASSIFY);
	}

	@Test
	public void classificationMemoryTest()
	{
		memoryIncrease(CLASSIFY);
	}

	@Test
	public void realizationTimeTest()
	{
		timeIncrease(REALIZE);
	}

	@Test
	public void realizationMemoryTest()
	{
		memoryIncrease(REALIZE);
	}

	private void timeIncrease(final int task)
	{
		assumeTrue(_current.size() > task && _previous.size() > task);

		final ReleaseStatistics curr = _current.get(task);
		final ReleaseStatistics prev = _previous.get(task);
		final boolean isSignificant = changeIsStatisticallySignificant(curr.getTimeStat("avg"), prev.getTimeStat("avg"), curr.getTimeStat("var"), prev.getTimeStat("var"), curr.getTimeStat("n"), prev.getTimeStat("n"));

		if (isSignificant)
			fail(Task.values()[task] + " Time regression (from " + prev.getTimeStat("avg") + " to " + curr.getTimeStat("avg") + "). " + _message);
	}

	private void memoryIncrease(final int task)
	{
		assumeTrue(_current.size() > task && _previous.size() > task);

		final ReleaseStatistics curr = _current.get(task);
		final ReleaseStatistics prev = _previous.get(task);
		final boolean isSignificant = changeIsStatisticallySignificant(curr.getMemStat("avg"), prev.getMemStat("avg"), curr.getMemStat("var"), prev.getMemStat("var"), curr.getMemStat("n"), prev.getMemStat("n"));

		if (isSignificant)
			fail(Task.values()[task] + " Memory regression (from " + prev.getMemStat("avg") + " to " + curr.getMemStat("avg") + "). " + _message);
	}

	private boolean changeIsStatisticallySignificant(final double m1, final double m2, @SuppressWarnings("unused") final double v1, final double v2, @SuppressWarnings("unused") final double n1, final double n2)
	{
		if (m1 > m2 && m1 > 0.01)
			try
			{
				//return math.tTest(m1, m2, v1, v2, n1, n2, ALPHA);	//2-sided, 2-sample t-test. returns true if they're different, i.e., it rejected the null hyphoteses that there is no difference between the means
				//return math.tTest(m1, m2, v1, n1, alpha);	//2-sided, 1-sample t-test. returns true if they're different, i.e., it rejected the null hyphoteses that there is no difference between the means
				//double val = 1 - (m2/m1);
				//return val > MAX_PERFORMANCE_DECREASE? true: false;

				if (m1 > math.confidenceInterval(m2, v2, n2, ALPHA)[1] * (1 + MAX_PERFORMANCE_DECREASE)) //2-sided, 1-sample t-test confidence interval. Bigger the alpha, smaller the range.
					return true;
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		return false;
	}
}
