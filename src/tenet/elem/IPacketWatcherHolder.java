package tenet.elem;

/**
 * Holder of IPacketWatcher can hold only one watcher on one protocolid at same
 * time. To hold more watcher. Composite of IPacketWatcher should be considered.
 * If watcher is null, no invoking watcher.onPacket setPacketWatcher can be
 * invoked any time.
 * 
 * @author Meilun Sheng.
 * 
 */
public interface IPacketWatcherHolder {
	public void setPacketWatcher(IPacketWatcher watcher, Integer protocol);
}
