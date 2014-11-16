/**
 * 
 */
package svenz.remote.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.MapTransformer;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.action.MenuAction;
import svenz.remote.action.MuteToggleAction;
import svenz.remote.action.PlayableAction;
import svenz.remote.action.PowerToggleAction;
import svenz.remote.action.VolumeChangeAction;
import svenz.remote.common.utilities.ConfigurationList;
import svenz.remote.common.utilities.LoggingExceptionHandler;
import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.common.utilities.StringFormatTransformer;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.DeviceGroupRegistry;
import svenz.remote.device.IMenu;
import svenz.remote.device.IPlayable;
import svenz.remote.device.impl.MenuSelectable;
import svenz.remote.device.impl.OrderedSelectable;
import svenz.remote.device.jaxb.DeviceGroups;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.LoggingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager;

/**
 * @author Sven Zethelius
 *
 */
public class Main implements Closeable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static final Insets INSET_ZERO = new Insets(0, 0, 0, 0);
	private Map<String, String> m_resources;
	private DeviceGroupRegistry m_deviceRegistry;
	private ScheduledExecutorService m_executor;
	private CompositeConfiguration m_orderConfig;
	private final MenuSelectable m_menuSelectable = new MenuSelectable();
	private final Collection<Closeable> m_closes = new ArrayList<Closeable>();
	private final int m_inset = 8;

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception
	{
		LoggingExceptionHandler.init();
		final Main main = new Main();

		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4); // TODO determine best pool size
		Utilities.configure(executor, 1, TimeUnit.MINUTES);
		main.setExecutor(executor);

		main.setResources(ResourceBundle.getBundle(Main.class.getPackage().getName() + ".Resources",
				Locale.getDefault()));

		SocketChannelManager channelManager = new SocketChannelManager();
		channelManager.setExecutor(executor);

		SSDPManager ssdpManager = new SSDPManager(channelManager, executor);
		ssdpManager.addDeviceListener(new LoggingDeviceListener());

		DeviceGroupRegistry registry = new DeviceGroupRegistry();
		registry.setExecutor(executor);
		registry.setSSDPManager(ssdpManager);
		registry.setChannelManager(channelManager);
		registry.load(readDeviceGroups());
		main.setDeviceRegistry(registry);

		// TODO configure
		PropertiesConfiguration orderConfig = new PropertiesConfiguration(new File("./order.properies"));
		Utilities.setAsyncSaveConfiguration(orderConfig, executor, 1, TimeUnit.SECONDS);
		main.setOrderConfiguration(orderConfig);

		main.addCloseable(registry, ssdpManager, channelManager);

		channelManager.open();
		ssdpManager.open();
		registry.open();

		SwingUtilities.invokeLater(new LoggingRunnable(new Runnable() {

			@Override
			public void run()
			{
				main.show();
			}
		}));
	}

	private static DeviceGroups readDeviceGroups() throws Exception
	{
		InputStream is = Main.class.getResourceAsStream("DeviceGroup.xml");
		try
		{
			return new Persister().read(DeviceGroups.class, is);
		}
		finally
		{
			Utilities.safeClose(is);
		}
	}

	@Override
	public void close()
	{
		for (Iterator<Closeable> iter = m_closes.iterator(); iter.hasNext();)
		{
			Utilities.safeClose(iter.next());
			iter.remove();
		}

		m_executor.shutdown();
	}

	public void addCloseable(Closeable... closes)
	{
		Collections.addAll(m_closes, closes);
	}

	public void setResourcesMap(Map<String, String> resources)
	{
		m_resources = resources;
	}

	@SuppressWarnings("unchecked")
	public void setResources(ResourceBundle resources)
	{
		m_resources = MapUtils.toMap(resources);
	}

	public void setExecutor(ScheduledThreadPoolExecutor executor)
	{
		m_executor = executor;
	}

	public void setDeviceRegistry(DeviceGroupRegistry deviceRegistry)
	{
		m_deviceRegistry = deviceRegistry;
		m_deviceRegistry.register(m_menuSelectable);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setOrderConfiguration(FileConfiguration orderConfig)
	{
		for (Iterator<String> iter = orderConfig.getKeys(); iter.hasNext();)
			m_resources.remove(iter.next());
		CompositeConfiguration config = new CompositeConfiguration();
		config.addConfiguration(orderConfig, true);
		config.addConfiguration(new MapConfiguration((Map) m_resources), false);
		m_orderConfig = config;
	}

	public void show()
	{
		try
		{
			JPanel panel = panel();
			GridBagLayout layout = layout(1, 2);
			panel.setLayout(layout);
			GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout);
			gbc.horiz().n().weight(1.0, 0.0);
			panel.add(getSelectors(), gbc.clone().inset(m_inset, m_inset, m_inset / 2, m_inset));
			gbc.nextRow();
			panel.add(getControls(), gbc.clone().inset(m_inset / 2, m_inset, m_inset, m_inset).horiz().weight(1.0, 1.0));

			CloseActionListener.initKeyBindings(panel);
			initFrame(panel, gbc.gridy + 1);
		}
		catch (IOException e)
		{
			LOGGER.error("Unhandled exception", e);
			close();
		}
	}

	private void initFrame(JPanel panel, int rows)
	{
		final JFrame frame = new JFrame();
		frame.setTitle(m_resources.get("Frame/title"));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		contentPane.add(panel);

		Dimension dimension = frame.getLayout().preferredLayoutSize(panel);
		dimension.width += m_inset * 2;
		dimension.height += m_inset * (rows * 2);
		frame.setMinimumSize(dimension);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e)
			{
				Utilities.safeClose(Main.this);
			}
		});
		frame.setVisible(true);
	}

	private JPanel getSelectors() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(1, 2);
		panel.setLayout(layout);

		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout);
		gbc.weight(1.0, 0.0).horiz();

		DeviceActionListener listener = new DeviceActionListener();
		listener.setRegistry(m_deviceRegistry);
		JComboBox<String> selectLocation = getSelect("Frame.selectors.location", m_deviceRegistry.getLocations());
		listener.setLocation(selectLocation);
		panel.add(selectLocation, gbc.clone().inset(0, 0, m_inset / 2, 0));
		gbc.nextRow();

		JComboBox<String> selectActivity = getSelect("Frame.selectors.activity", m_deviceRegistry.getActivities());
		listener.setActivity(selectActivity);
		panel.add(selectActivity, gbc.clone().inset(m_inset / 2, 0, 0, 0));

		listener.actionPerformed(new ActionEvent(selectLocation, ActionEvent.ACTION_PERFORMED, null));
		return panel;
	}

	private JPanel getControls() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(1, 2);
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n().horiz().weight(1.0, 0.0);

		panel.add(getUpperControl(), gbc.clone().inset(0, 0, m_inset, 0));
		gbc.nextRow();
		panel.add(getPlayableControl(), gbc.clone());

		return panel;
	}

	private Component getUpperControl() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(3, 1);
		layout.columnWidths = new int[] { 48, 144, 48 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n().horiz().weight(1.0, 0.0);

		panel.add(getUpperLeftControl(), gbc.clone().inset(0, 0, 0, m_inset / 2).nw());
		panel.add(getMenuControl(), gbc.nextCol().clone().inset(0, m_inset / 2, 0, m_inset / 2));
		panel.add(getUpperRightControl(), gbc.nextCol().clone().inset(0, m_inset / 2, 0, 0).ne());
		return panel;
	}

	private Component getUpperLeftControl() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(1, 1);
		layout.columnWidths = new int[] { 48 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n();

		JButton btnPower = button("Frame.controls.power", 48, 48);
		// TODO image change on power on/off
		PowerToggleAction powerAction = new PowerToggleAction();
		m_deviceRegistry.register(powerAction);
		btnPower.addActionListener(action(powerAction));
		panel.add(btnPower, gbc.clone());

		return panel;
	}

	private Component getMenuControl() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(1, 2);
		layout.columnWidths = new int[] { 128 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n();

		panel.add(getMenuButtons(), gbc.clone());
		gbc.nextRow();
		panel.add(getMenuArrows(), gbc.clone().inset(m_inset));

		return panel;
	}

	/**
	 * Get a transformer that converts from a key to a resource, using a formatter to determine the actual resource name
	 * from the key.
	 * 
	 * @param format
	 *            String transform of the key
	 * @return
	 */
	private Transformer getFormatResourceTransformer(String format)
	{
		return Utilities.cacheTransformer(new ChainedTransformer(new Transformer[] {
				new StringFormatTransformer(format),
				MapTransformer.getInstance(m_resources)
		}));
	}

	private Component getMenuButtons() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(2, 1);
		layout.columnWidths = new int[] { 64, 64 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).both();
		JButton btnMenu = button("Frame.controls.menu", 64, 32);

		List<String> order = new ConfigurationList<>(m_orderConfig, "Device.menu/list");
		OrderedSelectable selectable = new OrderedSelectable(m_menuSelectable, order);
		SelectablePopupRunnable popup = new SelectablePopupRunnable();
		popup.setTransformer(getFormatResourceTransformer("Device.menu.%1$s/name"));
		popup.setSelectable(selectable);
		popup.setTarget(btnMenu);

		ClickLengthMouseListener cllistener = new ClickLengthMouseListener();
		cllistener.setExecutor(m_executor);
		cllistener.setLongDelay(600, TimeUnit.MILLISECONDS); // TODO config
		cllistener.setLongClick(new RunnableActionListener(popup));
		cllistener.setShortClick(action(menu(IMenu.Action.Menu)));
		btnMenu.addMouseListener(new ButtonFilterMouseListener(cllistener, MouseEvent.BUTTON1));
		btnMenu.addMouseListener(new ButtonFilterMouseListener(new ActionMouseListener(
				new RunnableActionListener(popup)), MouseEvent.BUTTON3));

		panel.add(btnMenu, gbc.clone().inset(0, 0, 0, m_inset / 2).nw());

		JButton btnExit = button("Frame.controls.exit", 64, 32);
		btnExit.addActionListener(action(menu(IMenu.Action.Exit)));
		panel.add(btnExit, gbc.nextCol().clone().inset(0, m_inset / 4, 0, 0).ne());

		return panel;
	}

	private Component getMenuArrows() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(3, 3);
		layout.columnWidths = new int[] { 24, 96, 24 };
		layout.rowHeights = new int[] { 24, 96, 24 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n();

		JButton btnMenuUp = button("Frame.controls.menu.up", 96, 24);
		btnMenuUp.addActionListener(action(menu(IMenu.Action.Up)));
		panel.add(btnMenuUp, gbc.nextCol().clone().s());

		gbc.nextRow();
		JButton btnMenuLeft = button("Frame.controls.menu.left", 24, 96);
		btnMenuLeft.addActionListener(action(menu(IMenu.Action.Left)));
		panel.add(btnMenuLeft, gbc.clone().e());

		JButton btnMenuEnter = button("Frame.controls.enter", 96, 96);
		btnMenuEnter.addActionListener(action(menu(IMenu.Action.Menu)));
		panel.add(btnMenuEnter, gbc.nextCol().clone().c().inset(m_inset));

		JButton btnMenuRight = button("Frame.controls.menu.right", 24, 96);
		btnMenuRight.addActionListener(action(menu(IMenu.Action.Right)));
		panel.add(btnMenuRight, gbc.nextCol().clone().w());
		gbc.nextRow();

		JButton btnMenuDown = button("Frame.controls.menu.down", 96, 24);
		btnMenuDown.addActionListener(action(menu(IMenu.Action.Down)));
		panel.add(btnMenuDown, gbc.nextCol().clone().n());

		return panel;
	}

	private Component getUpperRightControl() throws IOException
	{
		return getVolumePanel();
	}

	private Component getVolumePanel() throws IOException
	{
		JPanel panel = panel();

		GridBagLayout layout = layout(1, 3);
		layout.columnWidths = new int[] { 48 };
		layout.rowHeights = new int[] { 48, 48, 48 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n();

		JButton btnVolUp = button("Frame.controls.volume.up", 48, 48);
		btnVolUp.addMouseListener(repeat(action(vol(1)), 300, TimeUnit.MILLISECONDS)); // TODO configure
		panel.add(btnVolUp, gbc.clone().inset(0, 0, m_inset / 2, 0));
		gbc.nextRow();

		JButton btnVolDown = button("Frame.controls.volume.down", 48, 48);
		btnVolDown.addMouseListener(repeat(action(vol(-1)), 300, TimeUnit.MILLISECONDS)); // TODO configure
		panel.add(btnVolDown, gbc.clone().inset(m_inset / 2, 0, m_inset / 2, 0));
		gbc.nextRow();

		JButton btnMute = button("Frame.controls.volume.mute", 48, 48);
		btnMute.addActionListener(action(mute()));
		// TODO change listener for icon
		panel.add(btnMute, gbc.clone().inset(m_inset / 2, 0, 0, 0));

		return panel;
	}

	private JPanel getPlayableControl() throws IOException
	{
		JPanel panel = panel();
		GridBagLayout layout = layout(4, 2);
		layout.columnWidths = new int[] { 64, 64, 64, 64 };
		layout.rowHeights = new int[] { 32, 32 };
		panel.setLayout(layout);
		GridBagConstraintsBuilder gbc = new GridBagConstraintsBuilder(layout).n().both();

		JButton btnPause = button("Frame.controls.pause", 32, 32);
		btnPause.addActionListener(action(play(IPlayable.Action.Pause)));
		panel.add(btnPause, gbc.clone().inset(0, 0, m_inset / 2, m_inset / 2));

		JButton btnPlay = button("Frame.controls.play", 32, 32);
		btnPlay.addActionListener(action(play(IPlayable.Action.Play)));
		panel.add(btnPlay, gbc.nextCol().width(2).clone().inset(0, m_inset / 2, m_inset / 2, m_inset / 2));

		JButton btnStop = button("Frame.controls.stop", 32, 32);
		btnStop.addActionListener(action(play(IPlayable.Action.Stop)));
		panel.add(btnStop, gbc.nextCol().clone().inset(0, m_inset / 2, m_inset / 2, 0));
		gbc.nextRow();

		JButton btnPrev = button("Frame.controls.skip.rev", 64, 32);
		btnPrev.addActionListener(action(play(IPlayable.Action.PreviousTrack)));
		panel.add(btnPrev, gbc.clone().inset(m_inset / 2, 0, 0, m_inset / 2));

		JButton btnRewind = button("Frame.controls.rewind", 64, 32);
		btnRewind.addActionListener(action(play(IPlayable.Action.Rewind)));
		panel.add(btnRewind, gbc.nextCol().clone().inset(m_inset / 2, m_inset / 2, 0, m_inset / 2));

		JButton btnFastForward = button("Frame.controls.ffwd", 64, 32);
		btnFastForward.addActionListener(action(play(IPlayable.Action.FastForward)));
		panel.add(btnFastForward, gbc.nextCol().clone().inset(m_inset / 2, m_inset / 2, 0, m_inset / 2));

		JButton btnNext = button("Frame.controls.skip.fwd", 64, 32);
		btnNext.addActionListener(action(play(IPlayable.Action.NextTrack)));
		panel.add(btnNext, gbc.nextCol().clone().inset(m_inset / 2, m_inset / 2, 0, 0));

		return panel;
	}

	/**
	 * Update any saved configuration to reflect the current list incase it has changed.
	 * 
	 * @param id
	 *            configuration id
	 * @param orderListDefault
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateListConfig(String id, List<String> existingList)
	{
		List<String> orderList = (List) m_orderConfig.getList(id);
		existingList = Utilities.mergeOrderedLists(existingList, orderList);
		m_orderConfig.setProperty(id, existingList);
	}

	private JComboBox<String> getSelect(String id, Collection<String> selections) throws IOException
	{
		JComboBox<String> select = new JComboBox<String>();
		String listProperty = id + "/list";

		List<String> selectionList = new ArrayList<>(selections);

		updateListConfig(listProperty, selectionList);

		List<String> order = new ConfigurationList<>(m_orderConfig, listProperty);
		select.setModel(new MostRecentlyUsedListComboModel<String>(order));
		localize(select, id);
		select.setRenderer(new TransformerListCellRenderer(select.getRenderer(), 
				getFormatResourceTransformer(id+".%1$s/name")));
		select.addActionListener(new ListOrderListener(order));
		return select;
	}

	private JButton button(String id, int width, int height) throws IOException
	{
		JButton btn = new JButton(icon(id, width, height));
		localize(btn, id);
		btn.setMargin(INSET_ZERO);
		return btn;
	}

	private Icon icon(String id, int width, int height) throws IOException
	{
		String file = m_resources.get(id + "/icon");
		Image image = ImageIO.read(this.getClass().getResource(file));
		if (((BufferedImage) image).getWidth() != width && ((BufferedImage) image).getHeight() != height)
			image = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);

		return new ImageIcon(image);
	}

	private void localize(JComponent c, String id)
	{
		c.setToolTipText(m_resources.get(id + "/tooltip"));
	}

	private static GridBagLayout layout(int columns, int rows)
	{
		return layout(pixels(columns), pixels(rows));
	}

	private static GridBagLayout layout(int[] columnWidths, int[] rowHeights)
	{
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = columnWidths;
		layout.rowHeights = rowHeights;
		layout.columnWeights = weights(columnWidths);
		layout.rowWeights = weights(rowHeights);
		return layout;
	}

	private static double[] weights(int[] pxs)
	{
		double[] weights = new double[pxs.length];
		for (int i = 0; i < pxs.length; i++)
		{
			weights[i] = 0.0;
		}
		return weights;
	}

	private static int[] pixels(int count)
	{
		int[] pixels = new int[count];
		for (int i = 0; i < count; i++)
		{
			pixels[i] = 0;
		}
		return pixels;
	}

	private JPanel panel()
	{
		JPanel panel = new JPanel();
		// panel.setBorder(new LineBorder(Color.black));
		return panel;
	}

	private ActionListener action(Runnable r)
	{
		return new RunnableActionListener(r, m_executor);
	}

	private MenuAction menu(IMenu.Action action)
	{
		MenuAction menuAction = new MenuAction();
		menuAction.setAction(action);
		m_menuSelectable.addMenuAware(menuAction);
		return menuAction;
	}

	private PlayableAction play(IPlayable.Action action)
	{
		PlayableAction playAction = new PlayableAction();
		playAction.setAction(action);
		m_deviceRegistry.register(playAction);
		return playAction;
	}

	private VolumeChangeAction vol(int delta)
	{
		VolumeChangeAction action = new VolumeChangeAction();
		action.setDelta(delta);
		m_deviceRegistry.register(action);
		return action;
	}

	private MuteToggleAction mute()
	{
		MuteToggleAction action = new MuteToggleAction();
		m_deviceRegistry.register(action);
		return action;
	}

	private MouseListener repeat(ActionListener action, long delay, TimeUnit unit)
	{
		RepeatingClickMouseListener listener = new RepeatingClickMouseListener();
		listener.setListener(action);
		listener.setDelay(delay, unit);
		listener.setExecutor(m_executor);
		return listener;
	}

}
