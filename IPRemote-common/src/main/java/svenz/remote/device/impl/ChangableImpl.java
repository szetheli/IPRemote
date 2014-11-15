/**
 * ChangableImpl.java
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
package svenz.remote.device.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import svenz.remote.device.IChangable;

/**
 * @author Sven Zethelius
 *
 */
public class ChangableImpl<T> implements IChangable
{
	private transient final List<IChangeListener> m_listeners = new ArrayList<IChangeListener>(1);
	private final Object m_target;
	private final String m_property;
	private transient final AtomicReference<T> m_status = new AtomicReference<T>();

	public ChangableImpl(Object target, String property)
	{
		m_target = target;
		m_property = property;
	}

	@Override
	public void addChangeListener(IChangeListener listener)
	{
		synchronized (m_listeners)
		{
			m_listeners.add(listener);
		}
	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		synchronized (m_listeners)
		{
			m_listeners.remove(listener);
		}
	}

	public void notify(String parameter)
	{
		synchronized (m_listeners)
		{
			for (IChangeListener listener : m_listeners)
				listener.stateChanged(m_target, parameter);
		}
	}

	public T getStatus()
	{
		return m_status.get();
	}

	public T setStatus(T t)
	{
		T tOld = m_status.getAndSet(t);
		if ((t == null ^ tOld == null) || (t != null && !t.equals(tOld)))
		{
			notify(m_property);
		}
		return tOld;
	}

}
