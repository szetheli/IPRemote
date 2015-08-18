/**
 * 
 */
package svenz.remote.comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.LoggingExceptionHandler;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.remote.net.protocol.ssdp.LoggingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager;

/**
 * Main class for sending commands to specific IP
 * 
 * @author Sven Zethelius
 * 
 */
public class IPCommandSender2 implements Callable<Void>, ITCPSocketChannelCallback
{

	private static final Logger LOGGER = LoggerFactory.getLogger(IPCommandSender2.class);
	private final ScheduledThreadPoolExecutor m_executor = new ScheduledThreadPoolExecutor(4);
	private final SocketChannelManager m_channelManager = new SocketChannelManager();
	private final SSDPManager m_ssdpManager;
	private final Pattern m_pattern = Pattern.compile("(.*?):(\\d+)");
	private final WritableByteChannel m_instance;


	/**
	 * Expects IP:port as parameter
	 * 
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception
	{
		LoggingExceptionHandler.init();
		new IPCommandSender2(args[0]).call();
	}

	public IPCommandSender2(String address) throws Exception
	{
		m_executor.setKeepAliveTime(1, TimeUnit.MINUTES);
		m_executor.allowCoreThreadTimeOut(true);

		m_ssdpManager = new SSDPManager(m_channelManager, m_executor);

		m_channelManager.setExecutor(m_executor);
		m_channelManager.open();

		m_ssdpManager.addDeviceListener(new LoggingDeviceListener());

		m_ssdpManager.open();

		Matcher matcher = m_pattern.matcher(address);
		matcher.matches();
		InetSocketAddress inet = new InetSocketAddress(matcher.group(1), Integer.parseInt(matcher.group(2)));
		m_instance = m_channelManager.connect(inet, this);
	}

	@Override
	public Void call() throws Exception
	{
		BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		while (m_instance.isOpen() && null != (line = rdr.readLine()))
		{
			if (line.contains("\\r") || line.contains("\\n"))
				line = line.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
			else
				line = line + "\r\n";

			LOGGER.trace("Sent: {}", line);
			m_instance.write(ByteBuffer.wrap(line.getBytes()));
		}
		return null;
	}

	@Override
	public void handleRead(byte[] b, InetSocketAddress address) throws IOException
	{
		LOGGER.info("Received: {}", new String(b));
	}

	@Override
	public void close() throws IOException
	{
		LOGGER.info("Closing {}", m_instance);
		Utilities.safeClose(m_instance);
		Utilities.safeClose(m_ssdpManager);
		Utilities.safeClose(m_channelManager);
		m_executor.shutdown();
	}

	@Override
	public void connectionOpen(TCPSocketChannelInstance instance)
	{
		LOGGER.info("Connected to {}", instance);
	}

	@Override
	public void connectionClose(TCPSocketChannelInstance instance)
	{
		LOGGER.info("connectionClose {}", m_instance);
	}

	@Override
	public void connectionFailed(TCPSocketChannelInstance instance)
	{
		LOGGER.info("connectionFailed {}", m_instance);
	}

}
