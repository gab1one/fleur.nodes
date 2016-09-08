package io.landysh.inflor.java.knime.dataTypes.columnStoreCell;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;

import com.google.protobuf.InvalidProtocolBufferException;

import io.landysh.inflor.java.core.dataStructures.ColumnStore;

public class ColumnStoreContent {

	public static final class CellSerializer implements DataCellSerializer<ColumnStoreCell> {

		@Override
		public ColumnStoreCell deserialize(DataCellDataInput input) throws IOException {
			try {
				final byte[] bytes = new byte[input.readInt()];
				input.readFully(bytes);
				ColumnStore cStore;
				cStore = ColumnStore.load(bytes);
				final ColumnStoreCell newCell = new ColumnStoreCell(cStore);
				return newCell;
			} catch (final Exception e) {
				e.printStackTrace();
				throw new IOException("Error during deserialization");
			}
		}

		@Override
		public void serialize(ColumnStoreCell cell, DataCellDataOutput output) throws IOException {
			final byte[] bytes = cell.getColumnStore().save();
			output.writeInt(bytes.length);
			output.write(bytes);
		}
	}

	public static final DataType TYPE = DataType.getType(ColumnStoreCell.class);

	private ColumnStore m_data = null;

	public ColumnStoreContent(byte[] buffer) throws InvalidProtocolBufferException {
		m_data = ColumnStore.load(buffer);
	}

	public ColumnStoreContent(ColumnStore vectorStore) {
		m_data = vectorStore;
	}

	public ColumnStore getColumnStore() {
		return m_data;
	}

	public ColumnStoreCell toColumnStoreCell(FileStore fs) {
		return new ColumnStoreCell(fs, m_data);
	}
}