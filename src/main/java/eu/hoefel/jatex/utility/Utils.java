package eu.hoefel.jatex.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods.
 * 
 * @author udoh
 */
public class Utils {

	private Utils() {
	    throw new IllegalStateException("Utility class");
	}

	/**
	 * Gets the array dimension, i.e. e.g. the double[][] class will yield 2.
	 * 
	 * @param type the class to check
	 * @return the dimensionality of the type
	 */
	public static int getArrayDimension(Class<?> type) {
		if (type.getComponentType() == null) {
			return 0;
		} else {
			return getArrayDimension(type.getComponentType()) + 1;
		}
	}

	/**
	 * Flattens a 2D double array into 1D. The returned data is stored row by row
	 * from {@code a}. This method only works for rectangular arrays.
	 * 
	 * @param a the 2D array
	 * @return the flattened array
	 */
	public static final double[] flatten(double[][] a) {
		int size = a.length * a[0].length;
		double[] flat = new double[size];

		int index = 0;
		for (int i = 0; i < a.length; i++) {
			int numNew = a[i].length;
			System.arraycopy(a[i], 0, flat, index, numNew);
			index += numNew;
		}
		return flat;
	}

	/**
	 * Creates the directory if necessary/possible.
	 * 
	 * @param directory the directory to create
	 */
	public static final void mkdir(String directory) {
		File f = new File(directory);
		if (f.isDirectory() && f.exists()) return;

		if (!f.mkdir() && !f.isDirectory() || !f.exists()) {
			throw new IllegalAccessError("Directory '" + f.getParent() + "' does not exist and we cannot create it.");
		}
	}

	/**
	 * Writes the specified text to the specified file.
	 * 
	 * @param fileName the name of the file to write to
	 * @param text     the text to write
	 */
	public static final void textToFile(String fileName, String text) {
		mkdir(new File(fileName).getParent());
		try (FileOutputStream fOut = new FileOutputStream(fileName)) {
			fOut.write(text.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Puts the given strings into a map (as keys, the values are empty Strings).
	 * 
	 * @param strs the to-be keys for the map
	 * @return a map with the given strings as keys and empty strings as values
	 */
	public static final Map<String, String> mapOf(String... strs) {
		return Stream.of(strs).filter(Objects::nonNull).collect(Collectors.toMap(s -> s, s -> ""));
	}
}
