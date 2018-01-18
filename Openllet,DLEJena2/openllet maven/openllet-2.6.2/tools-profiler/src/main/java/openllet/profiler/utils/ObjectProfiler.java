package openllet.profiler.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import openllet.atom.OpenError;

// ----------------------------------------------------------------------------
/**
 * This non-instantiable class presents an API for object sizing and profiling as described in the article. See _individual methods for details.
 * <P>
 * You would need to code your own identity hashmap to port this to earlier Java versions.
 * <P>
 * Security: this implementation uses AccessController.doPrivileged() so it could be granted privileges to access non-public class fields separately from your
 * main application code. The minimum set of persmissions necessary for this class to function correctly follows:
 *
 * <pre>
 *      permission java.lang.RuntimePermission "accessDeclaredMembers";
 *      permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
 * </pre>
 *
 * @see IObjectProfileNode
 * @author (C) <a href="http://www.javaworld.com/columns/jw-qna-_index.shtml">Vlad Roubtsov</a>, 2003
 */
public abstract class ObjectProfiler
{
	public static final String INPUT_OBJECT_NAME = "<INPUT>"; // root _node name

	// the following constants are physical sizes (in bytes) and are JVM-dependent:
	// [the _current values are Ok for most 32-bit JVMs]

	public static final int OBJECT_SHELL_SIZE = 8; // java.lang.Object shell size in bytes
	public static final int OBJREF_SIZE = 4;

	public static final int LONG_FIELD_SIZE = 8;
	public static final int INT_FIELD_SIZE = 4;
	public static final int SHORT_FIELD_SIZE = 2;
	public static final int CHAR_FIELD_SIZE = 2;
	public static final int BYTE_FIELD_SIZE = 1;
	public static final int BOOLEAN_FIELD_SIZE = 1;
	public static final int DOUBLE_FIELD_SIZE = 8;
	public static final int FLOAT_FIELD_SIZE = 4;

	// class metadata _cache:
	private static final Map<Class<?>, ClassMetadata> CLASS_METADATA_CACHE = new WeakHashMap<>(101);

	/**
	 * Set this to 'true' to make the _node names default to using class names without package prefixing for more compact dumps
	 */
	public static final boolean SHORT_TYPE_NAMES = false;

	/**
	 * If this is 'true', node names will use short class names for common classes in java.lang.* and java.util.*, regardless of {@link #SHORT_TYPE_NAMES}
	 * setting.
	 */
	public static final boolean SHORT_COMMON_TYPE_NAMES = true;

	/**
	 * Estimates the full size of the object graph rooted at 'obj'. Duplicate _data instances are correctly accounted for. The implementation is not recursive.
	 * <P>
	 * Invariant: sizeof(obj) == profile(obj).size() if 'obj' is not null
	 *
	 * @param obj input object instance to be measured
	 * @return 'obj' size [0 if 'obj' is null']
	 */
	public static int sizeof(final Object obj)
	{
		if (obj == null)
			return 0;

		final IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

		return computeSizeof(obj, visited, CLASS_METADATA_CACHE);
	}

	public static int sizeof(final Object obj, final Object... avoid)
	{
		if (obj == null)
			return 0;

		final IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

		for (final Object o : avoid)
			visited.put(o, o);

		return computeSizeof(obj, visited, CLASS_METADATA_CACHE);
	}

	/**
	 * Estimates the full size of the object graph rooted at 'obj' by pre-populating the "visited" set with the object graph rooted at 'base'. The net effect is
	 * to compute the size of 'obj' by summing over all instance _data contained in 'obj' but not in 'base'.
	 *
	 * @param base graph boundary [may not be null]
	 * @param obj input object instance to be measured
	 * @return 'obj' size [0 if 'obj' is null']
	 */
	public static int sizedelta(final Object base, final Object obj)
	{
		if (obj == null)
			return 0;
		if (base == null)
			throw new IllegalArgumentException("null input: base");

		final IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

		computeSizeof(base, visited, CLASS_METADATA_CACHE);
		return visited.containsKey(obj) ? 0 : computeSizeof(obj, visited, CLASS_METADATA_CACHE);
	}

	/**
	 * Creates a spanning tree representation for instance _data contained in 'obj'. The tree is produced using bread-first traversal over the full object graph
	 * implied by non-null instance and array references originating in 'obj'.
	 *
	 * @see IObjectProfileNode
	 * @param obj input object instance to be profiled [may not be null]
	 * @return the profile tree root _node [never null]
	 */
	public static IObjectProfileNode profile(final Object obj)
	{
		if (obj == null)
			throw new IllegalArgumentException("null input: obj");

		final IdentityHashMap<Object, ObjectProfileNode> visited = new IdentityHashMap<>();

		final ObjectProfileNode root = createProfileTree(obj, visited, CLASS_METADATA_CACHE);
		finishProfileTree(root);

		return root;
	}

	// convenience methods:

	public static String pathName(final IObjectProfileNode[] path)
	{
		final StringBuffer s = new StringBuffer();

		for (int i = 0; i < path.length; ++i)
		{
			if (i != 0)
				s.append('/');
			s.append(path[i].name());
		}

		return s.toString();
	}

	public static String fieldName(final Field field, final boolean shortClassNames)
	{
		return typeName(field.getDeclaringClass(), shortClassNames).concat("#").concat(field.getName());
	}

	public static String typeName(final Class<?> clsParam, final boolean shortClassNames)
	{
		Class<?> cls = clsParam;
		int dims = 0;
		for (; cls.isArray(); ++dims)
			cls = cls.getComponentType();

		String clsName = cls.getName();

		if (shortClassNames)
		{
			final int lastDot = clsName.lastIndexOf('.');
			if (lastDot >= 0)
				clsName = clsName.substring(lastDot + 1);
		}
		else
			if (SHORT_COMMON_TYPE_NAMES)
				if (clsName.startsWith("java.lang."))
					clsName = clsName.substring(10);
				else
					if (clsName.startsWith("java.util."))
						clsName = clsName.substring(10);

		for (int i = 0; i < dims; ++i)
			clsName = clsName.concat("[]");

		return clsName;
	}

	/**
	 * Internal class used to _cache class metadata information.
	 */
	private static final class ClassMetadata
	{
		public final int _primitiveFieldCount;
		public final int _shellSize; // class shell size
		public final Field[] _refFields; // cached non-static fields (made accessible)

		public ClassMetadata(final int primitiveFieldCount, final int shellSize, final Field[] refFields)
		{
			_primitiveFieldCount = primitiveFieldCount;
			_shellSize = shellSize;
			_refFields = refFields;
		}
	}

	private static final class ClassAccessPrivilegedAction implements PrivilegedExceptionAction<Object>
	{
		@Override
		public Object run() throws Exception
		{
			return _cls.getDeclaredFields();
		}

		public void setContext(final Class<?> cls)
		{
			_cls = cls;
		}

		private Class<?> _cls;

	}

	private static final class FieldAccessPrivilegedAction implements PrivilegedExceptionAction<Object>
	{
		@Override
		public Object run() throws Exception
		{
			_field.setAccessible(true);

			return null;
		}

		public void setContext(final Field field)
		{
			_field = field;
		}

		private Field _field;

	}

	private ObjectProfiler()
	{
		// prevent instance.
	}

	/*
	 * The main worker method for sizeof() and sizedelta().
	 */
	private static int computeSizeof(final Object objParam, final IdentityHashMap<Object, Object> visited, final Map /* Class->ClassMetadata */<Class<?>, ClassMetadata> metadataMap)
	{
		Object obj = objParam;
		// this uses _depth-first traversal; the exact graph traversal algorithm
		// does not matter for computing the total size and this method could be
		// easily adjusted to do breadth-first instead (addLast() instead of addFirst()),
		// however, dfs/bfs require max _queue length to be the length of the longest
		// graph path/width of traversal front correspondingly, so I expect
		// dfs to use fewer resources than bfs for most Java objects;

		if (obj == null)
			return 0;

		final LinkedList<Object> queue = new LinkedList<>();

		visited.put(obj, obj);
		queue.add(obj);

		int result = 0;

		final ClassAccessPrivilegedAction caAction = new ClassAccessPrivilegedAction();
		final FieldAccessPrivilegedAction faAction = new FieldAccessPrivilegedAction();

		while (!queue.isEmpty())
		{
			obj = queue.removeFirst();
			final Class<?> objClass = obj.getClass();

			if (objClass.isArray())
			{
				final int arrayLength = Array.getLength(obj);
				final Class<?> componentType = objClass.getComponentType();

				result += sizeofArrayShell(arrayLength, componentType);

				if (!componentType.isPrimitive())
					// traverse each array slot:
					for (int i = 0; i < arrayLength; ++i)
					{
					final Object ref = Array.get(obj, i);

					if ((ref != null) && !visited.containsKey(ref))
					{
					visited.put(ref, ref);
					queue.addFirst(ref);
					}
					}
			}
			else
			// the object is of a non-array type
			{
				final ClassMetadata metadata = getClassMetadata(objClass, metadataMap, caAction, faAction);
				final Field[] fields = metadata._refFields;

				result += metadata._shellSize;

				// traverse all non-null ref fields:

				for (final Field field : fields)
				{
					final Object ref;
					try
					// to get the field value:
					{
						ref = field.get(obj);
					}
					catch (final Exception e)
					{
						throw new OpenError("cannot get field [" + field.getName() + "] of class [" + field.getDeclaringClass().getName() + "]: " + e.toString());
					}

					if ((ref != null) && !visited.containsKey(ref))
					{
						visited.put(ref, ref);
						queue.addFirst(ref);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Performs phase 1 of profile creation: bread-first traversal and node creation.
	 */
	private static ObjectProfileNode createProfileTree(final Object objParam, final IdentityHashMap<Object, ObjectProfileNode> visited, final Map /* Class->ClassMetadata */<Class<?>, ClassMetadata> metadataMap)
	{
		Object obj = objParam;

		final ObjectProfileNode root = new ObjectProfileNode(null, obj, null);

		final LinkedList<ObjectProfileNode> queue = new LinkedList<>();

		queue.addFirst(root);
		visited.put(obj, root);

		final ClassAccessPrivilegedAction caAction = new ClassAccessPrivilegedAction();
		final FieldAccessPrivilegedAction faAction = new FieldAccessPrivilegedAction();

		while (!queue.isEmpty())
		{
			final ObjectProfileNode node = queue.removeFirst();

			obj = node._obj;
			final Class<?> objClass = obj.getClass();

			if (objClass.isArray())
			{
				final int arrayLength = Array.getLength(obj);
				final Class<?> componentType = objClass.getComponentType();

				// add shell pseudo-_node:
				final AbstractShellProfileNode shell = new ArrayShellProfileNode(node, objClass, arrayLength);
				shell._size = sizeofArrayShell(arrayLength, componentType);

				node._shell = shell;
				node.addFieldRef(shell);

				if (!componentType.isPrimitive())
					// traverse each array slot:
					for (int i = 0; i < arrayLength; ++i)
					{
					final Object ref = Array.get(obj, i);

					if (ref != null)
					{
					ObjectProfileNode child = visited.get(ref);
					if (child != null)
					++child._refcount;
					else
					{
					child = new ObjectProfileNode(node, ref, new ArrayIndexLink(node._link, i));
					node.addFieldRef(child);

					queue.addLast(child);
					visited.put(ref, child);
					}
					}
					}
			}
			else
			// the object is of a non-array type
			{
				final ClassMetadata metadata = getClassMetadata(objClass, metadataMap, caAction, faAction);
				final Field[] fields = metadata._refFields;

				// add shell pseudo-_node:
				final AbstractShellProfileNode shell = new ObjectShellProfileNode(node, metadata._primitiveFieldCount, metadata._refFields.length);
				shell._size = metadata._shellSize;

				node._shell = shell;
				node.addFieldRef(shell);

				// traverse all non-null ref fields:
				for (final Field field : fields)
				{
					final Object ref;
					try
					// to get the field value:
					{
						ref = field.get(obj);
					}
					catch (final Exception e)
					{
						throw new OpenError("cannot get field [" + field.getName() + "] of class [" + field.getDeclaringClass().getName() + "]: " + e.toString());
					}

					if (ref != null)
					{
						ObjectProfileNode child = visited.get(ref);
						if (child != null)
							++child._refcount;
						else
						{
							child = new ObjectProfileNode(node, ref, new ClassFieldLink(field));
							node.addFieldRef(child);

							queue.addLast(child);
							visited.put(ref, child);
						}
					}
				}
			}
		}

		return root;
	}

	/**
	 * Performs phase 2 of profile creation: totalling of _node sizes (via non-recursive post-order traversal of the tree created in phase 1) and 'locking down'
	 * of profile nodes into their most compact form.
	 */
	private static void finishProfileTree(final ObjectProfileNode nodeParam)
	{
		ObjectProfileNode node = nodeParam;
		final LinkedList<IObjectProfileNode> queue = new LinkedList<>();
		IObjectProfileNode lastFinished = null;

		while (node != null)
		{
			// note that an unfinished non-shell _node has its child count
			// in m_size and m_children[0] is its shell _node:

			if ((node._size == 1) || (lastFinished == node._children[1]))
			{
				node.finish();
				lastFinished = node;
			}
			else
			{
				queue.addFirst(node);
				for (int i = 1; i < node._size; ++i)
				{
					final IObjectProfileNode child = node._children[i];
					queue.addFirst(child);
				}
			}

			if (queue.isEmpty())
				return;
			else
				node = (ObjectProfileNode) queue.removeFirst();
		}
	}

	/**
	 * A helper method for manipulating a class metadata cache.
	 */
	private static ClassMetadata getClassMetadata(final Class<?> cls, final Map /* Class->ClassMetadata */<Class<?>, ClassMetadata> metadataMap, final ClassAccessPrivilegedAction caAction, final FieldAccessPrivilegedAction faAction)
	{
		if (cls == null)
			return null;

		ClassMetadata result;
		synchronized (metadataMap)
		{
			result = metadataMap.get(cls);
		}
		if (result != null)
			return result;

		int primitiveFieldCount = 0;
		int shellSize = OBJECT_SHELL_SIZE; // java.lang.Object shell
		final List /* Field */<Field> refFields = new LinkedList<>();

		final Field[] declaredFields;
		try
		{
			caAction.setContext(cls);
			declaredFields = (Field[]) AccessController.doPrivileged(caAction);
		}
		catch (final PrivilegedActionException pae)
		{
			throw new OpenError("could not access declared fields of class " + cls.getName() + ": " + pae.getException());
		}

		for (final Field declaredField : declaredFields)
		{
			final Field field = declaredField;
			if ((Modifier.STATIC & field.getModifiers()) != 0)
				continue;

			final Class<?> fieldType = field.getType();
			if (fieldType.isPrimitive())
			{
				// memory alignment ignored:
				shellSize += sizeofPrimitiveType(fieldType);
				++primitiveFieldCount;
			}
			else
			{
				// prepare for graph traversal later:
				if (!field.isAccessible())
					try
					{
						faAction.setContext(field);
						AccessController.doPrivileged(faAction);
					}
					catch (final PrivilegedActionException pae)
					{
						throw new OpenError("could not make field " + field + " accessible: " + pae.getException());
					}

				// memory alignment ignored:
				shellSize += OBJREF_SIZE;
				refFields.add(field);
			}
		}

		// recurse into superclass:
		final ClassMetadata superMetadata = getClassMetadata(cls.getSuperclass(), metadataMap, caAction, faAction);
		if (superMetadata != null)
		{
			primitiveFieldCount += superMetadata._primitiveFieldCount;
			shellSize += superMetadata._shellSize - OBJECT_SHELL_SIZE;
			refFields.addAll(Arrays.asList(superMetadata._refFields));
		}

		final Field[] _refFields = new Field[refFields.size()];
		refFields.toArray(_refFields);

		result = new ClassMetadata(primitiveFieldCount, shellSize, _refFields);
		synchronized (metadataMap)
		{
			metadataMap.put(cls, result);
		}

		return result;
	}

	/**
	 * Computes the "shallow" size of an array instance.
	 */
	private static int sizeofArrayShell(final int length, final Class<?> componentType)
	{
		// this ignores memory alignment issues by design:

		final int slotSize = componentType.isPrimitive() ? sizeofPrimitiveType(componentType) : OBJREF_SIZE;

		return OBJECT_SHELL_SIZE + INT_FIELD_SIZE + OBJREF_SIZE + length * slotSize;
	}

	/**
	 * Returns the JVM-specific size of a primitive type.
	 */
	private static int sizeofPrimitiveType(final Class<?> type)
	{
		if (type == int.class)
			return INT_FIELD_SIZE;
		else
			if (type == long.class)
				return LONG_FIELD_SIZE;
			else
				if (type == short.class)
					return SHORT_FIELD_SIZE;
				else
					if (type == byte.class)
						return BYTE_FIELD_SIZE;
					else
						if (type == boolean.class)
							return BOOLEAN_FIELD_SIZE;
						else
							if (type == char.class)
								return CHAR_FIELD_SIZE;
							else
								if (type == double.class)
									return DOUBLE_FIELD_SIZE;
								else
									if (type == float.class)
										return FLOAT_FIELD_SIZE;
									else
										throw new IllegalArgumentException("not primitive: " + type);
	}

}
