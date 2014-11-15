/**
 * 
 */
package svenz.remote.device.ipremote;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Sven Zethelius
 *
 */
public abstract class AbstractQueuingDevice extends AbstractDevice
{
	private transient final QueuingWritableByteChannel m_queueChannel = new QueuingWritableByteChannel();

	@Override
	protected WritableByteChannel getWriteChannel()
	{
		WritableByteChannel channel = super.getWriteChannel();
		m_queueChannel.setChannel(channel);
		return channel != null ? m_queueChannel : null;
	}

	protected QueuingWritableByteChannel getQueuingWritableByteChannel()
	{
		return m_queueChannel;
	}

	@Override
	protected void handleResponse(String response)
	{
		byte[] lastCodeBytes = m_queueChannel.getOutstandingRequest();
		if (lastCodeBytes == null)
			throw new IllegalStateException("Unexpected response without request " + response);

		boolean success = false;
		try
		{
			success = handleResponse(response, new String(lastCodeBytes));
		}
		finally
		{
			try
			{
				m_queueChannel.acknowledgeResponse(success);
			}
			catch (IOException e)
			{
				getLogger().error("Unable to send next message to {}", m_queueChannel, e);
			}
		}
	}

	protected void clearPending()
	{
		m_queueChannel.clearPending();
	}

	protected void clearPending(String code)
	{
		m_queueChannel.clearPending(code);
	}

	@Override
	public void setExecutor(ScheduledExecutorService executor)
	{
		super.setExecutor(executor);
	}

	protected abstract boolean handleResponse(String response, String lastCode);
}
