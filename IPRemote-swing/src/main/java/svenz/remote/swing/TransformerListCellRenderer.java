/**
 * ResourceListCellRenderer.java
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

import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.apache.commons.collections.Transformer;

/**
 * @author Sven Zethelius
 *
 */
public class TransformerListCellRenderer implements ListCellRenderer<String>
{
	private final ListCellRenderer<? super String> m_renderer;
	private final Transformer m_resources;

	public TransformerListCellRenderer(ListCellRenderer<? super String> renderer, Transformer resources)
	{
		m_renderer = renderer;
		m_resources = resources;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
			boolean isSelected, boolean cellHasFocus)
	{
		value = (String) m_resources.transform(value);
		return m_renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}

}
