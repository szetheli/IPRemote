/**
 * 
 */
package svenz.remote.android;

import java.util.List;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author Sven Zethelius
 *
 */
public class CompositeOnItemSelectedListener implements OnItemSelectedListener
{
	private final List<OnItemSelectedListener> m_listeners;

	public CompositeOnItemSelectedListener(List<OnItemSelectedListener> listeners)
	{
		m_listeners = listeners;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		for (OnItemSelectedListener listener : m_listeners)
			listener.onItemSelected(parent, view, position, id);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
		for (OnItemSelectedListener listener : m_listeners)
			listener.onNothingSelected(parent);
	}

}
