package tenet.core;

public class Lib {
	private static boolean assertionEnabled = true;

	/**
	 * Control if assertion is enabled
	 * 
	 * @param ae
	 */
	public static void enableAssertion(boolean ae) {
		assertionEnabled = ae;
	}

	/**
	 * Asserts that <i>value</i> is <tt>true</tt>.
	 * 
	 * @param expression
	 *            the expression to assert.
	 */
	public static void assertTrue(boolean value) {
		if (assertionEnabled && !value)
			throw new AssertionFailureError();
	}

	/**
	 * Asserts that <i>value</i> is <tt>true</tt>.
	 * 
	 * @param expression
	 *            the expression to assert.
	 * @param message
	 *            the error message.
	 */
	public static void assertTrue(boolean value, String msg) {
		if (assertionEnabled && !value)
			throw new AssertionFailureError(msg);
	}

	/**
	 * Asserts that this call is never made. Same as <tt>assertTrue(false)</tt>.
	 */
	public static void assertNotReached() {
		assertTrue(false);
	}

	/**
	 * Asserts that this call is never made, with the specified error messsage.
	 * Same as <tt>assertTrue(false, message)</tt>.
	 * 
	 * @param message
	 *            the error message.
	 */
	public static void assertNotReached(String msg) {
		assertTrue(false, msg);
	}

	/**
	 * Set the binary interval of original to value, starting at pos, masked by
	 * mask
	 * 
	 * @param original
	 *            original value
	 * @param value
	 *            new partial value
	 * @param pos
	 *            the position
	 * @param mask
	 *            usually (1 << length) - 1
	 * @return the new value
	 */
	public static int setBits(int original, int value, int pos, int mask) {
		return original & (~(mask << pos)) | (value << pos);
	}

	/**
	 * Returns the binary interval of original, starting at pos, masked by mask
	 * 
	 * @param value
	 *            the original value
	 * @param pos
	 *            the position
	 * @param mask
	 *            usually (1 << length) - 1
	 */
	public static int getBits(int value, int pos, int mask) {
		return (value >> pos) & mask;
	}

	/**
	 * Convert to an int from its little-endian byte string representation.
	 * 
	 * @param array
	 *            the array containing the byte string.
	 * @param offset
	 *            the offset of the byte string in the array.
	 * @return the corresponding int value.
	 */
	public static int bytesToInt(byte[] array, int offset) {
		return (int) ((((int) array[offset + 0] & 0xFF) << 0)
				| (((int) array[offset + 1] & 0xFF) << 8)
				| (((int) array[offset + 2] & 0xFF) << 16) | (((int) array[offset + 3] & 0xFF) << 24));
	}

	/**
	 * Convert an int into its little-endian byte string representation.
	 * 
	 * @param array
	 *            the array in which to store the byte string.
	 * @param offset
	 *            the offset in the array where the string will start.
	 * @param value
	 *            the value to convert.
	 */
	public static void bytesFromInt(byte[] array, int offset, int value) {
		array[offset + 0] = (byte) ((value >> 0) & 0xFF);
		array[offset + 1] = (byte) ((value >> 8) & 0xFF);
		array[offset + 2] = (byte) ((value >> 16) & 0xFF);
		array[offset + 3] = (byte) ((value >> 24) & 0xFF);
	}
}

class AssertionFailureError extends Error {
	private static final long serialVersionUID = -1866982668595909275L;

	public AssertionFailureError() {
		super();
	}

	public AssertionFailureError(String message) {
		super(message);
	}
}
