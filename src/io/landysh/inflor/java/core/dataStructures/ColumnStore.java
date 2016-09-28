package io.landysh.inflor.java.core.dataStructures;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import com.google.protobuf.InvalidProtocolBufferException;

import io.landysh.inflor.java.core.proto.AnnotatedVectorMessage.AnnotatedVectorsProto;
import io.landysh.inflor.java.core.proto.AnnotatedVectorMessage.AnnotatedVectorsProto.Keyword;
import io.landysh.inflor.java.core.proto.AnnotatedVectorMessage.AnnotatedVectorsProto.Vector;

public class ColumnStore {

	private static final String DEFAULT_PREFFERED_NAME_KEYWORD = "$FIL";

	public static ColumnStore load(byte[] bytes) throws InvalidProtocolBufferException {
		final AnnotatedVectorsProto message = AnnotatedVectorsProto.parseFrom(bytes);

		// Load the keywords
		final Hashtable<String, String> keywords = new Hashtable<String, String>();
		for (int i = 0; i < message.getKeywordsCount(); i++) {
			final Keyword keyword = message.getKeywords(i);
			final String key = keyword.getKey();
			final String value = keyword.getValue();
			keywords.put(key, value);
		}
		// Load the vectors
		final int rowCount = message.getVectors(0).getArrayCount();
		final ColumnStore columnStore = new ColumnStore(keywords, rowCount);
		final int columnCount = message.getVectorsCount();
		final String[] vectorNames = new String[columnCount];
		for (int j = 0; j < columnCount; j++) {
			final Vector vector = message.getVectors(j);
			final String key = vector.getName();
			vectorNames[j] = key;
			final double[] values = new double[vector.getArrayCount()];
			for (int i = 0; i < values.length; i++) {
				values[i] = vector.getArray(i);
			}
			columnStore.addColumn(key, values);
		}
		columnStore.setPreferredName(columnStore.getKeywordValue(DEFAULT_PREFFERED_NAME_KEYWORD));
		return columnStore;
	}

	public static ColumnStore load(FileInputStream input) throws Exception {
		final byte[] buffer = new byte[input.available()];
		input.read(buffer);
		final ColumnStore columnStore = ColumnStore.load(buffer);
		return columnStore;
	}

	// file details
	public String UUID;

	private Hashtable<String, String> keywords;
	private Hashtable<String, FCSDimension> columnData;

	// data properties
	private Integer rowCount = -1;
	private String preferredName;

	// minimal constructor, use with .load()
	public ColumnStore() {
	}

	/**
	 * Store keywords and numeric columns in a persistable object.
	 * 
	 * @param inKeywords
	 *            some annotation to get started with. Must be a valid FCS
	 *            header but may be added to later.
	 */
	public ColumnStore(Hashtable<String, String> keywords, int rowCount) {
		this.keywords = keywords;
		columnData = new Hashtable<String, FCSDimension>();
		this.rowCount = rowCount;
		preferredName = getKeywordValue(DEFAULT_PREFFERED_NAME_KEYWORD);
	}

	public void addColumn(String name, double[] data) {
		if (rowCount == data.length) {
			final FCSDimension newDimension = new FCSDimension(name, data, new Hashtable<String, String>());
			columnData.put(name, newDimension);
		} else {
			throw new IllegalStateException("New dimension does not match frame size: " + rowCount.toString());
		}
	}

	public double[] getDimensionData(String xName) {
		double[] data = columnData.get(xName).getData();
		return data;
	}

	public int getColumnCount() {
		return getColumnNames().length;
	}

	public String[] getColumnNames() {
		final int size = columnData.keySet().size();
		final String[] newArray = new String[size];
		final String[] columnNames = columnData.keySet().toArray(newArray);
		return columnNames;
	}

	public Hashtable<String, FCSDimension> getData() {
		return columnData;
	}

	public Hashtable<String, String> getKeywords() {
		return keywords;
	}

	public String getKeywordValue(String keyword) {
		String result = null;
		try {
			result = keywords.get(keyword).trim();
		} catch (NullPointerException npe) {
			// No operatoin, just return a null value.
		}
		return result;

	}


	public String getPrefferedName() {
		String name = UUID;
		if (this.preferredName != null) {
			name = this.preferredName;
		}
		return name;
	}

	public double[] getRow(int index) {
		final double[] row = new double[getColumnCount()];
		int i = 0;
		for (final String name : getColumnNames()) {
			row[i] = columnData.get(name).getData()[index];
			i++;
		}
		return row;
	}

	public int getRowCount() {
		return rowCount;
	}

	public FCSDimension getVector(String name) {
		return columnData.get(name);
	}

	public byte[] save() {
		// create the builder
		final AnnotatedVectorsProto.Builder messageBuilder = AnnotatedVectorsProto.newBuilder();

		// Should add UUID field here.

		// add the vector names.
		for (final String s : getColumnNames()) {
			messageBuilder.addVectorNames(s);
		}

		// add the keywords.
		for (final String s : keywords.keySet()) {
			final String key = s;
			final String value = keywords.get(s);
			final AnnotatedVectorsProto.Keyword.Builder keyBuilder = AnnotatedVectorsProto.Keyword.newBuilder();
			keyBuilder.setKey(key);
			keyBuilder.setValue(value);
			final AnnotatedVectorsProto.Keyword keyword = keyBuilder.build();
			messageBuilder.addKeywords(keyword);
		}
		// add the data.
		final Integer size = getColumnNames().length;
		for (int i = 0; i < size; i++) {
			final AnnotatedVectorsProto.Vector.Builder vectorBuilder = AnnotatedVectorsProto.Vector.newBuilder();
			final String name = getColumnNames()[i];
			// Raw data
			final double[] rawArray = columnData.get(name).getData();
			vectorBuilder.setName(name);
			for (final double element : rawArray) {
				vectorBuilder.addArray(element);
			}
			final AnnotatedVectorsProto.Vector v = vectorBuilder.build();
			messageBuilder.addVectors(v);
		}

		// build the message
		final AnnotatedVectorsProto AVSProto = messageBuilder.build();
		final byte[] message = AVSProto.toByteArray();
		return message;
	}

	public void save(FileOutputStream out) throws IOException {
		final byte[] message = this.save();
		out.write(message);
		out.flush();
	}

	public void setData(Hashtable<String, FCSDimension> allData) {
		columnData = allData;
		rowCount = allData.get(getColumnNames()[0]).getData().length;
	}

	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}

	@Override
	public String toString() {
		return getPrefferedName();
	}
}