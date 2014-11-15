/**
 * LoggingRunnable.java
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
package svenz.remote.common.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log any exceptions from runnable, using the runnable class's {@link Logger}
 * 
 * @author Sven Zethelius
 * 
 */
public class LoggingRunnable implements Runnable
{
	private final Runnable m_runnable;

	public LoggingRunnable(Runnable runnable)
	{
		super();
		m_runnable = runnable;
	}

	@Override
	public void run()
	{
		try
		{
			m_runnable.run();
		}
		catch (Exception e)
		{
			LoggerFactory.getLogger(Utilities.getClassForLogging(m_runnable)).error("Exception during run", e);
		}
		catch (Error e)
		{
			LoggerFactory.getLogger(Utilities.getClassForLogging(m_runnable)).error("Error during run", e);
			throw e;
		}
	}


}
