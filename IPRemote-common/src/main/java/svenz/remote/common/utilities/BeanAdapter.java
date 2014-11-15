/**
 * 
 */
package svenz.remote.common.utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sven Zethelius
 *
 */
public class BeanAdapter
{
	private final Map<List<String>, Object> m_cache = new HashMap<List<String>, Object>();
	private final Map<Class<?>, Map<String, Method>> m_propertyDescriptors =
			new HashMap<Class<?>, Map<String, Method>>();

	public void addRoot(String name, Object o)
	{
		m_cache.put(Collections.singletonList(name), o);
	}

	public Object resolve(List<String> path) throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException
	{
		int idx = path.size();
		Object o = null;
		if (idx > 1) // guard in case resolve is for a root
			idx--;
		for (; idx > 0 && o == null; idx--)
			o = m_cache.get(path.subList(0, idx));
		if (o == null)
			throw new IllegalArgumentException("No root found for " + path);
		for (idx++; idx < path.size(); idx++)
		{
			Class<? extends Object> clazz = o.getClass();
			Method desc = getReadMethod(clazz, path.get(idx));
			if (desc == null)
				throw new IllegalArgumentException("Property not found for " + path.subList(0, idx + 1));
			o = desc.invoke(o);
			if (o == null)
				throw new IllegalArgumentException("No value found for " + path.subList(0, idx + 1));
			m_cache.put(path.subList(0, idx + 1), o);
		}
		return o;
	}

	private Method getReadMethod(Class<?> clazz, String property) throws NoSuchMethodException, SecurityException
	{
		Map<String, Method> methods = m_propertyDescriptors.get(clazz);
		if (methods == null)
			m_propertyDescriptors.put(clazz, methods = new HashMap<String, Method>());
		Method method = methods.get(property);
		if (method != null)
			return method;
		String propertyName = property.substring(0, 1).toUpperCase() + property.substring(1);
		try
		{
			method = clazz.getMethod("get" + propertyName);
		}
		catch (NoSuchMethodException e)
		{ // expected if boolean
			method = clazz.getMethod("is" + propertyName);
		}
		methods.put(property, method);
		return method;
	}

}
