// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.profiler;

import static openllet.profiler.ProfileUtils.error;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import openllet.core.KBLoader;
import openllet.core.KnowledgeBase;
import openllet.core.utils.ATermUtils;
import openllet.core.utils.FileUtils;
import openllet.core.utils.MemUtils;
import openllet.core.utils.VersionInfo;
import openllet.jena.JenaLoader;
import openllet.owlapi.OWLAPILoader;
import openllet.profiler.utils.IObjectProfileNode;
import openllet.profiler.utils.ObjectProfiler;

/**
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 *
 * @author Evren Sirin
 */
public class ProfileKB
{
	public enum LoaderType
	{
		JENA, OWLAPI
	}

	public enum MemoryProfiling
	{
		APPROX, ALL_SIZE, ALL_VERBOSE, KB_SIZE, KB_VERBOSE, NONE
	}

	public enum Task
	{
		Parse(false), Load(false), Consistency(true), Classify(false), Realize(true);

		private boolean _requiresInstances;

		Task(final boolean requiresInstances)
		{
			_requiresInstances = requiresInstances;
		}

		boolean requiresInstances()
		{
			return _requiresInstances;
		}
	}

	public static void main(final String[] args)
	{
		new ProfileKB().run(args);
	}

	public static List<String> readConfigFile(final String configFile) throws IOException
	{
		final List<String> datasets = new ArrayList<>();

		try (final BufferedReader in = new BufferedReader(new FileReader(configFile)))
		{
			String line = null;

			while ((line = in.readLine()) != null && line.length() > 0)
				datasets.add(line);
		}

		return datasets;
	}

	private double memPercentageLimit = 0.05;

	private int iterations = 1;

	private MemoryProfiling _memoryProfiling = MemoryProfiling.APPROX;

	private Task _task = Task.Consistency;

	private LoaderType _loaderType = LoaderType.JENA;

	private boolean _imports = true;

	private final PrintWriter out = new PrintWriter(System.out);

	public ProfileKB()
	{
	}

	public void setMemoryProfiling(final MemoryProfiling memoryProfiling)
	{
		_memoryProfiling = memoryProfiling;
	}

	public void setTask(final Task task)
	{
		_task = task;
	}

	public void setLoaderType(final LoaderType loaderType)
	{
		_loaderType = loaderType;
	}

	public List<String> parseArgs(final String[] args) throws Exception
	{
		List<String> datasets = null;

		final LongOpt[] longopts = new LongOpt[9];
		longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		longopts[1] = new LongOpt("memory", LongOpt.REQUIRED_ARGUMENT, null, 'm');
		longopts[2] = new LongOpt("task", LongOpt.REQUIRED_ARGUMENT, null, 't');
		longopts[3] = new LongOpt("percentage", LongOpt.REQUIRED_ARGUMENT, null, 'p');
		longopts[4] = new LongOpt("repeat", LongOpt.REQUIRED_ARGUMENT, null, 'r');
		longopts[5] = new LongOpt("ontology", LongOpt.REQUIRED_ARGUMENT, null, 'o');
		longopts[6] = new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f');
		longopts[7] = new LongOpt("loader", LongOpt.REQUIRED_ARGUMENT, null, 'l');
		longopts[8] = new LongOpt("imports", LongOpt.REQUIRED_ARGUMENT, null, 'i');

		final Getopt g = new Getopt(ProfileKB.class.toString(), args, "hm:t:p:r:o:f:l:i:", longopts);

		try
		{
			int c;
			while ((c = g.getopt()) != -1)
				switch (c)
				{
					case 'h':
					{
						System.out.println("exist");
						System.exit(0);
					}

					//$FALL-THROUGH$
					case 'l':
						final String interfaceName = g.getOptarg().toUpperCase();
						try
						{
							_loaderType = LoaderType.valueOf(interfaceName);
						}
						catch (final IllegalArgumentException e)
						{
							error("Task " + interfaceName + " is not one of " + Arrays.toString(LoaderType.values()), e);
						}
						break;

					case 't':
						final String taskName = g.getOptarg();
						try
						{
							_task = Task.valueOf(taskName);
						}
						catch (final IllegalArgumentException e)
						{
							error("Task " + taskName + " is not one of " + Arrays.toString(Task.values()), e);
						}
						break;

					case 'p':
						memPercentageLimit = Double.parseDouble(g.getOptarg());
						break;

					case 'm':
						final String s = g.getOptarg();
						try
						{
							_memoryProfiling = MemoryProfiling.valueOf(s.toUpperCase());
						}
						catch (final IllegalArgumentException e)
						{
							error("Memory profiling " + s + " is not one of " + Arrays.toString(MemoryProfiling.values()), e);
						}
						break;

					case 'r':
						iterations = Integer.parseInt(g.getOptarg());
						break;

					case 'f':
						final String configFile = g.getOptarg();
						datasets = readConfigFile(configFile);
						break;

					case 'o':
						final String ontology = g.getOptarg();
						datasets = Arrays.asList(ontology);
						break;

					case 'i':
						_imports = Boolean.parseBoolean(g.getOptarg());
						break;

					case '?':
						error("The option '" + (char) g.getOptopt() + "' is not valid");

						//$FALL-THROUGH$
					default:
						error("Unrecognized option: " + (char) c);
				}
		}
		catch (final NumberFormatException e)
		{
			error("Invalid number: " + e);
		}

		if (datasets == null)
			error("No config file (-f) or input ontology (-o) provided!");

		return datasets;
	}

	private void print(final IObjectProfileNode node)
	{
		final StringBuilder sb = new StringBuilder();

		for (int p = 0, pLimit = node.pathlength(); p < pLimit; ++p)
			sb.append("  ");

		final IObjectProfileNode root = node.root();
		final IObjectProfileNode[] children = node.children();

		final double sizeInMB = ProfileUtils.mb(node.size());

		sb.append(String.format("%.2f", sizeInMB));
		if (node != root) // root _node is always 100% of the overall size
		{
			final double percent = (double) node.size() / root.size();

			if (percent <= memPercentageLimit)
				return;

			sb.append(" (");
			sb.append(String.format("%2.1f%%", 100 * percent));
			sb.append(")");
		}

		sb.append(" -> ");

		String name = node.name();
		final int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0)
			name = name.substring(lastDot + 1);

		sb.append(name);

		if (node.object() != null)
			if (node.name().endsWith("#table") || node.name().endsWith("#elementData"))
			{
				IObjectProfileNode shell = null;
				final int n = children.length - 1;
				for (int i = n; i >= 0; i--)
				{
					shell = children[i];
					if (shell.object() == null)
						break;
				}
				if (shell != null)
				{
					final int size = node.size() - shell.size();
					final double avg = (double) size / n;

					sb.append(" children: " + n + " avg: " + avg + " " + shell.name() + " " + ProfileUtils.mb(shell.size()));
				}
			}
			else
			{
				sb.append(" : ");
				sb.append(ObjectProfiler.typeName(node.object().getClass(), true));

				if (node.refcount() > 1) // show refcount only when it's > 1
				{
					sb.append(", refcount=");
					sb.append(node.refcount());
				}
			}

		out.println(sb);
		out.flush();

		for (final IObjectProfileNode child : children)
			print(child);
	}

	private double printProfile(final KnowledgeBase kb, final KBLoader loader, final String header)
	{
		long mem = 0;

		Object obj = loader;
		switch (_memoryProfiling)
		{
			case NONE:
				break;
			case KB_SIZE:
				obj = kb;
				//$FALL-THROUGH$
			case ALL_SIZE:
				System.out.println(header);
				mem = ObjectProfiler.sizeof(obj, ATermUtils.getFactory());
				MemUtils.printMemory("Size: ", mem);
				break;
			case KB_VERBOSE:
				obj = kb;
				//$FALL-THROUGH$
			case ALL_VERBOSE:
				System.out.println(header);
				final IObjectProfileNode profile = ObjectProfiler.profile(obj);
				print(profile);
				mem = profile.size();
				for (final IObjectProfileNode node : profile.children())
					if (node.object() != null && node.object().equals(ATermUtils.getFactory()))
					{
						mem -= node.size();
						break;
					}
				break;
			case APPROX:
				System.out.println(header);
				MemUtils.printMemory("Total: ", MemUtils.totalMemory());
				MemUtils.printMemory("Free : ", MemUtils.freeMemory());
				MemUtils.printMemory("Used*: ", MemUtils.totalMemory() - MemUtils.freeMemory());
				MemUtils.runGC();
				mem = MemUtils.usedMemory();
				MemUtils.printMemory("Used : ", mem);
				break;
		}

		System.out.println();

		return ProfileUtils.mb(mem);
	}

	public Collection<Result<Task>> profile(final String... files)
	{
		final KBLoader loader = (_loaderType == LoaderType.JENA) ? new JenaLoader() : new OWLAPILoader();

		loader.setIgnoreImports(!_imports);

		final KnowledgeBase kb = loader.getKB();

		final List<Result<Task>> results = new ArrayList<>();

		for (int i = 0; i <= _task.ordinal(); i++)
		{
			final Task task = Task.values()[i];

			final long start = System.currentTimeMillis();

			switch (task)
			{
				case Parse:
					loader.parse(FileUtils.getFileURIsFromRegex(files).toArray(new String[0]));
					break;

				case Load:
					loader.load();
					ProfileUtils.printCounts(kb);
					break;

				case Consistency:
					kb.isConsistent();
					ProfileUtils.printCounts(kb.getABox());
					break;

				case Classify:
					kb.classify();
					break;

				case Realize:
					kb.realize();
					break;

				default:
					throw new AssertionError("This task does not exist: " + task);
			}

			final double time = (System.currentTimeMillis() - start) / 1000.0;
			final double mem = task.requiresInstances() && kb.getABox().isEmpty() ? results.get(results.size() - 1).getAvgMemory() : printProfile(kb, loader, "After " + task);
			results.add(new Result<>(task, mem, time));
		}
		kb.getTimers().print();

		loader.clear();

		return results;
	}

	public void run(final String[] args)
	{
		try
		{
			final List<String> datasets = parseArgs(args);

			final int colCount = _memoryProfiling == MemoryProfiling.NONE ? 1 : 2;
			final int colWidth = 8;
			final ResultList<Task> results = new ResultList<>(colCount, colWidth);
			for (int i = 0; i < iterations; i++)
			{
				System.out.println("\n\n\nITERATION: " + (i + 1) + "\n\n\n");

				for (final String dataset : datasets)
					try
					{
						final String[] files = dataset.split(" ");
						final String name = files[0];

						final Collection<Result<Task>> currResults = profile(files);

						results.addResult(name, currResults);

						System.out.println("\n\n\nRESULT " + (i + 1) + ":");
						System.out.println("Version: " + VersionInfo.getInstance().getVersionString());
						results.print();
					}
					catch (final RuntimeException e)
					{
						e.printStackTrace();
					}

				//				MemUtils.runGC();
			}
		}
		catch (final Throwable t)
		{
			t.printStackTrace();
		}
	}
}
