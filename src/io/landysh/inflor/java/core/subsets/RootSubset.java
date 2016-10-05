package io.landysh.inflor.java.core.subsets;

import java.io.Serializable;
import java.util.BitSet;

import io.landysh.inflor.java.core.dataStructures.ColumnStore;

public class RootSubset extends AbstractSubset implements Serializable{

	/**
	 *  Root subset class. Used as a root node in a subset tree.
	 */
	private static final long serialVersionUID = -8189764506384264612L;
	private ColumnStore data;
	
	public RootSubset(ColumnStore data) {
		this.data = data;
		this.members = new BitSet(data.getRowCount());
	}

	@Override
	protected BitSet evaluate() {
		return this.members;
	}

	@Override
	protected ColumnStore getData() {
		return this.data;
	}
}
//EOF