package com.tytanapps.game2048;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class GameModes
{
	public static final int LOAD_GAME_ID = 0;
	public static final int NORMAL_MODE_ID = 1;
	public static final int PRACTICE_MODE_ID = 2;
	public static final int PRO_MODE_ID = 3;
	public static final int ARCADE_MODE_ID = 4;
	public static final int RUSH_MODE_ID = 5;
	public static final int SURVIVAL_MODE_ID = 6;
	public static final int X_MODE_ID = 7;
	public static final int CORNER_MODE_ID = 8;
	public static final int SPEED_MODE_ID = 9;
	public static final int ZEN_MODE_ID = 10;
	public static final int GHOST_MODE_ID = 11;
	public static final int CRAZY_MODE_ID = 12;
    public static final int CUSTOM_MODE_ID = 13;
    public static final int TITLE_MODE_ID = 15;


    public static final int SURVIVAL_MODE_TIME = 15; // seconds

    public static List<Integer> getListOfGameModesIds() {
        List<Integer> gameModes = new ArrayList<Integer>();
        gameModes.add(PRACTICE_MODE_ID);
        gameModes.add(NORMAL_MODE_ID);
        gameModes.add(ARCADE_MODE_ID);
        gameModes.add(RUSH_MODE_ID);
        gameModes.add(X_MODE_ID);
        gameModes.add(CORNER_MODE_ID);
        gameModes.add(GHOST_MODE_ID);
        gameModes.add(SPEED_MODE_ID);
        gameModes.add(SURVIVAL_MODE_ID);
        gameModes.add(CRAZY_MODE_ID);

        return gameModes;
    }

	public static Game newGameFromId(int id)
	{
		switch (id) {
		case NORMAL_MODE_ID:
			return normalMode();
		case PRACTICE_MODE_ID:
			return practiceMode();
		case PRO_MODE_ID:
			return proMode();
		case ARCADE_MODE_ID:
			return arcadeMode();
		case RUSH_MODE_ID:
			return rushMode();
		case SURVIVAL_MODE_ID:
			return survivalMode();
		case X_MODE_ID:
			return XMode();
		case CORNER_MODE_ID:
			return cornerMode();
		case SPEED_MODE_ID:
			return speedMode();
		case ZEN_MODE_ID:
			return zenMode();
		case GHOST_MODE_ID:
			return ghostMode();
        case CRAZY_MODE_ID:
            return crazyMode();
        default:
			return normalMode();
		}
	}

	/**
	 * 
	 * @param id The game mode id
	 * @return The mode name
	 */
	public static int getGameTitleById(int id) {
        switch (id) {
		case NORMAL_MODE_ID:
			return R.string.mode_normal;
		case PRACTICE_MODE_ID:
			return R.string.mode_practice;
		case PRO_MODE_ID:
			return R.string.mode_pro;
		case ARCADE_MODE_ID:
			return R.string.mode_arcade;
		case RUSH_MODE_ID:
			return R.string.mode_rush;
		case SURVIVAL_MODE_ID:
			return R.string.mode_survival;
		case X_MODE_ID:
			return R.string.mode_x;
		case CORNER_MODE_ID:
			return R.string.mode_corner;
		case SPEED_MODE_ID:
			return R.string.mode_speed;
		case ZEN_MODE_ID:
			return R.string.mode_zen;
		case GHOST_MODE_ID:
			return R.string.mode_ghost;
		case CRAZY_MODE_ID:
			return R.string.mode_crazy;
        case CUSTOM_MODE_ID:
            return R.string.mode_custom;
        default:
			return R.string.app_name;
		}
	}

	/**
	 * 
	 * @param id The game mode id
	 * @return The description of the game mode
	 */
	public static int getGameDescById(int id) {

		switch (id) {
		case NORMAL_MODE_ID:
			return R.string.mode_desc_normal;
		case PRACTICE_MODE_ID:
			return R.string.mode_desc_practice;
		case PRO_MODE_ID:
			return R.string.mode_desc_pro;
		case ARCADE_MODE_ID:
			return R.string.mode_desc_arcade;
		case RUSH_MODE_ID:
			return R.string.mode_desc_rush;
		case SURVIVAL_MODE_ID:
			return R.string.mode_desc_survival;
		case X_MODE_ID:
			return R.string.mode_desc_x;
		case CORNER_MODE_ID:
			return R.string.mode_desc_corner;
		case SPEED_MODE_ID:
			return R.string.mode_desc_speed;
		case ZEN_MODE_ID:
			return R.string.mode_desc_zen;
		case GHOST_MODE_ID:
			return R.string.mode_desc_ghost;
		case CRAZY_MODE_ID:
			return R.string.mode_desc_crazy;
		case CUSTOM_MODE_ID:
			return R.string.mode_desc_custom;
		default:
			return -1;
		}
	}

	// Practice Mode
	// Unlimited everything
	public static Game practiceMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setTimeLimit(-1);
		game.setPowerupLimit(-1);
        game.setGenieEnabled(true);
		game.setGameModeId(PRACTICE_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}

	// Normal Mode
	// Unlimited moves and time
	// 10 undos
	public static Game normalMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setTimeLimit(-1);
        game.setUseItemInventory(true);
		game.setGameModeId(NORMAL_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}

	// Pro Mode
	// Unlimited moves and time
	// No undos
	public static Game proMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(0);
		game.setTimeLimit(-1);
		game.setPowerupLimit(0);
		game.setGameModeId(PRO_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}
	// Pro Mode
	// Unlimited moves and time
	// No undos
	public static Game arcadeMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(0);
		game.setTimeLimit(-1);
		game.setArcadeMode(true);
		game.setPowerupLimit(0);
		game.setGameModeId(ARCADE_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}

	// Rush Mode
	// Higher value tiles spawn
	public static Game rushMode()
	{
		Game game = new Game();
		game.setDynamicTileSpawning(true);
		game.setGameModeId(RUSH_MODE_ID);
        game.setUseItemInventory(true);
        game.finishedCreatingGame();

		return game;
	}


	// Survival Mode
	// Unlimited moves and undos
	// Only 30 seconds to play. The time increases when tiles >= 8 combine
	public static Game survivalMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setTimeLimit(SURVIVAL_MODE_TIME);
		game.enableSurvivalMode();
        game.setUseItemInventory(true);
		game.setGameModeId(SURVIVAL_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}

	// XMode
	// Unlimited moves and time
	// 10 undos
	// Places an X on the board that can move but not combine 
	public static Game XMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setTimeLimit(-1);
		game.enableXMode();
		game.setGameModeId(X_MODE_ID);
        game.setUseItemInventory(true);
        game.finishedCreatingGame();

		return game;
	}

	// Corner Mode
	// Unlimited moves and time
	// 10 undos
	// Places immovable pieces in the corners of the board
	public static Game cornerMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setTimeLimit(-1);
		game.enableCornerMode();
		game.setGameModeId(CORNER_MODE_ID);
        game.setUseItemInventory(true);
        game.finishedCreatingGame();

		return game;
	}

	// Speed Mode
	// Unlimited moves and time
	// 10 undos
	// Tiles appear every every 2 seconds even if no move was made
	public static Game speedMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setTimeLimit(-1);
		game.setSpeedMode(true);
        game.setUseItemInventory(true);
		game.setGameModeId(SPEED_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}

	// Zen Mode
	// Unlimited moves, undos and time
	// Every piece can combine

    /*
        Used to debug the game. The game starts with every tile from 2 to 2048
     */
	public static Game zenMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setPowerupLimit(-1);
		game.setTimeLimit(-1);

        //game.setZenMode(true);

        int tile = 2;
        Grid newGameGrid = game.getGrid();
        newGameGrid.clear();
        for(int row = 0; row < game.getGrid().getNumRows(); row++)
            for(int col = 0; col < game.getGrid().getNumCols(); col++) {
                if(tile <= 2048) {
                    newGameGrid.set(new Location(row, col), tile);
                    tile *= 2;
                }
            }

        game.setGameModeId(ZEN_MODE_ID);
        game.finishedCreatingGame();
		
		return game;
	}

	// Crazy Mode
	// Unlimited moves and undos
	// A 5x5 game with every other mode enabled (except zen)
	public static Game crazyMode()
	{
        Game game = new Game(5,5);
		game.setMoveLimit(-1);
		game.setTimeLimit(SURVIVAL_MODE_TIME);
		game.enableSurvivalMode();
		game.enableCornerMode();
		game.enableXMode();
		game.setDynamicTileSpawning(true);
		game.setSpeedMode(true);
        game.setUseItemInventory(true);
		game.setGameModeId(CRAZY_MODE_ID);
        game.finishedCreatingGame();

		return game;
	}
	
	// Ghost Mode
	// All tiles appear as ?
	public static Game ghostMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
        game.setGhostMode(true);
        game.setGameModeId(GHOST_MODE_ID);
        game.setUseItemInventory(true);
        game.finishedCreatingGame();

		return game;
	}

    // Creates 2048
    public static Game createTitleMode()
    {
        Log.d("a", "Entering createTitleMode");

        Game game = new Game();
        game.setUseItemInventory(false);
        game.setPowerupLimit(-1);
        game.setUndoLimit(-1);
        game.setGameModeId(TITLE_MODE_ID);

        Grid grid = new Grid(4,4);
        grid.set(new Location(1,1), 2);
        grid.set(new Location(2,2), 2);
        game.setGrid(grid);
        game.finishedCreatingGame();

        return game;
    }

    // Creates 2048
    public static Location getTitleModeLocation(int turnNumber)
    {
        Log.d("a", "Entering getTitleModeLocation: " + turnNumber);

        switch(turnNumber){
            case 2:
                return new Location(2,0);
            case 3:
                return new Location(1,2);
            case 4:
                return new Location(0,2);
            case 5:
                return new Location(3,0);
            case 6:
                return new Location(0,1);
        }
        return null;
    }

    // Creates 2048
    public static int getTitleValue(int turnNumber)
    {
        Log.d("a", "Entering getTitleValue: " + turnNumber);

        switch(turnNumber){
            case 2:
                return 4;
            case 3:
                return 2;
            case 4:
                return 2;
            case 5:
                return 2;
            case 6:
                return -22;
        }
        return -1;
    }

    /*

    | | | | |
    | |2| | |
    | | |2| |
    | | | | |

    | |2|2| |
    | | | | |
    |4| | | |
    | | | | |

    | | | |4|
    | | |2| |
    | | | |4|
    | | | | |

    | | |2| |
    | | | | |
    | | | | |
    | | |2|8|

    | | |4|8|
    | | | | |
    | | | | |
    |2| | | |

    |2| |4|8|
    | | | | |
    | | | | |
    | | | | |

    |2|0|4|8|
    | | | | |
    | | | | |
    | | | | |

    */
}
