/**
 * MIT License
 * 
 * Copyright (c) 2018 Justin Kunimune
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package olincrossword;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The main thing; make and print out a crossword puzzle!
 * 
 * @author Justin Kunimune
 */
public class Main {
	
	private static final int SIZE = 21;
	private static final int FRAME_RATE = 4000;
	
	
	public static void main(String[] args) throws IOException {
		final char[][] grid = loadInitialState();
		final String[][] words = loadWords();
		final LinkedHashMap<Integer, Integer> hist = new LinkedHashMap<Integer, Integer>();
		final Set<String> used = new HashSet<String>();
		
		int display = 0;
		int alreadyCheckedTo = 0; // how far we've already looked
		
		while (true) {
			int maxL = 0, maxN = 0; // properties of best slot
			int bestI = 0, bestJ = 0; // location of best slot
			int bestAcr = 0; // orientation of best slot
			for (int i = 1; i < grid.length-1; i ++) { // look at all the slots
				for (int j = 1; j < grid[i].length-1; j ++) {
					if (grid[i][j] != '#' && grid[i-1][j] == '#' && grid[i+1][j] != '#') { // down?
						if (!hist.containsKey(0x00000|i<<8|j)) { // make sure we haven't filled this one
							int l = 0, n = 0;
							while (grid[i+l][j] != '#') {
								if (grid[i+l][j] != ' ')
									n ++; // count the intersections
								l ++; // and the length
							}
							if (n > maxN || (n == maxN && l > maxL)) { // if it has more intersections
								bestI = i; // save it
								bestJ = j;
								bestAcr = 0;
								maxL = l;
								maxN = n;
							}
						}
					}
					
					if (grid[i][j] != '#' && grid[i][j-1] == '#' && grid[i][j+1] != '#') { // across?
						if (!hist.containsKey(0x10000|i<<8|j)) { // make sure we haven't filled this one
							int l = 0, n = 0;
							while (grid[i][j+l] != '#') {
								if (grid[i][j+l] != ' ')
									n ++; // count the intersections
								l ++; // and the length
							}
							if (n > maxN || (n == maxN && l > maxL)) { // if it has more intersections
								bestI = i; // save it
								bestJ = j;
								bestAcr = 1;
								maxL = l;
								maxN = n;
							}
						}
					}
				}
			}
			
			if (maxL == 0) // if we didn't find a single slot to fill
				break;
			
			int choice = -1;
			try {
				choice = chooseWord(maxL, bestI, bestJ, bestAcr, grid, words, used, alreadyCheckedTo); // try to find a word that fits
			}
			catch (RuntimeException e) { // if you can't find one,
				for (int i = 0; i < grid.length; i ++)
					for (int j = 0; j < grid[i].length; j ++)
						grid[i][j] = (grid[i][j] == '#') ? '#' : ' '; // reset the grid
				int afterThis = hist.size();
				for (Integer slot: hist.keySet()) { // recall the history
					afterThis --;
					int across = slot>>16, i = slot>>8&0x0ff, j = slot&0x000ff;
					int l = 0;
					while ((across>0 ? grid[i][j+l] : grid[i+l][j]) != '#')
						l ++;
					if (afterThis > 0) {
						fill(words[l][hist.get(slot)], i, j, across, grid); // and refill all the previous words
					}
					else { // EXCEPT for the last one
						alreadyCheckedTo = hist.get(slot)+1;
						used.remove(words[l][hist.get(slot)]);
						hist.remove(slot); // which ought to be removed (after recording which words won't work)
					}
				}
			}
			if (choice >= 0) { // if you in fact did find a reasonable choice
				String word = words[maxL][choice]; // assuming you succeeded in choosing a word,
				fill(word, bestI, bestJ, bestAcr, grid); // put it in
				hist.put(bestAcr<<16|bestI<<8|bestJ, choice);
				used.add(word); // remember this
				alreadyCheckedTo = 0; // anything is fair game now
			}
			
			if (display == 0) {
				System.out.println(toString(grid));
				display = FRAME_RATE;
			}
			else
				display --;
		}
		
		System.out.println(toString(grid));
		System.out.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	}
	
	
	private static int chooseWord(int len, int i, int j, int across, char[][] grid, String[][] words, Set<String> used, int startAt) throws RuntimeException {
		for (int k = startAt; k < words[len].length; k ++) { // look at our options
			if (used.contains(words[len][k])) { // make sure it hasn't been used
				continue;
			}
			if (!fits(words[len][k], i, j, across, grid)) { // and that it actually fits
				continue;
			}
			return k;
		}
		throw new RuntimeException(String.format("%d,%d %s (%d); from %d", i, j, across>0?"across":"down", len, startAt)); // if you didn't find a single one, cry.
	}
	
	
	private static boolean fits(String word, int i, int j, int across, char[][] grid) {
		for (int l = 0; l < word.length(); l ++) {
			if (across>0) {
				if (word.charAt(l) == '#' && grid[i][j+l] != '#')
					return false;
				if (grid[i][j+l] != ' ' && grid[i][j+l] != word.charAt(l))
					return false;
			}
			else {
				if (word.charAt(l) == '#' && grid[i+1][j] != '#')
					return false;
				if (grid[i+l][j] != ' ' && grid[i+l][j] != word.charAt(l))
					return false;
			}
		}
		return true;
	}
	
	
	private static void fill(String word, int i, int j, int across, char[][] grid) {
		for (int k = 0; k < word.length(); k ++) // fill it in
			if (across>0)
				grid[i][j+k] = word.charAt(k);
			else
				grid[i+k][j] = word.charAt(k);
	}
	
	
	private static char[][] loadInitialState() throws IOException {
		char[][] out = new char[SIZE+2][SIZE+2];
		for (int i = 0; i < SIZE+2; i ++)
			for (int j = 0; j < SIZE+2; j ++)
				out[i][j] = '#';
		
		BufferedReader in = new BufferedReader(new FileReader("res/init.txt"));
		try {
			for (int i = 1; i < SIZE+1; i ++) {
				for (int j = 1; j < SIZE+1; j ++) {
					out[i][j] = (char)in.read();
				}
				in.read();
			}
		} finally {
			in.close();
		}
		return out;
	}
	
	
	private static String[][] loadWords() throws IOException {
		List<List<String>> bins = new ArrayList<List<String>>(SIZE);
		
		for (String wordGroup: new String[] {"olin", "scholarly", "ukacd"}) {
			BufferedReader in = new BufferedReader(new FileReader(String.format("res/%swords.txt", wordGroup)));
			try {
				List<String> wordsFromThisFile = new LinkedList<String>();
				String w;
				while ((w = in.readLine()) != null) // read each word
					wordsFromThisFile.add(w);
				
				Collections.shuffle(wordsFromThisFile); // randomise!
				
				for (String word: wordsFromThisFile) { // now go through and process them in their new order:
					int len = word.split(" ")[0].length(); // get the length (it's not so simple)
					while (len >= bins.size())
						bins.add(new LinkedList<String>()); // make sure its length has a corresponding bin
					bins.get(len).add(word.toUpperCase().replace(' ', '#')); // add it to that bin
				}
			} finally {
				in.close();
			}
		}
		
		String[][] out = new String[bins.size()][]; // finally, convert it all to an array
		for (int i = 0; i < out.length; i ++) {
			System.out.println(bins.get(i).size());
			out[i] = bins.get(i).toArray(new String[0]);
		}
		return out;
	}
	
	
	private static String toString(char[][] arr) {
		String out = "";
		for (char[] row: arr) {
			for (char c: row) {
				out += c;
				out += " ";
			}
			out += "\n";
		}
		return out;
	}
}
