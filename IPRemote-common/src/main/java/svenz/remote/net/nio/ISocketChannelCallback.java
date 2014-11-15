/**
 *
 * ISocketChannelCallback.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Callback interface for reading from a channel
 * 
 * @author Sven Zethelius
 * 
 */
public interface ISocketChannelCallback extends Closeable
{
	void handleRead(byte[] b, InetSocketAddress address) throws IOException;
}
