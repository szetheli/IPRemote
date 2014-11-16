/**
 * ListOrderListener.java
 * Copyright 2013, Sven Zethelius
 * 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package svenz.remote.swing;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 * @author Sven Zethelius
 * 
 */
public class ListOrderListener implements ActionListener
{
	private final List<String> m_order;

	public ListOrderListener(List<String> order)
	{
		super();
		m_order = order;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e)
	{
		final ComboBoxModel<String> cb = ((JComboBox<String>) e.getSource()).getModel();
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run()
			{
				for(int i=0, c=cb.getSize(); i < c; i++)
				{
					Object o = cb.getElementAt(i);
					if (!o.equals(m_order.get(i)))
					{
						m_order.add(i, (String) o);
					}
				}
			}
		});
	}

}
