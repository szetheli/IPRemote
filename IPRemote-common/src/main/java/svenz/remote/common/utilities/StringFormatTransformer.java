/**
 * 
 */
package svenz.remote.common.utilities;

import org.apache.commons.collections.Transformer;

/**
 * @author Sven Zethelius
 *
 */
public class StringFormatTransformer implements Transformer
{
	private final String m_formatter;

	public StringFormatTransformer(String formatter)
	{
		m_formatter = formatter;
	}

	@Override
	public Object transform(Object input)
	{
		return String.format(m_formatter, input);
	}

}
