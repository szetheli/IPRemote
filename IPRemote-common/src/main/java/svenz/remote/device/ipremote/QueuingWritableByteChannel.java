/**
 * 
 */
package svenz.remote.device.ipremote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.LoggingRunnable;

/**
 * @author Sven Zethelius
 *
 */
public class QueuingWritableByteChannel implements WritableByteChannel
{
	private static final Logger LOGGER = LoggerFactory.getLogger(QueuingWritableByteChannel.class);

	private WritableByteChannel m_channel;
	private final Queue<Request> m_queue = new ConcurrentLinkedQueue<Request>();
	private Request m_awaiting;
	private ScheduledExecutorService m_executor;
	private long m_retryDelayMS = -1;
	
	public void setExecutor(ScheduledExecutorService executor)
	{
		m_executor = executor;
	}
	
	public void setChannel(WritableByteChannel channel)
	{
		if (m_channel == channel)
			return;
		synchronized (m_queue)
		{
			if (!m_queue.isEmpty())
				LOGGER.warn("Channel reset.  The following commands to {} lost:{}", m_channel, m_queue);
			m_queue.clear(); // delete any previous writes that may have been queued
			m_channel = channel;
			m_awaiting = null;
		}
	}

	public void setRetryDelayMS(long retryDelayMS)
	{
		m_retryDelayMS = retryDelayMS;
	}

	public void sendNext() throws IOException
	{
		synchronized (m_queue)
		{
			if (m_channel == null || m_awaiting != null)
				return;

			Request r = m_queue.peek();
			if (r != null)
			{
				m_awaiting = r;
				try
				{
					m_channel.write(ByteBuffer.wrap(r.getRequestBytes()));
				}
				catch (IOException e)
				{
					acknowledgeResponseFailed();
					throw e;
				}
			}
		}
	}

	private void retry()
	{
		synchronized (m_queue)
		{
			m_awaiting = null;
			try
			{
				sendNext();
			}
			catch (IOException e)
			{
				LOGGER.error("Exception attempting to retry {}", m_queue, e);
				try
				{
					acknowledgeResponseFailed();
				}
				catch (IOException e2)
				{// shouldn't happen since we are retrying
					throw new IllegalStateException("Unexpected exception", e2);
				}
			}
		}
	}

	private void acknowledgeResponseFailed() throws IOException
	{
		long retryDelayMS = m_awaiting.getRetryDelayMS();
		if (retryDelayMS > 0)
		{
			m_executor.schedule(new LoggingRunnable(new Runnable() {

				@Override
				public void run()
				{
					retry();
				}
			}), retryDelayMS, TimeUnit.MILLISECONDS);
		}
		else
		{
			m_awaiting = null;
		}
	}

	public void acknowledgeResponse(boolean success) throws IOException
	{
		synchronized (m_queue)
		{
			if (success)
			{
				if (m_queue.peek() == m_awaiting)
					m_queue.remove();
				m_awaiting = null;
			}
			else
			{
				Request r = m_awaiting;
				acknowledgeResponseFailed();
				if (m_awaiting == null)
				{
					if (r == m_queue.peek())
						m_queue.remove();
				}
			}
			sendNext();
		}
	}

	public byte[] getOutstandingRequest()
	{
		// we're processing a response
		Request r = m_queue.peek();
		return r == null ? null : r.getRequestBytes();
	}

	@Override
	public int write(ByteBuffer src) throws IOException
	{
		int consumed = src.remaining();
		byte[] b = new byte[consumed];
		src.get(b);
		m_queue.add(new Request(b, m_retryDelayMS));
		sendNext();
		return consumed;
	}

	@Override
	public boolean isOpen()
	{
		return m_channel.isOpen();
	}

	@Override
	public void close() throws IOException
	{
		m_channel.close();
		setChannel(null);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(m_channel);
		if (!m_queue.isEmpty())
		{
			sb.append(" Pending:");
			for (Request r : m_queue)
				sb.append(r).append(',');
			sb.replace(sb.length() - 1, sb.length(), "");
		}
		return sb.toString();
	}

	/**
	 * Clear any outstanding requests that haven't been sent.
	 */
	public void clearPending()
	{
		synchronized (m_queue)
		{
			// clear all but the "must have" messages
			for (Iterator<Request> iter = m_queue.iterator(); iter.hasNext();)
			{
				Request r = iter.next();
				if (r.getRetryDelayMS() < 0)
					iter.remove();
			}
		}
	}

	/**
	 * Clear a specific code from queue
	 * 
	 * @param code
	 */
	public void clearPending(String code)
	{
		byte[] bCode = code.getBytes();
		synchronized (m_queue)
		{
			for (Iterator<Request> iter = m_queue.iterator(); iter.hasNext();)
			{
				Request r = iter.next();
				if (r.getRetryDelayMS() < 0 && Arrays.equals(r.getRequestBytes(), bCode))
					iter.remove();
			}
		}
	}

	private class Request implements Runnable
	{
		private final byte[] m_request;
		private final long m_retryDelayMS;

		/**
		 * @param request
		 * @param retryDelayMS
		 */
		public Request(byte[] request, long retryDelayMS)
		{
			m_request = request;
			m_retryDelayMS = retryDelayMS;
		}

		public byte[] getRequestBytes()
		{
			return m_request;
		}

		public long getRetryDelayMS()
		{
			return m_retryDelayMS;
		}

		@Override
		public void run()
		{
		}

		@Override
		public String toString()
		{
			return new String(m_request).replace("\r", "").replace("\n", "") + (m_retryDelayMS < 0 ? "" : "*");
		}
	}
}
