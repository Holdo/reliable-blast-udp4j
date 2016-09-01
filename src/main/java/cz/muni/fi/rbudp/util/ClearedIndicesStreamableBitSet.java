package cz.muni.fi.rbudp.util;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class ClearedIndicesStreamableBitSet extends BitSet {

	public ClearedIndicesStreamableBitSet() {
		super();
	}

	public ClearedIndicesStreamableBitSet(int nbits) {
		super(nbits);
	}

	public IntStream clearedIndicesStream() {
		class BitSetIterator implements PrimitiveIterator.OfInt {
			private int next = nextClearBit(0);

			@Override
			public boolean hasNext() {
				return next != -1;
			}

			@Override
			public int nextInt() {
				if (next != -1) {
					int ret = next;
					next = nextClearBit(next + 1);
					return ret;
				} else {
					throw new NoSuchElementException();
				}
			}
		}

		return StreamSupport.intStream(
				() -> Spliterators.spliterator(
						new BitSetIterator(), cardinality(),
						Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED),
				Spliterator.SIZED | Spliterator.SUBSIZED |
						Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED,
				false);
	}
}
