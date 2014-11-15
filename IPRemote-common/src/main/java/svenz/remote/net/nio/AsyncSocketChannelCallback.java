/**
 *
 * AsyncReadCallback.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.LoggingRunnable;

/**
 * AsyncSocketChannelCallback moves the call to an executor
 * 
 * @author Sven Zethelius
 * 
 */
public class AsyncSocketChannelCallback implements ITCPSocketChannelCallback
{
	private final Executor m_executor;
	private final ITCPSocketChannelCallback m_callback;

	public AsyncSocketChannelCallback(Executor executor, ITCPSocketChannelCallback callback)
	{
		super();
		m_executor = executor;
		m_callback = callback;
	}

	@Override
	public void close() throws IOException
	{
		m_callback.close();
	}

	@Override
	public void handleRead(final byte[] b, final InetSocketAddress address) throws IOException
	{
		async(new Runnable()
			{

				@Override
				public void run()
				{
					try
					{
						m_callback.handleRead(b, address);
					}
					catch (Exception e)
					{
						LoggerFactory.getLogger(m_callback.getClass()).error("Exception handling read", e);
					}
				}
			});
	}


	@Override
	public void connectionOpen(final TCPSocketChannelInstance instance)
	{
		async(new Runnable() {

			@Override
			public void run()
			{
				m_callback.connectionOpen(instance);
			}
		});
	}

	@Override
	public void connectionClose(final TCPSocketChannelInstance instance)
	{
		async(new Runnable() {

			@Override
			public void run()
			{
				m_callback.connectionClose(instance);
			}
		});
	}

	@Override
	public void connectionFailed(final TCPSocketChannelInstance instance)
	{
		async(new Runnable() {

			@Override
			public void run()
			{
				m_callback.connectionFailed(instance);
			}
		});

	}

	private void async(Runnable r)
	{
		m_executor.execute(new LoggingRunnable(r));
	}

}
