/*******************************************************************************************************
 *
 * CSVRow.java, in gama.ui.shared.viewers, is part of the source code of the GAMA modeling and simulation platform
 * .
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.viewers.csv.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a row made of String elements
 *
 * @author fhenri
 *
 */
public class CSVRow {

	/** Splitted line */
	private final ArrayList<String> entries;

	/** Row changes listener */
	private final IRowChangesListener listener;

	/** The is header. */
	private boolean isHeader;

	/**
	 * Constructor
	 *
	 * @param line
	 * @param listener
	 */
	public CSVRow(final List<String> line, final IRowChangesListener listener) {
		entries = new ArrayList<>(line);
		this.listener = listener;
	}

	/**
	 * Constructor
	 *
	 * @param lineElements
	 * @param listener
	 */
	public CSVRow(final String[] lineElements, final IRowChangesListener listener) {
		this(Arrays.asList(lineElements), listener);
	}

	/**
	 * Create an empty row
	 *
	 * @param nbOfColumns
	 * @param delimiter
	 * @param listener
	 * @return
	 */
	public static CSVRow createEmptyLine(final int nbOfColumns, final IRowChangesListener listener) {
		final List<String> line = new LinkedList<>();
		for (int i = 0; i < nbOfColumns; i++) { line.add(""); }
		return new CSVRow(line, listener);
	}

	/**
	 * @return
	 */
	public ArrayList<String> getEntries() { return entries; }

	/**
	 * @return
	 */
	public String[] getEntriesAsArray() { return entries.toArray(new String[entries.size()]); }

	/**
	 * @param elementIndex
	 * @param elementString
	 */
	public void setRowEntry(final int elementIndex, final String elementString) {
		if (entries.get(elementIndex).compareTo(elementString) != 0) {
			entries.set(elementIndex, elementString);
			listener.rowChanged(this, elementIndex);
		}
	}

	/**
	 * return the element at a given index. This method makes sure that if the current line does not have as many
	 * elements as the header, it will not break and return an empty string
	 *
	 * @param index
	 * @return the element at a given index
	 */
	public String getElementAt(final int index) {
		if (index >= entries.size()) return "";
		return entries.get(index);
	}

	/**
	 * Return the number of elements in this row
	 *
	 * @return number of elements in this row
	 */
	public int getNumberOfElements() { return entries.size(); }

	/**
	 * @param element
	 */
	public void addElement(final String element) {
		entries.add(element);
	}

	/**
	 * Remove an element of the row represented by its index
	 *
	 * @param index
	 */
	public void removeElementAt(final int index) {
		entries.remove(index);
	}

	/**
	 * Sets the header.
	 *
	 * @param header
	 *            the new header
	 */
	public void setHeader(final boolean header) { isHeader = header; }

	/**
	 * Checks if is header.
	 *
	 * @return true, if is header
	 */
	public boolean isHeader() { return isHeader; }

	/**
	 * Gets the comment.
	 *
	 * @return the comment
	 */
	public String getComment() { return entries.get(0).substring(1); }

	/**
	 * Give the String representation of a CSVRow object.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String result = "";
		for (final String s : entries) {
			// FIXME get preferences here
			result = result.concat(s).concat(",");
		}
		return result;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(entries);
	}

	/**
	 * A CSVRow is equal to another one if all element are equals.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object anObject) {

		// The commented lines implies that if two rows have the same content,
		// the cell editor will modify
		// the first one found instead of the focused one
		// each row should be considered as unique even if they have the same
		// content

		/*
		 * AttributeRow thisRow = (AttributeRow) anObject; for (int i=0; i<getNumberOfElements(); i++) { if
		 * (!(getElementAt(i).equals(thisRow.getElementAt(i)))) { return false; } } return true;
		 */

		if (this == anObject) return true;
		return false;
	}

}
