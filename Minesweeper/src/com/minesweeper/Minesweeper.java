package com.minesweeper;


import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/******************************************************************************
 *  Compilation:  javac Minesweeper.java
 *  Execution:    java Minesweeper m n p
 *
 *  Creates an MxN minesweeper game where each cell is a bomb with
 *  probability p. Prints out the m-by-n game and the neighboring bomb
 *  counts.
 *
 *  Tien-Ping Tan Modification:
 *  Modified from the original code at http://introcs.cs.princeton.edu/java/14array/Minesweeper.java.html
 *  to include 2 extra columns at the left and right, and also 2 extra rows top and bottom.
 *  Reason? You are allowed to "open up" all "0" squares and their surrounding.
 *  Just like when you play the game interactively, when you click on a "0", it will open up
 *  the surrounding for you. Here, we open up all "0" to you, so you have something to start with
 *  when solving the puzzle.
 *  NOTE:
 *  1. Use the gameMap[][] for solving the puzzle.
 *  2. Use the mineMap[][] for reference.
 *  3. Use the openSquare() to open up a square.
 *
 *  Sample execution:
 *
 *      % java Minesweeper  5 10 0.3
 *      * . . . . . . . . *
 *      . . . . . . * . . .
 *      . . . . . . . . * *
 *      . . . * * * . . * .
 *      . . . * . . . . . .
 *
 *      * 1 0 0 0 1 1 1 1 *
 *      1 1 0 0 0 1 * 2 3 3
 *      0 0 1 2 3 3 2 3 * *
 *      0 0 2 * * * 1 2 * 3
 *      0 0 2 * 4 2 1 1 1 1
 *
 *
 ******************************************************************************/

public class Minesweeper {

    public final int MINE=9;
    public final int CLOSE=-1;
    public final int BLANK=0;
    public final int FLAG=-2;

    private int[][] mineMap;
    private int[][] gameMap;
    private TreeSet<String> mineList;
    private int[][] board = null;
    private boolean[][] flagMine = null;
    private boolean[][] empty = null;
    private ArrayList<boolean[]> solutions;
    boolean optimization;
    private long timestart;
    public Minesweeper() {

    }


    /**
     * Load minemap from file
     * @param filename
     */
    public Minesweeper(String filename) {


        mineMap = loadMineMap(filename);
        printMineMap();

        gameMap = createGameMap(mineMap);
        printGameMap();

        mineList = getAllMineLocation(mineMap);
        System.out.println("Total mines: " + mineList.size());
    }

    /**
     * Generate a random mine map. Mine is tag with the letter 9.
     * @param m row
     * @param n columns
     * @param p probability of mine. If m=10,n=10, total size=100. p=0.1, so total mines = 10.
     * Special value in the map: 9 => mine, -1 => close
     * @return
     */
    public int[][] generateMineMap(int m, int n, double p) {
        //int m = Integer.parseInt(args[0]);
        //int n = Integer.parseInt(args[1]);
        //double p = Double.parseDouble(args[2]);

        // game grid is [1..m][1..n], border is used to handle boundary cases
        boolean[][] bombs = new boolean[m+2][n+2];
        int[][] mineMap = new int[m+4][n+4];

        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                bombs[i][j] = (Math.random() < p);

        // print game
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++)
                if (bombs[i][j]) System.out.print("* ");
                else             System.out.print(". ");
            System.out.println();
        }

        // sol[i][j] = # bombs adjacent to cell (i, j)
        int[][] sol = new int[m+2][n+2];
        for (int i = 0; i <= m+1; i++)
            for (int j = 0; j <= n+1; j++)
                // (ii, jj) indexes neighboring cells
                for (int ii = i - 1; ii <= i + 1; ii++)
                    for (int jj = j - 1; jj <= j + 1; jj++)
                        if (ii>=0 && jj>=0 && ii<m+2 && jj<n+2 && bombs[ii][jj]) sol[i][j]++;

        // print solution
        System.out.println();
        for (int i = 0; i <= m+1; i++) {
            for (int j = 0; j <= n+1; j++) {
                if (bombs[i][j]){
                    //System.out.print("* ");
                    mineMap[i+1][j+1] = 9;
                } else{
                    mineMap[i+1][j+1] = sol[i][j];
                    //System.out.print(sol[i][j] + " ");
                }
            }
            System.out.println();
        }

        //printMap(mineMap);

        return mineMap;

    }

    public int[][] createGameMap(int[][] mineMap){

        int m = mineMap.length;
        int n = mineMap[0].length;
        int[][] gameMap = new int[m][n];

        //initialize game map, close all square
        for(int i=0; i<m; i++){
            for(int j=0; j<n; j++){
                //Set the square to close
                gameMap[i][j] = CLOSE;
            }
        }




        //open all square=0
        for(int i=0; i<m; i++){
            for(int j=0; j<n; j++){
                if (mineMap[i][j] == BLANK)
                    openSquare(i, j, gameMap);
            }
        }

        //uncomments for printout and verify
        //printMap(gameMap);

        return gameMap;
    }

    /**
     * Open a square in the map. If the square is a blank, it will open up neighboring squares.
     * @param x
     * @param y
     * @return if return true, it is not a mine. If it is false, you open up a mine!
     */

    public void setMineMap(int[][] mineMap, TreeSet<String> mineList) {
        this.mineMap = mineMap;
        this.mineList= mineList;
    }
    public void setGameMap(int[][] gameMap) {
        this.gameMap = gameMap;
        // this.oriMineList=oriMineList;
    }
    private boolean openSquare(int x, int y, int[][] gameMap){

        if (gameMap[x][y] == CLOSE && gameMap[x][y]!=FLAG){

            //open the square
            gameMap[x][y] = mineMap[x][y];


            if (gameMap[x][y] == BLANK ){
                //recursively open the neighboring squares
                for(int i=x-1; i<=x+1; i++){
                    for(int j=y-1; j<=y+1; j++){
                        if (i>=0 && j>=0 && i<mineMap.length && j<mineMap[0].length){
                            openSquare(i, j, gameMap);
                        }
                    }
                }
            } else if (gameMap[x][y] != MINE){
                //continue the game
                return true;
            } else{
                System.out.println("You lost");
                System.exit(0);
                //you open up a mine!!!
                return false;
            }
        } else{
            //the square already open. Do nothing
            return true;
        }

        return true;
    }



    public void saveMineMap(int[][] mineMap, String filename) {
        try {
            FileWriter fw = new FileWriter(filename);

            //keep the row and column information. We need it when we are loading it.
            fw.write(mineMap.length + " " + mineMap[0].length + "\n");

            for (int i = 0; i < mineMap.length; i++) {
                for (int j = 0; j < mineMap[i].length; j++) {
                    fw.write(mineMap[i][j] + "\t");
                }
                fw.write("\n");
            }

            fw.close();
        } catch(IOException e) {
            System.out.println(e);
        }
    }

    public void printGameMap() {
        System.out.println("GAME MAP");
        printMap(gameMap);
        System.out.println();
    }

    public void printMineMap() {
        System.out.println("MINE MAP");
        printMap(mineMap);
        System.out.println();
    }

    public void printMap(int[][] mineMap) {

        for (int i = 0; i < mineMap.length; i++) {
            for (int j = 0; j < mineMap[i].length; j++) {
                System.out.print(mineMap[i][j] + "\t");
            }
            System.out.println();
        }
    }

    public TreeSet<String> getAllMineLocation(int[][] mineMap) {

        TreeSet<String> mineList = new TreeSet<String>();

        for (int i = 0; i < mineMap.length; i++) {
            for (int j = 0; j < mineMap[i].length; j++) {
                if (mineMap[i][j] == MINE) {
                    String mine = i + " " + j;
                    mineList.add(mine);
                    //System.out.println(i + " " + j);
                }
            }
        }

        return mineList;
    }


    protected Vector<String> extractWords(String sentence) {
        Vector<String> words = new Vector<String>();
        String exp = "\\S+";

        //floating point
        if (sentence != null) {
            Pattern p = Pattern.compile(exp);
            Matcher m = p.matcher(sentence);

            //we only need the first 3 strings
            while (m.find()) {
                words.add(sentence.substring(m.start(), m.end()));
            }
        }

        return words;
    }


    public int[][] loadMineMap(String filename) {

        int[][] mineMap=null;

        try{
            FileReader fr = new FileReader(filename);
            LineNumberReader lnr = new LineNumberReader(fr);

            String line = lnr.readLine();
            int pos = line.indexOf(" ");
            int m = Integer.parseInt(line.substring(0, pos));
            int n = Integer.parseInt(line.substring(pos+1));

            //initialize mineMap
            mineMap = new int[m][n];

            line = lnr.readLine();
            int mCounter = 0;

            while(line != null) {

                Vector<String> words = extractWords(line);

                for(int i=0; i<words.size(); i++) {
                    mineMap[mCounter][i] = Integer.parseInt(words.get(i));
                }
                mCounter++;
                line = lnr.readLine();
            }


            lnr.close();
            fr.close();

        } catch(IOException e) {
            System.out.println(e);
        }



        return mineMap;
    }


    private void solution() {
        timestart = System.currentTimeMillis();
        int m = mineMap.length;
        int n = mineMap[0].length;
        int run = 0;
        boolean tank = false;

        for (int repeat = 1; repeat <= 100; repeat++) {

            checkSolve();

            if (run < 3) {
                run++;
            } else {
                if (!tank) {
                    tankAlgorithms();
                    tank = true;
                } else {

                }
            }
//            }

            for (int x = 0; x < m; x++) {
                for (int y = 0; y < n; y++) {
                    if (gameMap[x][y] > 0) {
                        solveEach(x, y);
                    }
                }
            }
        }
    }
    // return if won
    private boolean checkSolve() {
        int m = mineMap.length;
        int n = mineMap[0].length;
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {

                if (gameMap[x][y] == CLOSE) return false;

            }
        }
        long time = System.currentTimeMillis() - timestart;

        System.out.printf(" Congratulations you have solved the game.\n Time run (%dms)\n", time);
        System.exit(0);

        return true;
    }
    private void solveEach( int x, int y) {

        int countClosed = getSurroundType(x, y, CLOSE);
        if (countClosed == 0) return;

        int countAlreadyFlagged = getSurroundType(x, y, FLAG);
        int countMinesAround = gameMap[x][y];

        // First: flag as much as we can
        if (countMinesAround == countClosed + countAlreadyFlagged) {
            flagSurround(x, y);

            countAlreadyFlagged = getSurroundType(x, y, FLAG);
        }

//        // Second: open the ones around
        if (countMinesAround == countAlreadyFlagged) {

            openSurround(x,y);
            printGameMap();
        }
    }
    private int getSurroundType( int x, int y, int z) {
        int hits = 0;
        // top ■□□
        if (y > 0) {
            if (x > 0 && gameMap[x - 1][y - 1] == z) hits++;
            if (gameMap[x][y - 1] == z) hits++;
            if (x < mineMap.length - 1 && gameMap[x + 1][y - 1] == z) hits++;
        }
        // middle ■□□
        if (x > 0 && gameMap[x - 1][y] == z) hits++;
        if (x < mineMap.length - 1 && gameMap[x + 1][y] == z) hits++;
        // bottom ■□□
        if (y < mineMap[0].length - 1 ) {
            if (x > 0 && gameMap[x - 1][y + 1] == z) hits++;
            if (gameMap[x][y + 1] == z) hits++;
            if (x < mineMap[0].length - 1 && x>=0 && gameMap[x + 1][y + 1] == z) hits++;
        }

        return hits;

    }
    public void flagSurround( int x, int y) {
        // top ■□□
        if (y > 0) {
            if (x > 0)  tagMine(x - 1, y - 1);
            tagMine(x, y - 1);
            if (x < mineMap.length - 1 && x>=0)  tagMine(x + 1, y - 1);
        }
        // middle ■□□
        if (x > 0)  tagMine(x - 1, y);
        if (x + 1 < mineMap.length - 1 ) tagMine(x + 1, y);

        // bottom ■□□
        if (y + 1 < mineMap.length - 1 ) {
            if (x > 0)  tagMine(x - 1, y + 1);
            tagMine(x, y + 1);
            if (x < mineMap.length- 1 ) tagMine(x + 1, y + 1);
        }
    }
    public void openSurround( int x, int y) {
        //top
        if (y > 0) {
            if (x > 0)  openSquare(x - 1, y - 1,gameMap);
            openSquare(x, y - 1,gameMap);
            if (x < mineMap.length - 1 && x>=0)  openSquare(x + 1, y - 1,gameMap);
        }
        // middle ■□□
        if (x > 0)  openSquare(x - 1, y,gameMap);
        if (x + 1 < mineMap.length - 1 ) openSquare(x + 1, y,gameMap);

        // bottom ■□□
        if (y + 1 < mineMap.length - 1 ) {
            if (x > 0)  openSquare(x - 1, y + 1,gameMap);
            openSquare(x, y + 1,gameMap);
            if (x < mineMap.length- 1 ) openSquare(x + 1, y + 1,gameMap);
        }
    }
    //Tank algorithms: backtrack solution
    // Brute force used if false


    private void tankAlgorithms(){


        ArrayList<Point> borderTiles = new ArrayList<>();
        ArrayList<Point> blankTiles = new ArrayList<>();


        borderTiles = getPoints(borderTiles, blankTiles);
        if (borderTiles == null) return;
        if (segregateBorderTiles(borderTiles)) return;

    }


    // Get list of full tiles
    private ArrayList<Point> getPoints(ArrayList<Point> borderTiles, ArrayList<Point> blankTiles) {
        int m = mineMap.length;
        int n = mineMap[0].length;
        // optimisation not run if only few tiles
        optimization = false;
        for (int x = 0; x < m; x++)
            for (int y = 0; y < n; y++)
                if (gameMap[x][y] == CLOSE && gameMap[x][y] != FLAG) blankTiles.add(new Point(x, y));

        // Add all border tiles
        for (int x = 0; x < m; x++)
            for (int y = 0; y < n; y++)
                if (isBorder(x, y) && gameMap[x][y] != FLAG) borderTiles.add(new Point(x, y));

        // Count tiles outside range , 8 = limit for brute force
        int countTilesOutsideRange = blankTiles.size() - borderTiles.size();
        if (countTilesOutsideRange > 8) {
            optimization = true;
        } else borderTiles = blankTiles;


        // return if something went wrong
        if (borderTiles.size() == 0) return null;
        return borderTiles;
    }
    // segregateBorderTiles before running recursive
    // endgame, stop segregateBorderTiles as it may miss some mine
    private boolean segregateBorderTiles(ArrayList<Point> borderTiles) {

        int m = mineMap.length;
        int n = mineMap[0].length;
        ArrayList<ArrayList<Point>> segregate;
        if (!optimization) {
            segregate = new ArrayList<>();
            segregate.add(borderTiles);
        } else segregate = tilesSegregate(borderTiles);

        for (int tileId = 0; tileId < segregate.size(); tileId++) {
            // Copy all into temp
            solutions = new ArrayList<>();
            board = gameMap.clone();

            flagMine = new boolean[m][n];
            for (int x = 0; x < m; x++) {
                for (int y = 0; y < n; y++) {
                    flagMine[x][y] = gameMap[x][y] == FLAG;
                }
            }

            empty = new boolean[m][n];
            for (int x = 0; x < m; x++) {
                for (int y = 0; y < n; y++) {
                    empty[x][y] = board[x][y] >= 0;
                }
            }


            recursive(segregate.get(tileId), 0);

            // Something screwed up
            if (solutions.size() == 0) return true;


            // Check for solved squares
            for (int i = 0; i < segregate.get(tileId).size(); i++) {
                boolean mines = true,
                        allOpen = true;
                for (boolean[] solution : solutions) {
                    if (!solution[i]) mines = false;
                    if (solution[i]) allOpen = false;
                }

                Point tile = segregate.get(tileId).get(i);

                if (mines) tagMine(tile.x, tile.y);
                else if (allOpen) {
                    openSquare(tile.x, tile.y,gameMap);
                    printGameMap();
                }
            }
        }
        return false;
    }



    //segregateBorderTiles only if 2 regions are independent to each other
    private ArrayList<ArrayList<Point>> tilesSegregate(ArrayList<Point> borderTiles) {


        ArrayList<ArrayList<Point>> regions = new ArrayList<>();
        ArrayList<Point> list = new ArrayList<>();

        while (true) {

            LinkedList<Point> linkedList = new LinkedList<>();
            ArrayList<Point> lastRegion = new ArrayList<>();

            // Find start point
            for (Point region : borderTiles) {
                if (!list.contains(region)) {
                    linkedList.add(region);
                    break;
                }
            }

            if (linkedList.isEmpty()) break;

            while (!linkedList.isEmpty()) {

                Point tile = linkedList.poll();
                lastRegion.add(tile);
                list.add(tile);
                connectingTiles(borderTiles, linkedList, lastRegion, tile);

            }

            regions.add(lastRegion);

        }

        return regions;

    }
    // Find connecting tiles
    private void connectingTiles(ArrayList<Point> borderTiles, LinkedList<Point> linkedList, ArrayList<Point> lastRegion, Point tile) {
        int m = mineMap.length;
        int n = mineMap[0].length;
        for (Point compareTile : borderTiles) {

            boolean isConnect = false;

            if (lastRegion.contains(compareTile)) continue;

            if (Math.abs(tile.x - compareTile.x) > 2 || Math.abs(tile.y - compareTile.y) > 2) isConnect = false;
            else {
                // search the tiles
                tileSearch: for (int x = 0; x < m; x++) {
                    for (int y = 0; y < n; y++) {
                        if (gameMap[x][y] > 0) {
                            if (Math.abs(tile.x - x) <= 1 && Math.abs(tile.y - y) <= 1 &&
                                    Math.abs(compareTile.x - x) <= 1 && Math.abs(compareTile.y - y) <= 1) {
                                isConnect = true;
                                break tileSearch;
                            }
                        }
                    }
                }
            }

            if (isConnect) {
                if (!linkedList.contains(compareTile)) linkedList.add(compareTile);
            }

        }
    }


    void recursive(ArrayList<Point> borderTiles, int level) {


        int countflag = 0;
        int m = mineMap.length;
        int n = mineMap[0].length;
        for (int x = 0; x < m; x++)
            for (int y = 0; y < n; y++) {

                // Count flags for endgame
                if (flagMine[x][y]) countflag++;

                int currentTiles = board[x][y];
                if (currentTiles < 0) continue;

                // Total tiles border
                int countTilesBorder;
                if ((x == 0 && y == 0) || (x == m - 1 && y == n - 1)) countTilesBorder = 3;
                else if (x == 0 || y == 0 || x == m - 1 || y == n - 1) countTilesBorder = 5;
                else countTilesBorder = 8;

                // Situation: Multiple empty or multiple mines around
                if ((countTilesBorder - countFlagsSurroundingTiles(empty, x, y) < currentTiles) ||
                        (countFlagsSurroundingTiles(flagMine, x, y) > currentTiles)) return;

            }
        // Flags more than the original mine list size
        if (countflag > mineList.size()) return;

        // Problems solve
        if (level == borderTiles.size()) {

            if (!optimization && countflag < mineList.size()) return;

            boolean[] solution = new boolean[borderTiles.size()];
            for (int i = 0; i < borderTiles.size(); i++) {
                Point tile = borderTiles.get(i);
                solution[i] = flagMine[tile.x][tile.y];
            }
            solutions.add(solution);
            return;
        }

        Point tile = borderTiles.get(level);

        // Recursion for flag and no flag
        flagMine[tile.x][tile.y] = true;
        recursive(borderTiles, level + 1);
        flagMine[tile.x][tile.y] = false;

        empty[tile.x][tile.y] = true;
        recursive(borderTiles, level + 1);
        empty[tile.x][tile.y] = false;

    }

    //count flags surrounding the tiles
    private int countFlagsSurroundingTiles(boolean[][] array, int x, int y) {
        int mines = 0;
        //top
        if (y > 0) {
            if (x > 0 && array[x - 1][y - 1]) mines++;
            if (array[x][y - 1]) mines++;
            if (x < array.length - 1 && array[x + 1][y - 1]) mines++;
        }
        //middle
        if (x > 0 && array[x - 1][y]) mines++;
        if (x < array.length - 1 && array[x + 1][y]) mines++;

        //bottom
        if (y < array[0].length - 1) {
            if (x > 0 && array[x - 1][y + 1]) mines++;
            if (array[x][y + 1]) mines++;
            if (x < array.length - 1 && array[x + 1][y + 1]) mines++;
        }

        return mines;
    }

    //find all border tiles: unopen square with open square next to it
    private boolean isBorder(int x, int y) {
        int m = mineMap.length;
        int n = mineMap[0].length;
        if (gameMap[x][y] != CLOSE) return false;

        //top
        if (y > 0) {
            if (x > 0 && gameMap[x - 1][y - 1] >= 0) return true;
            if (gameMap[x][y - 1] >= 0) return true;
            if (x < m - 1 && gameMap[x + 1][y - 1] >= 0) return true;
        }
        //middle
        if (x > 0 && gameMap[x - 1][y] >= 0) return true;
        if (x < m - 1 && gameMap[x + 1][y] >= 0) return true;

        //bottom
        if (y < n - 1) {
            if (x > 0 && gameMap[x - 1][y + 1] >= 0) return true;
            if (gameMap[x][y + 1] >= 0) return true;
            if (x < m - 1 && gameMap[x + 1][y + 1] >= 0) return true;
        }
        return false;
    }

    public boolean tagMine(int i, int j) {

        String mine = new String(i + " " + j);
        if(gameMap[i][j]==CLOSE && gameMap[i][j]!=FLAG){
            if(mineList.contains(mine)){
                mineList.remove(mine);
                System.out.println("CORRECT ANSWER!");
                System.out.println("Number of mines: " + mineList.size());
                gameMap[i][j]=FLAG;
                printGameMap();
                return true;
            }
            else {
                System.out.println("WRONG ANSWER!");
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args){

        Minesweeper m = new Minesweeper();
        int[][] mineMap = m.generateMineMap(10, 10, 0.2);
        var mineList = m.getAllMineLocation(mineMap);
        m.setMineMap(mineMap, mineList);
        int[][] gameMap = m.createGameMap(mineMap);
        m.setGameMap(gameMap);
        m.saveMineMap(mineMap, "minemap.txt");
        System.out.println("MINE MAP");
        m.printMap(mineMap);
        System.out.println("GAME MAP");
        m.printMap(gameMap);

        //For testing, you may want to generate the map once, and save & load it again later
//        Minesweeper m = new Minesweeper("minemap.txt");
        //m.printGameMap();
        System.out.println("*************");

        m.solution();
        //boolean flag = m.openSquare(3, 8);
        //System.out.println(flag);
        //m.printGameMap();
    }
}
