/**
 * 
 */
package svenz.remote.net.nio;


/**
 * @author Sven Zethelius
 *
 */
public interface ITCPSocketChannelCallback extends ISocketChannelCallback
{
	void connectionOpen(TCPSocketChannelInstance instance);

	void connectionClose(TCPSocketChannelInstance instance);

	void connectionFailed(TCPSocketChannelInstance instance);
}
