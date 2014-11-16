/**
 * 
 */
package svenz.remote.android;

import java.util.ArrayList;
import svenz.remote.device.ISelectable;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnLongClickListener;

/**
 * @author Sven Zethelius
 *
 */
public class SelectablePopupListener implements OnLongClickListener
{
	private int m_titleResourceId;
	private String m_key;
	private ISelectable m_selectable;
	private Context m_activity;
	private OrderedPreference m_preference;
	private final ShowOnClickListener m_callback = new ShowOnClickListener();

	public void setTitleResourceId(int titleResourceId)
	{
		m_titleResourceId = titleResourceId;
	}

	public void setActivity(Context activity)
	{
		m_activity = activity;
	}

	public void setKey(String key)
	{
		m_key = key;
	}

	public void setPreference(OrderedPreference preference)
	{
		m_preference = preference;
	}

	public void setSelectable(ISelectable selectable)
	{
		m_selectable = selectable;
	}

	@Override
	public boolean onLongClick(View v)
	{
		Builder builder = new AlertDialog.Builder(m_activity, AlertDialog.THEME_HOLO_DARK);
		OrderedSpinnerAdapter adapter = m_preference.getOrderedAdapter(
				m_activity, 
				R.layout.spinner_row, 
				m_key, 
				new ArrayList<String>(m_selectable.getOptions()));
		adapter.setDropDownViewResource(android.R.layout.select_dialog_item);
		builder.setTitle(m_titleResourceId);
		builder.setAdapter(adapter, m_callback);
		AlertDialog dialog = builder.create();
		dialog.show();
		return true;
	}

	private class ShowOnClickListener implements android.content.DialogInterface.OnClickListener
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			AlertDialog d = (AlertDialog) dialog;
			OrderedSpinnerAdapter adapter = (OrderedSpinnerAdapter) d.getListView().getAdapter();
			m_selectable.setSelection(adapter.getBaseItem(which));
		}

	}


}
