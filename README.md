

# Pineapple Distance
## About
Pineapple Distance is a Windows-only web crawler (rudimentary search engine) which uses multiple threads and can save/resume its progress on incomplete searches. It takes the input of a search term, and outputs that term's "pineapple distance" (i.e. how many pages did the program have to traverse to find that term on a page, starting from Wikipedia's Pineapple page) and what page the term was found on.

If you enter 'c', the program will instead continue where it left off with your previous search. This only works if your last use of the program could not find your query in its 10 pages searched. If you try to continue before running it at all, after a completed search, or after quitting the program prematurely, it will search instead for the letter C and find it pretty quick.

This program implements Java's IO library to its search progress, uses the java.net library to retrieve HTML data from each given page and makes use of its regex search functions to look for the search term and for links, and uses threads to look at multiple pages at once in an arbitrary order. It keeps an ongoing "queue" of pages to look at, but throws out old terms so that the program traverses more distance and looks at more unrelated pages (since there's only a certain range of terms that will appear on Pineapple and its immediate relatives - also wanted each run of the program to be somewhat unique, even given the same term).

Check out `Pineapple_Distance.pdf` for more info and examples.

## How to Run
1. Run file `pineapple-distance-1.0.exe`. Give it permissions to install if needed.
2. Once it is installed, navigate to `C:\Program Files\pineapple-distance`, or wherever programs are automatically installed on your machine.
3. Right-click `pineapple-distance.exe` and select RUN AS ADMINISTRATOR. (You MUST do this EVERY TIME you open the program, or it will not be able to read/write its save data and will have problems.)
4. The program should now be running in a console window. Enter your search term when prompted (or just `c` if you would like to continue an unfinished search) and let it work its magic!

### Alternate Run Method (Developer)
- If you cannot run this program via the installer/exe, you can instead run it directly through the JDK
1. Make sure that JDK is installed & on your PATH. This program was written with [version 21.0.1](https://www.oracle.com/java/technologies/downloads/#jdk21-windows).
2. Use Windows Powershell and navigate into the `src` directory of the project.
3. You should not need to compile, but if the next part doesn't work, you can run command `javac Main.java`
4. Run command `java Main`

## Additional Notes
- This program is made to run on a **Windows** OS.
- Quitting the program randomly may lead to issues with the save/continue functionality, since you might interrupt the read/write processes. If you would like to use the save/continue functions, please wait for the program to quit on its own.
- The program only searches 10 pages at a time. It waits 1 second between searches so that Wikipedia doesn't start rejecting requests. If your term is not found on 10 pages, you can run the program again and enter 'c' as your term, which will continue where you've left off (if your last run of the program didn't already find a term).
  
Terms to try (you can do any word you like, but these tend to be found within a few runs, while not immediately being on the Pineapple page or being so obscure it takes too long. Try a given term multiple times to see the different paths the program might take to find it!):
- distance
- creek
- crystal
- You can just enter a keysmash to see what happens when it can't find a term / test the save and continue functionality. You can keep re-opening and continuing the search indefinitely.