/**
 * 
 */
package svenz.remote.device.ipremote;

import java.nio.channels.WritableByteChannel;
import org.apache.commons.configuration.Configuration;
import svenz.remote.device.ISound;

/**
 * @author Sven Zethelius
 *
 */
public class SoundImpl extends AbstractCoded<Void> implements ISound
{
	private  int m_volumeMin = -1;
	private  int m_volumeMax = -1;
	private final AbstractCoded<Integer> m_volumeCodes = new AbstractCoded<Integer>("volume", this);
	private final AbstractCoded<Boolean> m_muteCodes = new AbstractCoded<Boolean>("mute", this);

	public SoundImpl()
	{
		super(null, null);
	}

	@Override
	public void setName(String name)
	{
		super.setName(name);
		m_volumeCodes.setName(name + ".volume");
		m_muteCodes.setName(name + ".mute");
	}

	@Override
	public void addChangeListener(IChangeListener listener)
	{
		m_volumeCodes.addChangeListener(listener);
		m_muteCodes.addChangeListener(listener);

	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		m_volumeCodes.removeChangeListener(listener);
		m_muteCodes.removeChangeListener(listener);
	}

	@Override
	public void setWriter(WritableByteChannel writer)
	{
		m_muteCodes.setWriter(writer);
		m_volumeCodes.setWriter(writer);
	}

	@Override
	public void setCodes(Configuration config)
	{
		Configuration volumeCodes = config.subset("volume");
		m_volumeCodes.setCodes(filterCodes(volumeCodes, "Set", "Query"));
		int max = volumeCodes.getInt("Max", -1); 
		if (max > 0)
			setVolumeMax(max);
		int min = volumeCodes.getInt("Min", -1);
		if (min >= 0)
			setVolumeMin(min);
		
		m_muteCodes.setCodes(filterCodes(config.subset("mute"), "On", "Off", "Query"));
	}

	@Override
	public void query()
	{
		m_volumeCodes.query();
		m_muteCodes.query();
	}

	@Override
	public int getVolume()
	{
		Integer vol = m_volumeCodes.getStatus();
		return vol != null ? vol : -1;
	}

	@Override
	public int getVolumeMinimum()
	{
		return m_volumeMin;
	}

	@Override
	public int getVolumeMaximum()
	{
		return m_volumeMax;
	}

	public void setVolumeMin(int volumeMin)
	{
		m_volumeMin = volumeMin;
	}

	public void setVolumeMax(int volumeMax)
	{
		if (m_volumeMin == -1)
			m_volumeMin = 0;
		m_volumeMax = volumeMax;
	}
	
	@Override
	public void setVolume(int volume)
	{
		if (volume < m_volumeMin || volume > m_volumeMax)
			throw new IllegalArgumentException("Volume " + volume + " is invalid");

		m_volumeCodes.fireFormatted("Set", volume);

		if (Boolean.TRUE.equals(m_muteCodes.getStatus()))
			mute(false);
	}
	
	public void setVolumeStatus(int volume)
	{
		m_volumeCodes.setStatus(volume);
	}

	@Override
	public void mute(boolean muteOn)
	{
		m_muteCodes.fire(muteOn ? "On" : "Off");
	}

	@Override
	public boolean isMute()
	{
		return Boolean.TRUE.equals(m_muteCodes.getStatus());
	}

	public void setMuteStatus(boolean muteOn)
	{
		m_muteCodes.setStatus(muteOn);
	}

	@Override
	public String toString()
	{
		return getName() + "(Vol:" + m_volumeCodes.getStatus() + ",Mute:" + m_muteCodes.getStatus() + ")";
	}

}
