// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package openllet.test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBaseImpl;
import openllet.core.boxes.abox.Individual;
import openllet.core.boxes.rbox.Role;
import openllet.core.utils.ATermUtils;

public class TestUtils
{
	public static Random _rand = new Random(System.currentTimeMillis());

	public static ATermAppl selectRandomConcept(final Individual ind)
	{
		final ATermAppl[] types = ind.types()//
				.filter(clazz -> !((clazz == ATermUtils.TOP) || (clazz == ATermUtils.BOTTOM)))//
				.toArray(ATermAppl[]::new);
		ATermAppl clazz = null;
		final int MAX = 20;
		int count = 0;
		if (types.length > 0)
			do
			{
				count++;
				clazz = types[_rand.nextInt(types.length)];
			} while (count < MAX);

		return clazz;
	}

	public static ATermAppl selectRandomConcept(final KnowledgeBaseImpl kb)
	{
		final ATermAppl[] classes = kb.getTBox().allClasses().toArray(ATermAppl[]::new);
		ATermAppl clazz = null;
		do
		{
			clazz = classes[_rand.nextInt(classes.length)];
		} while ((clazz == ATermUtils.TOP) || (clazz == ATermUtils.BOTTOM));

		return clazz;
	}

	public static ATermAppl selectRandomObjectProperty(final KnowledgeBaseImpl kb)
	{

		//get all classes
		final List<Role> roles = new ArrayList<>(kb.getRBox().getRoles().values());
		Role role = null;
		do
		{
			role = roles.get(_rand.nextInt(roles.size()));
		} while (!role.isObjectRole());

		return role.getName();
	}

	public static ATermAppl selectRandomIndividual(final KnowledgeBaseImpl kb)
	{
		final ATermAppl[] inds = kb.individuals().toArray(ATermAppl[]::new);
		return inds[_rand.nextInt(inds.length)];
	}
}
