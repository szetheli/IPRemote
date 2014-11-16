/**
 * 
 */
package svenz.remote.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.DataSetObserver;

/**
 * @author Sven Zethelius
 *
 */
public class OrderedPreference
{
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderedPreference.class);
	private Executor m_executor;
	private SharedPreferences m_preferences;

	public void setExecutor(Executor executor)
	{
		m_executor = executor;
	}

	public void setPreferences(SharedPreferences preferences)
	{
		m_preferences = preferences;
	}

	private List<String> getOrderedListFromPreferences(String key)
	{
		List<String> order = new ArrayList<String>();
		String string = m_preferences.getString(key, null);
		if (string != null)
		{
			for (String selection : string.split(","))
				order.add(selection);
		}
		return order;
	}

	public OrderedSpinnerAdapter getOrderedAdapter(
			Context context,
			int resourceId,
			String prefix)
	{
		String key = prefix + "list";
		List<String> order = getOrderedListFromPreferences(key);
		if(order.isEmpty())
			return null;

		return createOrderedAdapter(context, resourceId, prefix, order);
	}
	
	public OrderedSpinnerAdapter getOrderedAdapter(
			Context context, 
			int resourceId, 
			String prefix,
			List<String> selections)
	{
		String key = prefix + "list";
		List<String> order = getOrderedListFromPreferences(key);

		selections = Utilities.mergeOrderedLists(selections, order);
		
		return createOrderedAdapter(context, resourceId, prefix, selections);
	}
	
	private OrderedSpinnerAdapter createOrderedAdapter(
			Context context, 
			int resourceId, 
			String prefix,
			List<String> selections)
	{
		OrderedSpinnerAdapter adapter = new OrderedSpinnerAdapter(context, resourceId, selections, prefix);
		adapter.registerDataSetObserver(new UpdateDataSetObserverable(adapter, prefix + "list"));
		return adapter;

	}

	private void putString(String key, String value)
	{
		Editor editor = m_preferences.edit();
		editor.putString(key, value);
		editor.commit();
		LOGGER.trace("Order updated {}:{}", key, value);

	}


	private class UpdateDataSetObserverable extends DataSetObserver implements Runnable
	{
		private final OrderedSpinnerAdapter m_adapter;
		private final String m_key;

		public UpdateDataSetObserverable(OrderedSpinnerAdapter adapter, String key)
		{
			super();
			m_adapter = adapter;
			m_key = key;
		}

		@Override
		public void onChanged()
		{
			m_executor.execute(this);
		}

		@Override
		public void run()
		{
			List<String> list = m_adapter.getBaseItems();
			StringBuilder sb = new StringBuilder(list.size() * 10);
			for (String item : list)
				sb.append(',').append(item);
			putString(m_key, sb.substring(1));
		}


	}

}
