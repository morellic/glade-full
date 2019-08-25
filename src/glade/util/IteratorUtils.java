/*  Copyright 2015-2017 Stanford University                                                                                                                                       
 *                                                                                                                                                                               
 *  Licensed under the Apache License, Version 2.0 (the "License");                                                                                                               
 *  you may not use this file except in compliance with the License.                                                                                                              
 *  You may obtain a copy of the License at                                                                                                                                       
                                                                                                                                                                                
 *      http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                
                                                                                                                                                                                
 *  Unless required by applicable law or agreed to in writing, software                                                                                                           
 *  distributed under the License is distributed on an "AS IS" BASIS,                                                                                                             
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                                                                                                      
 *  See the License for the specific language governing permissions and                                                                                                           
 *  limitations under the License. 
 */

package glade.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import glade.util.OracleUtils.Wrapper;
import glade.util.RandomUtils.RandomExtra;
import glade.util.Utils.Callback;
import glade.util.Utils.Filter;

public class IteratorUtils {
	public static interface Sampler {
		public abstract String sample();
	}
	
	public static class EmptyIterator<T> implements Iterator<T> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			return null;
		}

		@Override
		public void remove() {
			throw new RuntimeException();
		}
	}
	
	public static class EmptyIterable<T> implements Iterable<T> {
		@Override
		public Iterator<T> iterator() {
			return new EmptyIterator<T>();
		}
	}
	
	public static class BoundedIterator<T> implements Iterator<T> {
		private final Iterator<T> iterator;
		private final int maxIters;
		
		private int iter = 0;
		
		public BoundedIterator(Iterator<T> iterator, int maxIters) {
			this.iterator = iterator;
			this.maxIters = maxIters;
		}
		
		@Override
		public boolean hasNext() {
			return this.iterator.hasNext() && this.iter < this.maxIters;
		}

		@Override
		public T next() {
			if(!this.hasNext()) {
				return null;
			} else {
				this.iter++;
				return this.iterator.next();
			}
		}

		@Override
		public void remove() {
			this.iterator.remove();
		}
	}
	
	public static class WrappedIterator implements Iterator<String> {
		private final Iterator<String> iterator;
		private final Wrapper wrapper;
		
		public WrappedIterator(Iterator<String> iterator, Wrapper wrapper) {
			this.iterator = iterator;
			this.wrapper = wrapper;
		}
		
		public boolean hasNext() { return this.iterator.hasNext(); }
		public String next() { return this.wrapper.wrap(this.iterator.next()); }
		public void remove() { this.iterator.remove(); }
	}
	
	public static class WrappedIterable implements Iterable<String> {
		private final Iterable<String> iterable;
		private final Wrapper wrapper;
		
		public WrappedIterable(Iterable<String> iterable, Wrapper wrapper) {
			this.iterable = iterable;
			this.wrapper = wrapper;
		}
		
		public Iterator<String> iterator() {
			return new WrappedIterator(this.iterable.iterator(), this.wrapper);
		}
	}
	
	public static class BoundedIterable<T> implements Iterable<T> {
		private final Iterable<T> iterable;
		private final int maxIters;
		
		public BoundedIterable(Iterable<T> iterable, int maxIters) {
			this.iterable = iterable;
			this.maxIters = maxIters;
		}
		
		@Override
		public Iterator<T> iterator() {
			return new BoundedIterator<T>(this.iterable.iterator(), this.maxIters);
		}
	}
	
	public static class RandomSampleIterator implements Iterator<String> {
		private final Sampler sampler;
		
		public RandomSampleIterator(Sampler sampler) {
			this.sampler = sampler;
		}
		
		public String next() {
			return this.sampler.sample();
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public void remove() {
			throw new RuntimeException("Invalid operation!");
		}
	}
	
	public static class SampleIterable implements Iterable<String> {
		private final Sampler sampler;
		
		public SampleIterable(Sampler sampler) {
			this.sampler = sampler;
		}
		
		@Override
		public Iterator<String> iterator() {
			return new RandomSampleIterator(this.sampler);
		}
	}
	
	public static class DropIterator<T> implements Iterator<T> {
		private final Iterator<T> iterator;
		private final Filter<T> filter;
		
		public DropIterator(Iterator<T> iterator, Filter<T> filter) {
			this.iterator = iterator;
			this.filter = filter;
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public T next() {
			T t = this.iterator.next();
			return this.filter.filter(t) ? t : null;
		}

		@Override
		public void remove() {
			this.iterator.remove();
		}
	}
	
	public static class DropIterable<T> implements Iterable<T> {
		private final Iterator<T> iterator;
		private final Filter<T> filter;
		
		public DropIterable(Iterator<T> iterator, Filter<T> filter) {
			this.iterator = iterator;
			this.filter = filter;
		}
		
		@Override
		public Iterator<T> iterator() {
			return new DropIterator<T>(this.iterator, this.filter);
		}
	}
	
	public static class FilteredIterator<T> implements Iterator<T> {
		private final Iterator<T> iterator;
		private final Filter<T> filter;
		private final Callback callback;
		
		public FilteredIterator(Iterator<T> iterator, Filter<T> filter, Callback callback) {
			this.iterator = iterator;
			this.filter = filter;
			this.callback = callback;
			increment();
		}
		
		private T next;
		
		private void increment() {
			this.next = null;
			while(this.iterator.hasNext()) {
				this.callback.call();
				T t = this.iterator.next();
				if(this.filter.filter(t)) {
					this.next = t;
					break;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return this.next != null;
		}

		@Override
		public T next() {
			T t = this.next;
			increment();
			return t;
		}

		@Override
		public void remove() {
			throw new RuntimeException();
		}
	}
	
	public static class DefaultCallback implements Callback {
		public void call() {}
	}
	
	public static class FilteredIterable<T> implements Iterable<T> {
		private final Iterable<T> iterable;
		private final Filter<T> filter;
		private final Callback callback;
		
		public FilteredIterable(Iterable<T> iterable, Filter<T> filter, Callback callback) {
			this.iterable = iterable;
			this.filter = filter;
			this.callback = callback;
		}
		
		@Override
		public Iterator<T> iterator() {
			return new FilteredIterator<T>(this.iterable.iterator(), this.filter, this.callback);
		}
	}
	
	public static class MutationSampler implements Sampler {
		private final String seed;
		private final int numMutations;
		private final Random random;
		
		public MutationSampler(String seed, int numMutations, Random random) {
			this.seed = seed;
			this.numMutations = numMutations;
			this.random = random;
		}
		
		@Override
		public String sample() {
			return new RandomExtra(this.random).nextStringMutant(this.seed, this.random.nextInt(this.numMutations));
		}
	}
	
	public static class MultiMutationSampler implements Sampler {
		private final MultiRandomSampler samplers;
		public MultiMutationSampler(List<String> seeds, int numMutations, Random random) {
			List<Sampler> samplers = new ArrayList<Sampler>();
			for(String seed : seeds) {
				samplers.add(new MutationSampler(seed, numMutations, random));
			}
			this.samplers = new MultiRandomSampler(samplers, random);
		}
		@Override
		public String sample() {
			return this.samplers.sample();
		}
	}
	
	public static class MultiRandomSampler implements Sampler {
		private final List<Sampler> samplers = new ArrayList<Sampler>();
		private final Random random;
		
		public MultiRandomSampler(Iterable<Sampler> samplers, Random random) {
			for(Sampler sampler : samplers) {
				this.samplers.add(sampler);
			}
			this.random = random;
		}
		
		@Override
		public String sample() {
			int choice = this.random.nextInt(this.samplers.size());
			return this.samplers.get(choice).sample();
		}
	}
	
	public static class IteratorSampler implements Sampler {
		private final Iterable<String> iter;
		private Iterator<String> cur;
		public IteratorSampler(Iterable<String> iter) {
			this.iter = iter;
			this.cur = this.iter.iterator();
		}
		private void ensure() {
			if(!this.cur.hasNext()) {
				this.cur = this.iter.iterator();
			}
			if(!this.cur.hasNext()) {
				throw new RuntimeException();
			}
		}
		public String sample() {
			ensure();
			return this.cur.next();
		}
	}
	
	public static class MultiRoundRobinSampler implements Sampler {
		private final List<Sampler> samplers = new ArrayList<Sampler>();
		private int cur = -1;
		
		public MultiRoundRobinSampler(Iterable<Sampler> samplers) {
			for(Sampler sampler : samplers) {
				this.samplers.add(sampler);
			}
		}
		
		private int getCur() {
			return (++this.cur)%this.samplers.size();
		}
		
		@Override
		public String sample() {
			return this.samplers.get(this.getCur()).sample();
		}
	}
}
