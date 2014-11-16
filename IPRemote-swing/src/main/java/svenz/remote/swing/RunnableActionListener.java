/**
 * RunnableActionListener.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executor;
import svenz.remote.common.utilities.CallerExecutor;
import svenz.remote.common.utilities.LoggingRunnable;

/**
 * @author Sven Zethelius
 *
 */
public class RunnableActionListener implements ActionListener
{
	private final Runnable m_runnable;
	private final Executor m_executor;
	
	public RunnableActionListener(Runnable runnable)
	{
		this(runnable, CallerExecutor.INSTANCE);
	}

	public RunnableActionListener(Runnable runnable, Executor executor)
	{
		super();
		m_runnable = new LoggingRunnable(runnable);
		m_executor = executor;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		m_executor.execute(m_runnable);
	}
}
