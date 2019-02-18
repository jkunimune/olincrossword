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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The main thing; make and print out a crossword puzzle!
 * 
 * @author Justin Kunimune
 */
public class Main {
	
	private static final int SIZE = 21;
	
	
	public static void main(String[] args) throws IOException {
		final char[][] grid = loadInitialState();
		final Map<Integer, Integer> hist = new LinkedHashMap<Integer, Integer>();
		
		System.out.println(toString(grid));
		
		while (true) {
			int maxL = 0, maxN = 0; // properties of best slot
			int bestI = 0, bestJ = 0; // location of best slot
			int bestAcr = 0; // orientation of best slot
			for (int i = 1; i < grid.length-1; i ++) { // look at all the slots
				for (int j = 1; j < grid[i].length-1; j ++) {
					if (grid[i][j] != '#' && grid[i-1][j] == '#' && grid[i+1][j] != '#') { // across?
						if (!hist.containsKey(0x10000|i<<8|j)) { // make sure we haven't filled this one
							int l = 0, n = 0;
							while (grid[i+l][j] != '#') {
								if (grid[i+l][j] != ' ')
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
					
					if (grid[i][j] != '#' && grid[i][j-1] == '#' && grid[i][j+1] != '#') { // down?
						if (!hist.containsKey(0x00000|i<<8|j)) { // make sure we haven't filled this one
							int l = 0, n = 0;
							while (grid[i][j+l] != '#') {
								if (grid[i][j+l] != ' ')
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
				}
			}
			
			if (maxL == 0) // if we didn't find a single slot to fill
				break;
			
			String word = chooseWord(grid, maxL, bestI, bestJ, bestAcr);
			for (int k = 0; k < maxL; k ++)
				if (bestAcr>0)
					grid[bestI+k][bestJ] = word.charAt(k);
				else
					grid[bestI][bestJ+k] = word.charAt(k);
			hist.put(bestAcr<<16|bestI<<8|bestJ, 0);
			
			System.out.println(toString(grid));
		}
		
		System.out.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	}
	
	
	private static String
			chooseWord(char[][] grid, int len, int i, int j, int across) {
		String x = "";
		for (int k = 0; k < len; k ++)
			x += k;
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO: Handle this
			e.printStackTrace();
		}
		return x;
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
