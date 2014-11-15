/**
 * 
 */
package svenz.test.helper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.IsEqual;

/**
 * @author Sven Zethelius
 *
 */
public class PropertyMatcher<T> extends TypeSafeDiagnosingMatcher<T>
{
	private final String m_property;
	private final Method m_method;
	private final Matcher<?> m_matcher;

	public PropertyMatcher(Class<T> clazz, String name, Object item) throws IntrospectionException
	{
		this(clazz, name, IsEqual.equalTo(item));
	}

	public PropertyMatcher(Class<T> clazz, String name, Matcher<?> matcher) throws IntrospectionException
	{
		super(clazz);
		m_property = name;
		m_method = getMethod(clazz, name);
		m_matcher = matcher;
	}

	private static Method getMethod(Class<?> clazz, String name) throws IntrospectionException
	{
		BeanInfo info = Introspector.getBeanInfo(clazz);
		for (PropertyDescriptor pd : info.getPropertyDescriptors())
		{
			if (name.equals(pd.getName()))
				return pd.getReadMethod();
		}
		throw new IllegalArgumentException("Could not find property " + name + " on " + clazz);
	}

	@Override
	public void describeTo(Description description)
	{
		description.appendText(m_property + "=");
		m_matcher.describeTo(description);
	}

	@Override
	protected boolean matchesSafely(T item, Description mismatchDescription)
	{
		try
		{
			Object object = m_method.invoke(item);
			return m_matcher.matches(object);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			return false;
		}
	}

}
