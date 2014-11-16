/**
 * GridBagConstraintBuilder.java
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * @author Sven Zethelius
 *
 */
public class GridBagConstraintsBuilder extends GridBagConstraints
{
	private static final long serialVersionUID = 1L;

	private final int[] m_rowHeights;
	private final int[] m_colWidths;

	public GridBagConstraintsBuilder(GridBagLayout layout)
	{
		this.m_rowHeights = layout.rowHeights;
		this.m_colWidths = layout.columnWidths;
		this.gridx = 0;
		this.gridy = 0;

		if (layout.rowHeights == null || layout.rowWeights == null
				|| layout.rowHeights.length != layout.rowWeights.length
				|| layout.columnWidths == null || layout.columnWeights == null
				|| layout.columnWidths.length != layout.columnWeights.length)
			throw new IllegalArgumentException("Layout dimensions not initialized");
	}

	@Override
	public GridBagConstraintsBuilder clone()
	{
		GridBagConstraintsBuilder clone = (GridBagConstraintsBuilder) super.clone();
		return clone;
	}

	public GridBagConstraintsBuilder anchor(int anchor)
	{
		this.anchor = anchor;
		return this;
	}

	public GridBagConstraintsBuilder n()
	{
		return anchor(NORTH);
	}

	public GridBagConstraintsBuilder ne()
	{
		return anchor(NORTHEAST);
	}

	public GridBagConstraintsBuilder nw()
	{
		return anchor(NORTHWEST);
	}

	public GridBagConstraintsBuilder w()
	{
		return anchor(WEST);
	}

	public GridBagConstraintsBuilder e()
	{
		return anchor(EAST);
	}

	public GridBagConstraintsBuilder se()
	{
		return anchor(SOUTHEAST);
	}

	public GridBagConstraintsBuilder s()
	{
		return anchor(SOUTH);
	}

	public GridBagConstraintsBuilder sw()
	{
		return anchor(SOUTHWEST);
	}

	public GridBagConstraintsBuilder c()
	{
		return anchor(CENTER);
	}

	public GridBagConstraintsBuilder fill(int fill)
	{
		this.fill = fill;
		return this;
	}

	public GridBagConstraintsBuilder both()
	{
		return fill(BOTH);
	}

	public GridBagConstraintsBuilder horiz()
	{
		return fill(HORIZONTAL);
	}

	public GridBagConstraintsBuilder vert()
	{
		return fill(VERTICAL);
	}

	private void check(int actual, int expected, String message)
	{
		if (actual >= expected)
			throw new IllegalStateException("Exceeded " + message);
	}

	public GridBagConstraintsBuilder height(int y)
	{
		check(y + this.gridy, m_rowHeights.length, "height");
		this.gridheight = y;
		return this;
	}

	public GridBagConstraintsBuilder width(int x)
	{
		check(x + this.gridx - 1, m_colWidths.length, "width");
		this.gridwidth = x;
		return this;
	}

	public GridBagConstraintsBuilder nextCol()
	{
		this.gridx += this.gridwidth;
		this.gridwidth = 1;
		check(this.gridx, m_colWidths.length, "width");
		return this;
	}

	public GridBagConstraintsBuilder nextRow()
	{
		this.gridy++;
		this.gridheight = 1;
		this.gridx = 0;
		this.gridwidth = 1;
		check(this.gridy, m_rowHeights.length, "height");
		return this;
	}

	public GridBagConstraintsBuilder inset(int top, int left, int bottom, int right)
	{
		this.insets = new Insets(top, left, bottom, right);
		return this;
	}

	public GridBagConstraintsBuilder inset(int px)
	{
		return inset(px, px, px, px);
	}

	public GridBagConstraintsBuilder pad(int x, int y)
	{
		this.ipadx = x;
		this.ipady = x;
		return this;
	}

	public GridBagConstraintsBuilder weight(double x, double y)
	{
		this.weightx = x;
		this.weighty = y;
		return this;
	}
}

