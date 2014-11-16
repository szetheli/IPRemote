/**
 * 
 */
package svenz.remote.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * @author Sven Zethelius
 *
 */
public class OrderedSpinnerAdapter extends ArrayAdapter<String> implements OnItemSelectedListener
{
	private Spinner m_spinner;
	private final List<String> m_baseItems;
	private final String m_prefix;

	public OrderedSpinnerAdapter(Context context, int resource, List<String> objects, String prefix)
	{
		this(context, resource, objects, new ArrayList<String>(objects), prefix);
	}

	public OrderedSpinnerAdapter(Context context, 
			int resource, 
			List<String> objects, 
			List<String> baseItems,
			String prefix)
	{
		super(context, resource, objects);
		m_prefix = prefix;
		m_baseItems = baseItems;
	}

	public void register(Spinner spinner)
	{
		m_spinner = spinner;
		spinner.setAdapter(this);
		spinner.setOnItemSelectedListener(this);
	}

	public void registerListener(OnItemSelectedListener listener)
	{
		m_spinner.setOnItemSelectedListener(new CompositeOnItemSelectedListener(Arrays.asList(
				m_spinner.getOnItemSelectedListener(), listener)));
	}


	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		if (position == 0)
			return;
		synchronized (this)
		{
			String selected = super.getItem(position);
			super.setNotifyOnChange(false);
			super.remove(selected);
			super.insert(selected, 0);
			m_baseItems.remove(selected);
			m_baseItems.add(0, selected);
			super.notifyDataSetChanged();
		}
		m_spinner.setSelection(0);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
	}

	@Override
	public String getItem(int position)
	{
		Resources r = getContext().getResources();
		String item = super.getItem(position);
		// TODO cache the resource lookup
		int id = r.getIdentifier(m_prefix + item.replaceAll("\\.", "_"), "string", getClass().getPackage().getName());
		if (id == 0)
			throw new IllegalStateException("Unknown id for " + item);
		return r.getString(id);
	}

	public String getBaseSelectedItem()
	{
		return super.getItem(m_spinner.getSelectedItemPosition());
	}

	public String getBaseItem(int position)
	{
		return m_baseItems.get(position);
	}

	public List<String> getBaseItems()
	{
		synchronized (this)
		{
			return new ArrayList<String>(m_baseItems);
		}
	}

}
