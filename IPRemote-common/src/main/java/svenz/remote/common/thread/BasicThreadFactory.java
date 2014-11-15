/**
 *
 * BasicThreadFactory.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.common.thread;

import java.util.concurrent.ThreadFactory;

/**
 * BasicThreadFactory creates a thread
 * 
 * @author Sven Zethelius
 * 
 */
public class BasicThreadFactory implements ThreadFactory
{
	@Override
	public Thread newThread(Runnable r)
	{
		return new Thread(r);
	}
}
