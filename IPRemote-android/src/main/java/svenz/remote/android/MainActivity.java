package svenz.remote.android;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.simpleframework.xml.core.Persister;
import svenz.remote.action.MenuAction;
import svenz.remote.action.MuteToggleAction;
import svenz.remote.action.PlayableAction;
import svenz.remote.action.VolumeChangeAction;
import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.DeviceGroupRegistry;
import svenz.remote.device.IMenu;
import svenz.remote.device.IMenu.Action;
import svenz.remote.device.IPlayable;
import svenz.remote.device.impl.MenuSelectable;
import svenz.remote.device.jaxb.DeviceGroups;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Spinner;

public class MainActivity extends Activity
{
	private static final int MUTE_BUTTON_CLIP = 1550;
	private static final int MUTE_BUTTON_CLIP_LEVELS = 5;

	private MulticastLock m_multicastLock;
	private final ScheduledExecutorService m_executor;
	private final SocketChannelManager m_socketChannelManager = new SocketChannelManager();
	private final SSDPManager m_ssdpManager = new SSDPManager();
	private final DeviceGroupRegistry m_deviceGroupRegistry = new DeviceGroupRegistry();
	private final OrderedPreference m_preference = new OrderedPreference();
	private final MenuSelectable m_menuSelectable = new MenuSelectable();
	private final ConnectionReceiver m_receiver = new ConnectionReceiver();

	public MainActivity()
	{
		m_executor = new ScheduledThreadPoolExecutor(4); // TODO thread size?
		Utilities.configure((ScheduledThreadPoolExecutor) m_executor, 1, TimeUnit.MINUTES);

		m_socketChannelManager.setExecutor(m_executor);

		m_ssdpManager.setExecutor(m_executor);
		m_ssdpManager.setChannelManager(m_socketChannelManager);

		m_deviceGroupRegistry.setExecutor(m_executor);
		m_deviceGroupRegistry.setChannelManager(m_socketChannelManager);
		m_deviceGroupRegistry.setSSDPManager(m_ssdpManager);
		m_deviceGroupRegistry.register(m_menuSelectable);

		m_receiver.setSocketChannelManager(m_socketChannelManager);
		m_receiver.setSSDPManager(m_ssdpManager);
		m_receiver.setDeviceGroupRegistry(m_deviceGroupRegistry);

		m_preference.setExecutor(m_executor);
	}

	// Power off: onPause, onStop, ||| onRestart, onStart, onResume
	// Back: onPause, onStop, onDestroy
	// resume: onStart, onResume

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.v("Main", "onCreate(" + getIntent() + ")");

		setContentView(R.layout.activity_main);

		final WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled())
		{ // if wifi is disabled and the app is being created, attempt to connect
			wifi.setWifiEnabled(true);
			m_executor.schedule(new LoggingRunnable(new Runnable() {
				// disable if not connected after 1 minute
				@Override
				public void run()
				{
					if (null == wifi.getConnectionInfo())
						wifi.setWifiEnabled(false);
				}
			}), 1, TimeUnit.MINUTES);
		}
		// TODO connectionInfo.getMacAddress();
		// TODO connectionInfo.getSSID();

		m_multicastLock = wifi.createMulticastLock("multicastLock");

		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		m_ssdpManager.setSearchFrequency(20, TimeUnit.MINUTES); // TODO configure

		m_preference.setPreferences(preferences);
		setSpinnerAdapterFromLast(R.id.location, "location_");
		setSpinnerAdapterFromLast(R.id.activity, "activity_");

		m_executor.execute(new LoggingRunnable(new InitRunnable()));

		final ImageButton muteButton = (ImageButton) findViewById(R.id.mute);
		final ClipDrawable muteBaseClip = getClip(muteButton, R.id.mute_base_clip);
		muteBaseClip.setLevel(10000 - MUTE_BUTTON_CLIP * MUTE_BUTTON_CLIP_LEVELS);
		muteButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				int level = muteBaseClip.getLevel() - MUTE_BUTTON_CLIP;
				muteBaseClip.setLevel(level);
			}
		});

		initMenu();

		// TODO wire button event
		// TODO Toast for showing transient status messages
		// TODO remember device (device.xml and ip address) for quick reconnect without ssdp

	}

	private void initMenu()
	{
		ImageButton menuButton = (ImageButton) findViewById(R.id.menu);
		SelectablePopupListener menuListener = new SelectablePopupListener();
		menuListener.setActivity(this);
		menuListener.setKey("menu_");
		menuListener.setPreference(m_preference);
		menuListener.setSelectable(m_menuSelectable);
		menuListener.setTitleResourceId(R.string.menu_title);
		menuButton.setOnLongClickListener(menuListener);
		menuButton.setOnClickListener(action(menu(Action.Menu)));
	}

	private ClipDrawable getClip(ImageButton button, int clipId)
	{
		LayerDrawable drawable = (LayerDrawable) button.getDrawable();
		return (ClipDrawable) drawable.findDrawableByLayerId(clipId);
	}

	private void setSpinnerAdapterFromLast(int spinnerId, String prefix)
	{
		OrderedSpinnerAdapter dataAdapter = m_preference.getOrderedAdapter(this, R.layout.spinner_row, prefix);
		if (dataAdapter != null)
			setSpinnerAdapter(spinnerId, dataAdapter);
	}

	private OrderedSpinnerAdapter setSpinnerAdapterFromDevice(int spinnerId, String prefix, Collection<String> list)
	{
		ArrayList<String> objects = new ArrayList<String>(list.size() + 1);
		if (!list.contains(DeviceGroupRegistry.INVALID_SELECTION))
			objects.add(DeviceGroupRegistry.INVALID_SELECTION);
		objects.addAll(list);
		OrderedSpinnerAdapter dataAdapter = m_preference.getOrderedAdapter(this, R.layout.spinner_row, prefix, objects);
		setSpinnerAdapter(spinnerId, dataAdapter);
		return dataAdapter;
	}

	private void setSpinnerAdapter(int spinnerId, OrderedSpinnerAdapter dataAdapter)
	{
		dataAdapter.setDropDownViewResource(R.layout.spinner_row);

		Spinner spinner = (Spinner) findViewById(spinnerId);
		dataAdapter.register(spinner);
	}

	private void loadDevices()
	{
		// File filesDir = getFilesDir();
		// String trace = new File(filesDir, "profile.trace").getAbsolutePath();
		// Debug.startMethodTracing(trace);
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("DeviceGroup.xml"));
		try
		{
			DeviceGroups deviceGroups = new Persister().read(DeviceGroups.class, is);
			if (deviceGroups == null)
				throw new IllegalStateException("No deviceGroups");
			if (m_deviceGroupRegistry == null)
				throw new IllegalStateException("No registry WTF");
			m_deviceGroupRegistry.load(deviceGroups);

			m_receiver.onReceive(this, getIntent());
			registerReceiver(m_receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			registerReceiver(m_receiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

			// m_socketChannelManager.open(); // TODO
			m_ssdpManager.open();
			m_deviceGroupRegistry.open();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		catch (Error t)
		{
			Log.e("Main", "Exception", t);
			throw t;
		}
		finally
		{
			Utilities.safeClose(is);
		}
		// Debug.stopMethodTracing();
	}


	private void initUI()
	{
		OrderedSpinnerAdapter location =
				setSpinnerAdapterFromDevice(R.id.location, "location_", m_deviceGroupRegistry.getLocations());
		OrderedSpinnerAdapter activity =
				setSpinnerAdapterFromDevice(R.id.activity, "activity_", m_deviceGroupRegistry.getActivities());
		DeviceSelectionListener deviceSelectionListener = new DeviceSelectionListener();
		deviceSelectionListener.setRegistry(m_deviceGroupRegistry);
		deviceSelectionListener.setActivity(activity);
		deviceSelectionListener.setLocation(location);
		deviceSelectionListener.onItemSelected(null, null, MUTE_BUTTON_CLIP_LEVELS, MUTE_BUTTON_CLIP);

		// TODO registerReceiver(null, null, null, null); or in XML?

		initPower();

		VolumeAdjustListener volumeListener = new VolumeAdjustListener((ImageButton) findViewById(R.id.mute));
		m_deviceGroupRegistry.register(volumeListener);

		click(R.id.exit, action(menu(IMenu.Action.Exit)));
		click(R.id.back, action(menu(IMenu.Action.Back)));
		click(R.id.up, action(menu(IMenu.Action.Up)));
		click(R.id.down, action(menu(IMenu.Action.Down)));
		click(R.id.left, action(menu(IMenu.Action.Left)));
		click(R.id.right, action(menu(IMenu.Action.Right)));
		click(R.id.ok, action(menu(IMenu.Action.Enter)));

		click(R.id.volUp, action(vol(1)));
		click(R.id.volDown, action(vol(-1)));
		click(R.id.mute, action(mute()));

		click(R.id.play, action(play(IPlayable.Action.Play)));
		click(R.id.pause, action(play(IPlayable.Action.Pause)));
		click(R.id.stop, action(play(IPlayable.Action.Stop)));
		click(R.id.nextTrack, action(play(IPlayable.Action.NextTrack)));
		click(R.id.prevTrack, action(play(IPlayable.Action.PreviousTrack)));
		click(R.id.reverse, action(play(IPlayable.Action.Rewind)));
		click(R.id.forward, action(play(IPlayable.Action.FastForward)));

	}

	private void initPower()
	{
		ImageButton powerButton = (ImageButton) findViewById(R.id.power);
		PoweredAnimator powerAnimator = new PoweredAnimator(powerButton);
		m_deviceGroupRegistry.register(powerAnimator.getAnimator());
		powerButton.setOnClickListener(powerAnimator);
		// TODO long click listener
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		Log.v("Main", "onNewIntent(" + intent + ")");
		super.onNewIntent(intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		Log.v("Main", "onSaveInstanceState(" + outState + ")");
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		// TODO save ordered lists here rather then on each change
	}

	@Override
	public void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		Log.v("Main", "onAttachedToWindow()");
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Log.v("Main", "onStart()");
		m_multicastLock.acquire();
		try
		{
			m_socketChannelManager.open();
			m_ssdpManager.open();
		}
		catch (IOException e)
		{
			Log.e("Main", "Excepton opening connection", e);
		}
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		Log.v("Main", "onResume()");

	}

	@Override
	protected void onRestart()
	{
		// TODO Auto-generated method stub
		super.onRestart();
		Log.v("Main", "onRestart()");

	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		Log.v("Main", "onPause()");

	}

	@Override
	protected void onStop()
	{
		super.onStop();
		Log.v("Main", "onStop()");

		Utilities.safeClose(m_ssdpManager);
		Utilities.safeClose(m_socketChannelManager);
		if (m_multicastLock.isHeld())
			m_multicastLock.release();
	}

	@Override
	protected void onDestroy()
	{
		unregisterReceiver(m_receiver);
		Utilities.safeClose(m_deviceGroupRegistry);
		m_executor.shutdown();

		Log.v("Main", "onDestroy()");

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void click(int id, OnClickListener listener)
	{
		findViewById(id).setOnClickListener(listener);
	}

	private OnClickListener action(Runnable r)
	{
		return new RunnableOnClickListener(r, m_executor);
	}

	private Runnable play(IPlayable.Action paction)
	{
		PlayableAction action = new PlayableAction();
		action.setAction(paction);
		m_deviceGroupRegistry.register(action);
		return action;
	}

	private Runnable vol(int i)
	{
		VolumeChangeAction action = new VolumeChangeAction();
		action.setDelta(i);
		m_deviceGroupRegistry.register(action);
		return action;
	}

	private Runnable mute()
	{
		MuteToggleAction action = new MuteToggleAction();
		m_deviceGroupRegistry.register(action);
		return action;
	}

	private Runnable menu(IMenu.Action maction)
	{
		MenuAction action = new MenuAction();
		action.setAction(maction);
		m_menuSelectable.addMenuAware(action);
		return action;
	}

	/**
	 * Offload init tasks that take time to a background runnable so the main thread can load quickly.
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	private class InitRunnable implements Runnable
	{
		@Override
		public void run()
		{
			loadDevices();
			runOnUiThread(new InitUIRunnable());
		};
	}

	private class InitUIRunnable implements Runnable
	{
		@Override
		public void run()
		{
			initUI();
		}
	}

}
