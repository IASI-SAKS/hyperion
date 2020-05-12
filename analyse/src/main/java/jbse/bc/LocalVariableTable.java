package jbse.bc;

import jbse.common.Type;

import java.util.*;

/**
 * Class of objects representing a method local variable table.
 */
public class LocalVariableTable implements Iterable<LocalVariableTable.Row> {
	public static class Row {
		public int slot;
		public String descriptor;
		public String name;
		public int start;
		public int length;
		
		public Row(int slot, String descriptor, String name, int start, int length) {
			this.slot = slot;
			this.descriptor = descriptor;
			this.name = name;
			this.start = start;
			this.length = length;
		}
		
		@Override
		public String toString() {
			return "<" + slot + " " + descriptor + " " + name + " " + start + " " + length + ">";
		}
	}

	/** total number of slots */
	private int slots;
	
	/** entries of the local variable table as a map slot -> entries */
	private Map<Integer, Set<Row>> entries;
	
	/**
	 * Constructor returning an empty local variable table.
	 * 
	 * @param slots the number of slots of the local table.
	 */
	public LocalVariableTable(int slots) {
		this.slots = slots;
		this.entries = new HashMap<Integer, Set<Row>>();
	}
	
	public void setEntry(int slot, String descriptor, String name, int start, int length) {
		//rejects ill-formed entries
		if (slot >= this.slots) {
			return;
		}
		if ((descriptor.charAt(0) == Type.DOUBLE || descriptor.charAt(0) == Type.LONG)
			 && slot >= this.slots - 1) {
			return;
		}

		//gets/creates the set of entries associated to the slot number
		Set<Row> sub = null;
		if (this.entries.containsKey(slot))
			sub = this.entries.get(slot);
		else {
			sub = new HashSet<Row>();
			this.entries.put(slot, sub);
		}
		
		//adds a new entry to the set
		Row e = new Row(slot, descriptor, name, start, length);
		sub.add(e);
	}
	
	/**
	 * Given a slot number and a (value) type, returns all the type compatible 
	 * entries.
	 * 
	 * @param slot the number of the slot.
	 * @return a <code>Set&lt;Entry&gt;</code> containing all the rows 
	 *         associated to <code>slot</code> (empty iff no such 
	 *         entries exist).
	 */
	public Iterable<Row> rows(int slot) {
		Set<Row> retVal = this.entries.get(slot);
		if (retVal == null) {
			retVal = new HashSet<Row>();
		}
		return retVal;
	}
	
	public Row row(int slot, int curPC) {
        for (Row r : this.rows(slot)) {
        	if (r.start <= curPC && curPC < r.start + r.length) {
        		return r;
        	}
        }
        return null;
	}
	
	public int getSlots() {
		return this.slots;
	}

	public Iterator<Row> iterator() {
		return new MyIterator();
	}
	
	private class MyIterator implements Iterator<Row> {
		Iterator<Map.Entry<Integer, Set<Row>>> itOuter;
		Iterator<Row> itSub;
		
		public MyIterator() {
			//turns on the outer iterator
			Set<Map.Entry<Integer, Set<Row>>> entries = LocalVariableTable.this.entries.entrySet();
			this.itOuter = entries.iterator();
			this.itSub = null;
		}
		
		/**
		 * Checks if <code>this.itSub</code> is inert.
		 * 
		 * @return <code>true</code> iff <code>this.itSub</code> is 
		 *         inert, i.e., iff it does not provide a next element. 
		 */
		private boolean itSubInert() {
			return (this.itSub == null || this.itSub.hasNext() == false);
		}
		
		public boolean hasNext() {
			return (!this.itSubInert() || this.itOuter.hasNext());
		}

		public Row next() {
			//checks precondition
			if (!this.hasNext())
				throw new NoSuchElementException();
			
			//if the inner iterator is inert, turns it on
			if (this.itSubInert()) {
				Map.Entry<Integer, Set<Row>> e = this.itOuter.next();
				Set<Row> subEntries = e.getValue();
				this.itSub = subEntries.iterator();
			}
			
			//returns the next item
			return itSub.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}