package tenet.elem;

/**
 * This interface describes that IPacketWatcherHolder ,which aggregates the
 * implement of this interface, may try to call IPacketWatcher.onPacket when a
 * new packet is arrival or is going to be sent. The implement of this interface
 * must tell the invoker the real packets that it should receives or sends.
 * (maybe no packet or many packets)
 * 
 * @author Meilun Sheng
 * 
 */
public interface IPacketWatcher {
	public Packet[] onPacket(int direction, Packet packet);
}
