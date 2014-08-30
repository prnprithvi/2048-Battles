package com.tytanapps.game2048;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.Events;
import com.google.android.gms.games.quest.Quests;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.basegameutils.GameHelper;
import com.tytanapps.game2048.R;
import com.tytanapps.game2048.MainApplication.TrackerName;
import com.tytanapps.game2048.R.array;
import com.tytanapps.game2048.R.drawable;
import com.tytanapps.game2048.R.id;
import com.tytanapps.game2048.R.layout;
import com.tytanapps.game2048.R.menu;
import com.tytanapps.game2048.R.string;

import junit.framework.Assert;
import android.R.color;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodSession.EventCallback;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridLayout.Spec;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.preference.PreferenceManager;

public class GameActivity extends BaseGameActivity implements OnGestureListener {
	
	final static String LOG_TAG = GameActivity.class.getSimpleName();
	
	// The time in milliseconds for the animation
	public static final long SHUFFLE_SPEED = 300;
	public static final long NEW_TILE_SPEED = 300;
	public static final long TILE_SLIDE_SPEED = 300;
	
	private static boolean boardCreated = false;
	private static Game game;
	
	GridLayout gridLayout;
	
	// Warns you about making a making a move that may
	// lose you the game
	private static boolean genie_enabled = false;
	private boolean XTileAttackActive = false;
	private boolean ghostAttackActive = false;
	
	// Used to detect swipes and move the board
	private GestureDetectorCompat mDetector; 
	
	String appUrl = "https://play.google.com/store/apps/details?id=com.tytanapps.game2048";
	
	// Becomes false when the game is moved and becomes true in onDown
	boolean listenForSwipe = true;
	
	boolean animationInProgress = false;
	boolean gameLost = false;
	
	// This keeps track of the active animations and
	// stops them in onStop
	private ArrayList<ObjectAnimator> activeAnimations
		= new ArrayList<ObjectAnimator>();
	
	// Stores info about the game such as high score
	private static Statistics gameStats;
	
	// The distance in pixels between tiles
	private static int verticalTileDistance = 0;
	private static int horizontalTileDistance = 0;
	
	ShareActionProvider mShareActionProvider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new GameFragment()).commit();
		}
		
		// Start listening for swipes
		mDetector = new GestureDetectorCompat(this,this);

		// Google Analytics
		((MainApplication) getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);
		Tracker t = ((MainApplication) getApplication()).getTracker(TrackerName.APP_TRACKER);
		// Set screen name.
		t.setScreenName("Game Activity");
		// Send a screen view.
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		
		// Locate MenuItem with ShareActionProvider
	    MenuItem item = menu.findItem(R.id.menu_item_share);

	    // Fetch and store ShareActionProvider
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();
		createShareIntent();
	    
		return true;
	}
	
	// Call to update the share intent
	private void createShareIntent() {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT,
				"I am playing 2048. My high score is " + gameStats.highScore
				+ ". Try to beat me! " + appUrl);
		shareIntent.setType("text/plain");
		if (mShareActionProvider != null) {
	        mShareActionProvider.setShareIntent(shareIntent);
	    }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// When the how to play menu item is pressed switch to InfoActivity
		if (id == R.id.action_how_to_play) {
			Intent showInfo = new Intent(this, com.tytanapps.game2048.InfoActivity.class);
			startActivity(showInfo);
			return true;
		}
		// When the settings menu item is pressed switch to SettingsActivity
		if(id == R.id.action_settings) {
			Intent showSettings = new Intent(this, com.tytanapps.game2048.SettingsActivity.class);
			startActivity(showSettings);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		
		// If GameActivity is loaded for the first time the grid is created. If user returns to
		// this activity after switching to another activity, the grid is still recreated because
		// there is a chance that android killed this activity in the background
		boardCreated = false;
		
		gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Load the saved file containing the game. This also updates the screen.
		load();
		
		// Show the game mode in the menu bar
		int gameTitleId = GameModes.getGameTitleById(game.getGameModeId()); 
		if(gameTitleId != -1)
			getActionBar().setTitle(gameTitleId);
		
		// Disable the undo button if there are no undos remaining
		Button undoButton = ((Button) findViewById(R.id.undo_button));
		undoButton.setEnabled(game.getUndosRemaining() != 0);
		if (game.getUndosRemaining() == 0)
			undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
		else
			undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button));
		
		Button powerupButton = ((Button) findViewById(R.id.powerup_button));
		powerupButton.setEnabled(game.getPowerupsRemaining() != 0);
		
		if(game.getPowerupsRemaining() == 0) {
			powerupButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button_disabled));
		}
		
		powerupButton.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					v.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button_selected));
				}
				else if (event.getAction() == MotionEvent.ACTION_UP) {
					v.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button));
					showPowerupDialog();
				}
				return true;
			}
		});

		GoogleAnalytics.getInstance(this).reportActivityStart(this);

		super.onStart();	
	}
	
	@Override
	protected void onStop() {
		// Only save a game that is still in progress
		if(! game.lost())
			save();
		
		// Stop all active animations. If this is not done the game will crash
		for(ObjectAnimator animation : activeAnimations)
			animation.end();
		
		animationInProgress = false;
		
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
		
		super.onStop();
		
	}
	
	/**
	 * Moves all of the tiles
	 * @param direction Should use the static variables in Location class
	 */
	public void act(int direction) {
		animationInProgress = true;
		
		// If the ice attack is active in that direction do not move
		if((game.getAttackDuration() > 0 && game.getIceDirection() == direction) || gameLost) {
			animationInProgress = false;
			return;
		}
		
		if(genie_enabled && game.causeGameToLose(direction)) {
			animationInProgress = false;
			warnAboutMove(direction);
			return;
		}
		
		calculateDistances();
		int highestTile = game.highestPiece();
		
		// Load the speed to move the tiles from the settings
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				
		int speed = Integer.valueOf(prefs.getString("speed", "200"));
		
		// Save the game history before each move
		game.saveGameInHistory();
		
		// Get a list of all tiles
		List<Location> tiles = game.getGrid().getLocationsInTraverseOrder(direction);
		
		// An list of the move animations to play
		ArrayList<ObjectAnimator> translateAnimations = new ArrayList<ObjectAnimator>();
		
		// Loop through each tile
		for(Location tile : tiles) {
			// Determine the number of spaces to move
			int distance = game.move(tile, direction);
			
			// Only animate buttons that moved
			if(distance > 0) {
				
				if(direction == Location.LEFT || direction == Location.UP)
					distance *= -1;
				
				ImageView movedTile = findTileByLocation(tile);
				
				// The tag is changed to a value different than its actual value
				// which causes it to be updated in updateGrid
				movedTile.setTag(-10);
				
				// Determine the distance to move in pixels
				ObjectAnimator animation;
				if(direction == Location.LEFT || direction == Location.RIGHT) {
					distance *= horizontalTileDistance;
					animation = ObjectAnimator.ofFloat(movedTile, View.TRANSLATION_X, distance);
				}
				else {
					distance *= verticalTileDistance;
					animation = ObjectAnimator.ofFloat(movedTile, View.TRANSLATION_Y, distance);
				}
				
				// Time in milliseconds to move the tile
				animation.setDuration(speed);
				
				// Add the new animation to the list
				translateAnimations.add(animation);
			}
		}
		
		if(translateAnimations.isEmpty()) {
			animationInProgress = false;
			
			if(game.lost())
				lost();
				
			return;
		}
		
		translateAnimations.get(0).addListener(new AnimatorListener(){
			@Override
			// When the animation is over increment the turn number, update the game, 
			// and add a new tile
			public void onAnimationEnd(Animator animation) {
				
				updateGame();
				
				gameStats.totalMoves += 1;
				activeAnimations.clear();
				animationInProgress = false;
				
				if(game.getArcadeMode() && Math.random() < 0.1)
					addRandomBonus();
				
				if(XTileAttackActive && game.getAttackDuration() == 0)
					endXAttack();
				
				if(ghostAttackActive && game.getAttackDuration() == 0)
					endGhostAttack();
				
				game.newTurn();
				addTile();
			}
			
			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Animation cancelled");
				animationInProgress = false;
			}
			@Override
			public void onAnimationRepeat(Animator animation) { }
		});
		
		// Move all of the tiles
		for(ObjectAnimator animation: translateAnimations) {
			animation.start();
			activeAnimations.add(animation);
		}
		
		if(game.highestPiece() > highestTile && game.getGameModeId() == GameModes.NORMAL_MODE_ID)
			if(game.highestPiece() >= 128)
				unlockAchievementNewHighestTile(game.highestPiece());
	}
	
	private void addRandomBonus() {
		double rand = Math.random();
		String item = null;
		
		// 16% ice attack, 17% ghost attack, 17% XTile attack
		// 25% +1 undo,	25% +1 powerup
		if(rand < .5 && game.getAttackDuration() <= 0) {
			if(rand < .16) 
				ice();
			else
				if(rand < .33) 
					ghostAttack();
				else
					XTileAttack();
			updateTextviews();
		}
		else {
			if(rand < 0.75) {
				game.incrementUndosRemaining();
				Button undoButton = (Button) findViewById(R.id.undo_button);
				undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button));
				undoButton.setEnabled(true);
				item = "Undo";
			}
			else {
				game.incrementPowerupsRemaining();
				Button powerupButton = (Button) findViewById(R.id.powerup_button);
				powerupButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button));
				powerupButton.setEnabled(true);
				item = "Powerup";
			}
		
		Toast.makeText(getApplicationContext(),	"Bonus! +1 " + item,
				Toast.LENGTH_SHORT).show();
		}
	}

	private void ghostAttack() {

		game.ghostAttack();
		
		List<Location> tileLocs = game.getGrid().getFilledLocations();
		int tileValue;
		ImageView tile;
		for(Location loc : tileLocs) {
			tileValue = game.getGrid().get(loc);
			tile = findTileByLocation(loc);
			
			Drawable[] layers = new Drawable[2];
			// The current icon
			layers[0] = getResources().getDrawable(getIcon(tileValue));
			// No icon found, default to question mark
			layers[1] = getResources().getDrawable(getIcon(-10));
			TransitionDrawable transition = new TransitionDrawable(layers);
			tile.setImageDrawable(transition);
			transition.startTransition((int) NEW_TILE_SPEED);
		}
		
		ghostAttackActive = true;
		
	}
	private void endGhostAttack() {

		ghostAttackActive = false;
		
		List<Location> tileLocs = game.getGrid().getFilledLocations();
		int tileValue;
		ImageView tile;
		for(Location loc : tileLocs) {
			tileValue = game.getGrid().get(loc);
			tile = findTileByLocation(loc);
			
			Drawable[] layers = new Drawable[2];
			// The ghost icon
			layers[0] = getResources().getDrawable(getIcon(-10));
			// The tile icon
			layers[1] = getResources().getDrawable(getIcon(tileValue));
			TransitionDrawable transition = new TransitionDrawable(layers);
			tile.setImageDrawable(transition);
			transition.startTransition((int) NEW_TILE_SPEED);
			
		}
	}

	/**
	 * Update the game information. 
	 * Turn, Score, Undos Left, and Moves Left
	 */
	private void updateGame() {
		
		updateTextviews();

		// Update the game board
		updateGrid();
	}
	
	private void updateTextviews() {
		TextView turnTextView = (TextView) findViewById(R.id.turn_textview);
		TextView scoreTextView = (TextView) findViewById(R.id.score_textview);
		TextView undosTextView = (TextView) findViewById(R.id.undos_textview);
		TextView activeAttacksTextView = (TextView) findViewById(R.id.active_attacks_textview);
		TextView powerupsTextView = (TextView) findViewById(R.id.powerups_textview);
		Button undoButton = (Button) findViewById(R.id.undo_button);
		Button powerupButton = (Button) findViewById(R.id.powerup_button);
		
		// Update the turn number
		turnTextView.setText(getString(R.string.turn) + " #" + game.getTurns());

		// Update the score
		scoreTextView.setText(getString(R.string.score) + ": " + game.getScore());

		// Update the undos left
		int undosLeft = game.getUndosRemaining();
		if(undosLeft <= 0) {
			undosTextView.setVisibility(View.INVISIBLE);
			if(undosLeft == 0)
				undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
		}
		else {
			undosTextView.setVisibility(View.VISIBLE);
			undosTextView.setText(""+undosLeft);
			undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button));
		}
		undoButton.setEnabled(undosLeft != 0);

		// Update attacks
		activeAttacksTextView.setText(getAttackString());

		// Update the powerups left
		int powerupsLeft = game.getPowerupsRemaining();
		if(powerupsLeft <= 0) {
			powerupsTextView.setVisibility(View.INVISIBLE);
			if(powerupsLeft == 0)
				powerupButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button_disabled));
		}
		else {
			powerupsTextView.setVisibility(View.VISIBLE);
			powerupsTextView.setText(""+powerupsLeft);
			powerupButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button));
		}
		powerupButton.setEnabled(powerupsLeft != 0);
	}
	
	private String getAttackString() {
		int activeGameAttack = game.getActiveAttack();
		String resultString = "";
		
		switch(activeGameAttack) {
		case Game.X_ATTACK:
			resultString += "X Attack active";
			break;
		case Game.GHOST_ATTACK:
			resultString += "Ghost Attack active";
			break;
		case Game.ICE_ATTACK:
			resultString += "Frozen! Cannot move " + Location.directionToString(game.getIceDirection());
			break;
		default:
			return "";
		}
		
		resultString += " for " + game.getAttackDuration() + " turns";
		return resultString;

	}

	/**
	 * Create the game board
	 */
	private void createGrid() {
		
		// The grid that all tiles are on
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Set the number of rows and columns in the game
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());

		// The new tile to insert
		ImageView tile;
		// Used to check if the tile already exists
		ImageView existingTile;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tileValue;

		List<Location> tileLocations = game.getGrid().toList();
		for(Location tileLoc : tileLocations) {
			specRow = GridLayout.spec(tileLoc.getRow(), 1); 
			specCol = GridLayout.spec(tileLoc.getCol(), 1);
			gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
			
			// Check if that tile already exists
			existingTile = findTileByLocation(tileLoc);

			// Remove the existing tile if there is one
			if(existingTile!=null)
				((ViewGroup) existingTile.getParent()).removeView(existingTile);

			tile = new ImageView(this);
			tile.setId(getTileIdByLocation(tileLoc));
			
			tileValue = game.getGrid().get(tileLoc);
			if(tileValue == 0)
				tile.setVisibility(View.INVISIBLE);
			else 
				tile.setVisibility(View.VISIBLE);
			
			gridLayout.addView(tile,gridLayoutParam);
		}
		
		boardCreated = true;
	}
	
	/**
	 * Calculate the distances that tiles should move when the game is swiped
	 */
	private void calculateDistances() {
		GridLayout grid = (GridLayout) findViewById(R.id.grid_layout);
		
		verticalTileDistance = grid.getHeight() / game.getGrid().getNumRows();
		horizontalTileDistance = grid.getWidth() / game.getGrid().getNumCols();
	}

	/**
	 * Update the game board 
	 */
	private void updateGrid() {
		
		if(! boardCreated)
			createGrid();
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());
		
		ImageView tile;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int expectedValue, actualValue;
		
		List<Location> tileLocations = game.getGrid().toList();

		for(Location tileLoc : tileLocations) {
			tile = findTileByLocation(tileLoc);
			expectedValue = game.getGrid().get(tileLoc);

			// A tiles's tag is its value
			try {
				actualValue = Integer.parseInt(tile.getTag().toString());
			}
			catch(Exception e) {
				// Update the tile just in case
				actualValue = -10;
			}

			if(expectedValue != actualValue) {

				specRow = GridLayout.spec(tileLoc.getRow(), 1); 
				specCol = GridLayout.spec(tileLoc.getCol(), 1);
				gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);

				// Remove the tile
				ViewGroup layout = (ViewGroup) tile.getParent();
				if(null!=layout)
					layout.removeView(tile);

				// Create a new tile to insert back into the board
				tile = new ImageView(this);
				tile.setId(getTileIdByLocation(tileLoc));

				tile.setTag(expectedValue);
				setIcon(tile, expectedValue);

				if(expectedValue == 0) 
					tile.setVisibility(View.INVISIBLE);
				else
					tile.setVisibility(View.VISIBLE);

				// Insert the new tile into the board
				gridLayout.addView(tile,gridLayoutParam);
			}
		}
		
		if(game.lost())
			lost();
	}
	
	private void setIcon(ImageView tile, int tileValue) {
		tile.setBackgroundResource(getIcon(tileValue));
	}

	/**
	 * Update the tile's icon to match its value
	 * @param tile The ImageView to change
	 * @param tileValue The numerical value of the tile
	 */
	private int getIcon(int tileValue) {

		if(game.getGameModeId() == GameModes.GHOST_MODE_ID || ghostAttackActive)
			return R.drawable.tile_question;
		else {
			switch(tileValue) {
			case -2:
				return R.drawable.tile_x;
			case -1:
				return R.drawable.tile_corner;
			case 0:
				return R.drawable.tile_blank;
			case 2:
				return R.drawable.tile_2;
			case 4:
				return R.drawable.tile_4;
			case 8:
				return R.drawable.tile_8;
			case 16:
				return R.drawable.tile_16;
			case 32:
				return R.drawable.tile_32;
			case 64:
				return R.drawable.tile_64;
			case 128:
				return R.drawable.tile_128;
			case 256:
				return R.drawable.tile_256;
			case 512:
				return R.drawable.tile_512;
			case 1024:
				return R.drawable.tile_1024;
			case 2048:
				return R.drawable.tile_2048;
			// If I did not create an image for the tile,
			// default to a question mark
			default:
				return R.drawable.tile_question;
			}
		}
	}
	
	private void lost() {
		
		// Prevent the notification from appearing multiple times
		if(gameLost)
			return;
		
		gameLost = true;
		
		// This is the only place where total games played is incremented.
		gameStats.totalGamesPlayed++;
		
		// Create a new lose dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("You Lost");
		// Two buttons appear, try again and cancel
		builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               restartGame();
		           }
		       });
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User cancelled the dialog
		           }
		       });
		
		/*
		// Update the leaderboards
		if(getApiClient().isConnected()){
            Games.Leaderboards.submitScore(getApiClient(), 
                    getString(R.string.leaderboard_classic_mode), 
                    game.getScore());
            
            Games.Leaderboards.submitScore(getApiClient(), 
                    getString(R.string.leaderboard_lowest_score), 
                    game.getScore());
        }
        */
		
		// You cannot undo a game once you lose
		Button undoButton = (Button) findViewById(R.id.undo_button);
		undoButton.setEnabled(false);
		
		// Create the message to show the player
		String message = "";
		message = createLoseMessage(game, gameStats);
		builder.setMessage(message);
		AlertDialog dialog = builder.create();
		
		// You must click on one of the buttons in order to dismiss the dialog
		dialog.setCanceledOnTouchOutside(false);

		// Show the dialog
		dialog.show();
		
		save();
		
		// Delete the current save file. The user can no longer continue this game.
		File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
		currentGameFile.delete();
		
		submitEvents(game);
	}
	
	/**
	 * Update the events on google play games with the game information
	 * @param myGame The game to get the data from
	 */
	public void submitEvents(Game myGame)
	{
		if(getApiClient().isConnected()) {
			String playedGameId = getString(R.string.event_played_game);
			String totalMovesId = getString(R.string.event_total_moves);
			String totalScoreId = getString(R.string.event_total_score);

			// Increment the event counters
			Games.Events.increment(this.getApiClient(), playedGameId, 1);
			Games.Events.increment(this.getApiClient(), totalMovesId, game.getTurns());
			Games.Events.increment(this.getApiClient(), totalScoreId, game.getScore());
		}
	}
	
	public void callback() {
		// EventCallback is a subclass of ResultCallback; use this to handle the
		// query results
		EventCallback ec = new EventCallback();

		// Load all events tracked for your game
		com.google.android.gms.common.api.PendingResult<Events.LoadEventsResult>
		        pr = Games.Events.load(this.getApiClient(), true);
		pr.setResultCallback(ec);
	}
	
	class EventCallback implements ResultCallback
	{
	    // Handle the results from the events load call
	    public void onResult(com.google.android.gms.common.api.Result result) {
	        Events.LoadEventsResult r = (Events.LoadEventsResult)result;
	        com.google.android.gms.games.event.EventBuffer eb = r.getEvents();

	        for (int i=0; i < eb.getCount(); i++) {
	        	Event e = eb.get(i);
	        	
	        	Log.d(LOG_TAG, ""+e.toString());
	        	
	        	Toast.makeText(getApplicationContext(),
	    				""+e.getValue(),
	    				Toast.LENGTH_SHORT).show();
	        }
	        eb.close();
	    }
	}
	
	/** Create the message that is shown to the user after they lose.
	 * @param myGame The game that was currently played
	 * @param myGameStats The game stats of the game
	 * @return The message to display
	 */
	private String createLoseMessage(Game myGame, Statistics myGameStats) {
		String message = "";
		// Notify if there is a new high score
		if(myGame.getScore() > myGameStats.highScore) {
			myGameStats.highScore = myGame.getScore();
			myGameStats.bestGame = myGame;
			message += "New High Score! " + myGame.getScore();
		}

		// Notify if there is a new highest tile
		if(myGame.highestPiece() > myGameStats.highestTile) {
			myGameStats.highestTile = myGame.highestPiece();

			if(! message.equals(""))
				message += "\n"; 
			message += "New Highest Tile! " + myGame.highestPiece();
		}

		// Only notify if there is a new low score if there are no other records.
		if(myGameStats.lowScore < 0 ||
				myGame.getScore() < myGameStats.lowScore) {
			myGameStats.lowScore = myGame.getScore();
			myGameStats.worstGame = myGame;

			if(message.equals(""))
				message += "New Lowest Score! " + myGame.getScore();
		}

		// If there are no records then just show the score
		if(message.equals(""))
			message += "Final Score: " + myGame.getScore();
		return message;
	}
	
	private void showPowerupDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		final Button powerupButton = (Button) findViewById(R.id.powerup_button);
		
		if(game.lost()) {
			builder.setTitle("Cannot use powerup")
			.setMessage("You cannot use powerups after you lose");
		}
		else
			if(game.getPowerupsRemaining() != 0) {
				builder.setTitle("Choose powerup")
				.setItems(R.array.powerups, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
						Log.d(LOG_TAG, "on click");
						
						// The 'which' argument contains the index position
						// of the selected item
						switch(which) {
						case 0:
							shuffleGame();
							game.decrementPowerupsRemaining();
							break;
						case 1:
							// The number of powerups is decremented in removeTile
							// after a tile has been selected
							removeTile();
							break;
						case 2:
							removeLowTiles();
							game.decrementPowerupsRemaining();
							break;
						case 3:
							genie_enabled = true;
							game.decrementPowerupsRemaining();
							updateTextviews();
							break;
						}
					}
				});
			}
			else {
				builder.setTitle("No More Powerups")
				.setMessage("There are no powerups remaining");
			}
		builder.create().show();
	}
	
	/**
	 * This method is no longer used because I switched
	 * to ImageViews instead of buttons. I may need this method 
	 * later for zen mode.
	 * @param tile The number representation of the tile
	 * @return The string representation of the tile
	 */
	private String convertToTileText(int tile) {
		switch (tile) {
		case 0:
			return "";
		case -1:
			return "XX";
		case -2:
			return "x";
		default:
			return "" + tile;
		}
	}

	/**
	 * Add a new tile to the board
	 */
	private void addTile() {
		
		// Add a new tile to the game object
		Location loc = game.addRandomPiece();
		
		// Find the tile to make appear
		ImageView newTile = findTileByLocation(loc);
		
		// Immediately set the alpha of the tile to 0
		ObjectAnimator.ofFloat(newTile, View.ALPHA, 0).setDuration(0).start();
		
		// Update the new tile's tag and icon
		int tileValue = game.getGrid().get(loc);
		newTile.setTag(tileValue);
		setIcon(newTile, tileValue);
		
		// Make the tile visible. It still cannot be seen because the alpha is 0
		newTile.setVisibility(View.VISIBLE);
		
		// Fade the tile in
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(newTile, View.ALPHA, 1)
				.setDuration(NEW_TILE_SPEED);
		
		// Keep track of the active animations in case the activity is stopped
		alphaAnimation.addListener(new AnimatorListener(){
			@Override
			public void onAnimationEnd(Animator animation) {
				activeAnimations.clear();
			}
			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) {
				activeAnimations.clear();
			}
			@Override
			public void onAnimationRepeat(Animator animation) { }
		});
		
		activeAnimations.add(alphaAnimation);
		alphaAnimation.start();
	}
	
	/**
	 * Remove a tile from the board when it is tapped
	 */
	private void removeTile() {
		animationInProgress = true;
		
		for(int row = 0; row < game.getGrid().getNumRows(); row++) {
			for(int col = 0; col < game.getGrid().getNumCols(); col++) {
				ImageView tile = (ImageView) findViewById(row * 100 + col);

				if(tile.getVisibility() == View.VISIBLE &&
						(game.getGrid().get(new Location(row, col)) > 0)) {
					
					// Start shaking if the value is > 0
					Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
					tile.startAnimation(shake);

					tile.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View view) {

							// Create and start an animation of the tile fading away
							(ObjectAnimator.ofFloat(view, View.ALPHA, 0)
									.setDuration(NEW_TILE_SPEED)).start();
							game.removeTile(new Location(view.getId() / 100, view.getId() % 100));
							game.decrementPowerupsRemaining();
							updateTextviews();
							clearTileListeners();
						}
					});
				}
			}
		}

		View gameActivity = findViewById(R.id.game_activity);
		gameActivity.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View view, MotionEvent event) {
					clearTileListeners();
					Button powerupButton = (Button) findViewById(R.id.powerup_button);
					powerupButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button));
					powerupButton.setEnabled(true);
					return true;
				}
		});
	}
	
	private void clearTileListeners() {
		animationInProgress = false;
		(findViewById(R.id.game_activity)).setOnTouchListener(null);
		
		for(int row = 0; row < game.getGrid().getNumRows(); row++) {
			for(int col = 0; col < game.getGrid().getNumCols(); col++) {
				ImageView tile = (ImageView) findViewById(row * 100 + col);
				tile.setOnClickListener(null);
				tile.clearAnimation();
			}
		}
	}

	/**
	 * Remove all 2's and 4's from the game with a fade out animation
	 */
	private void removeLowTiles() {
		
		animationInProgress = true;
			
		// Save the game history before each move
		game.saveGameInHistory();
		
		// The grid where all of the tiles are in
		Grid gameBoard = game.getGrid();
		
		// Get a list of all tiles
		List<Location> tiles = game.getGrid().getFilledLocations();
		
		// An list of the fade animations to play
		ArrayList<ObjectAnimator> alphaAnimations = new ArrayList<ObjectAnimator>();
		
		// Loop through each tile
		for(Location tile : tiles) {
			if(gameBoard.get(tile) == 2 || gameBoard.get(tile) == 4) {
				
				ImageView toRemove = findTileByLocation(tile);
				
				// Set the tag to the new value
				toRemove.setTag(0);
				
				// Create a new animation of the tile fading away and
				// add it to the list
				alphaAnimations.add(ObjectAnimator.ofFloat(toRemove, View.ALPHA, 0)
						.setDuration(NEW_TILE_SPEED));
			}
		}
		
		if(alphaAnimations.isEmpty()) {
			animationInProgress = false;
			return;
		}
		
		// Assume that all animations finish at the same time
		alphaAnimations.get(0).addListener(new AnimatorListener(){
			
			@Override
			public void onAnimationEnd(Animator animation) {
				game.removeLowTiles();
				game.newTurn();
				gameStats.totalMoves += 1;
				updateGame();
				activeAnimations.clear();
				animationInProgress = false;
			}
			
			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Animation cancelled");
				activeAnimations.clear();
				animationInProgress = false;
			}
			@Override
			public void onAnimationRepeat(Animator animation) { }
		});
		
		// Remove all of the tiles
		for(ObjectAnimator animation: alphaAnimations) {
			activeAnimations.add(animation);
			animation.start();
		}
	}
	
	/**
	 * Shuffles the game board and animates the grid
	 * The grid layout spins 360�, the tiles are shuffled, then it spins
	 * back in the opposite direction
	 */
	private void shuffleGame() {
		
		// Save the game history before each move
		game.saveGameInHistory();
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Causes conflicts when the shuffle button is double tapped
		if(animationInProgress)
			return;
		
		ObjectAnimator rotateAnimation =
				ObjectAnimator.ofFloat(gridLayout, View.ROTATION, 360);
		rotateAnimation.setRepeatCount(1);
		rotateAnimation.setRepeatMode(ValueAnimator.REVERSE);
		
		// 300 ms should be fast enough to not notice the tiles changing
		rotateAnimation.setDuration(SHUFFLE_SPEED);
		
		rotateAnimation.addListener(new AnimatorListener(){
			@Override
			public void onAnimationStart(Animator animation) { 
				animationInProgress = true;
			}
			@Override
			public void onAnimationEnd(Animator animation) { 
				activeAnimations.clear();
				animationInProgress = false;
			}
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Shuffle animation cancelled");
				activeAnimations.clear();
				animationInProgress = false;
			}
			@Override
			public void onAnimationRepeat(Animator animation) {
				game.shuffle();
				gameStats.totalShufflesUsed += 1;
				gameStats.totalMoves += 1;
				updateGame();
			}
		});
		
		activeAnimations.add(rotateAnimation);
		rotateAnimation.start();
	}
	
	/**
	 * Freezes the game (can not move in a direction for a random amount of turns)
	 */
	public void ice() {
		
		// This attack cannot be stacked
		if(game.getAttackDuration() <= 0)
			game.ice();
	}
	
	/**
	 * Temporarily adds an X tile to the game for a limited amount of time
	 */
	private void XTileAttack() {
		
		// This attack cannot be stacked
		if(game.getAttackDuration() <= 0) {
			game.XTileAttack();
			XTileAttackActive = true;
			updateGrid();
		}
	}
	
	private void endXAttack() {
		
		XTileAttackActive = false;
		Location XTileLoc = game.endXTileAttack();
		
		if(XTileLoc != null) {
			ImageView tile = findTileByLocation(XTileLoc);
			
			// Create and start an animation of the tile fading away
			ObjectAnimator fade = ObjectAnimator.ofFloat(tile, View.ALPHA, 0)
					.setDuration(NEW_TILE_SPEED);
			fade.addListener(new AnimatorListener() {

				@Override
				public void onAnimationCancel(Animator arg0) {}
				@Override
				public void onAnimationEnd(Animator animation) {
					//updateGame();
				}
				@Override
				public void onAnimationRepeat(Animator animation) {}
				@Override
				public void onAnimationStart(Animator animation) {}
			});
			fade.start();
		}
	}

	/**
	 * Warn the user about moving in that direction
	 * @return True if the user decided to move in that direction anyway
	 */
	public void warnAboutMove(final int direction) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Are you sure?");
		builder.setMessage("Moving " + Location.directionToString(direction) + " might cause you to lose");
		
		// Two buttons appear, yes and no
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			// If the user clicked yes consume the powerup and move
			public void onClick(DialogInterface dialog, int id) {
				genie_enabled = false;
				game.act(direction);
				updateGame();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			// If the user clicked no consume the powerup and don't move
			public void onClick(DialogInterface dialog, int id) {
				genie_enabled = false;
			}
		});
		
		AlertDialog dialog = builder.create();	
		
		// You must click on one of the buttons in order to dismiss the dialog
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}
	
	/**
	 * Undo the game. Currently does not have any animations because it
	 * would be difficult to track every tile separately
	 */
	public void undo() {
		final Button undoButton = (Button) findViewById(R.id.undo_button);
		if(game.getUndosRemaining() == 0) {
			undoButton.setEnabled(false);
		}
		else
		{
			if(game.getTurns() > 1) {

				// Reset the rotation to the default orientation
				undoButton.setRotation(0);

				ObjectAnimator spinAnimation = ObjectAnimator.ofFloat(undoButton, View.ROTATION, -360);

				if(game.getUndosRemaining() == 1) {
					spinAnimation.addListener(new AnimatorListener() {

						@Override
						public void onAnimationCancel(Animator animation) {
							undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
						}
						@Override
						public void onAnimationEnd(Animator animation) {
							undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
						}
						@Override
						public void onAnimationRepeat(Animator animation) {}
						@Override
						public void onAnimationStart(Animator animation) {}
					});
				}

				spinAnimation.start();

				game.undo();
				gameStats.totalMoves += 1;
				gameStats.totalUndosUsed += 1;
				updateGame();
			}
		}
	}
	
	/**
	 * Restart the game.
	 */
	public void restartGame() {
		
		Log.d(LOG_TAG, "restart game");
		
		Button restartButton = (Button) findViewById(R.id.restart_button);
		restartButton.setRotation(0);
		ObjectAnimator spinAnimation = ObjectAnimator.ofFloat(restartButton, View.ROTATION, -360);
		spinAnimation.start();
		
		if(animationInProgress)
			clearTileListeners();
		
		// Save any new records
		if(game.highestPiece() > gameStats.highestTile)
			gameStats.highestTile = game.highestPiece();
		
		if(game.getScore() > gameStats.highScore) {
			gameStats.highScore = game.getScore();
			gameStats.bestGame = game;
		}
		
		game = GameModes.newGameFromId(game.getGameModeId());

		// Set the undo button to be enabled or disabled
		Button undoButton = (Button) findViewById(R.id.undo_button);
		if (game.getUndosRemaining() == 0)
			undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
		else
			undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button));

		gameLost = false;
		updateGame();
	}

	/**
	 * Save the game and game stats to a file
	 */
	private void save() {
		
		File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
		File gameStatsFile = new File(getFilesDir(), getString(R.string.file_game_stats));

		try {
			Save.save(game, currentGameFile);
			Save.save(gameStats, gameStatsFile);
		} catch (IOException e) {
			e.printStackTrace();
			// Notify the user of the error through a toast
			Toast.makeText(getApplicationContext(), "Error: Save file not found", Toast.LENGTH_SHORT).show();
		}
		
		requestBackup();
	}
	
	
	public void requestBackup() {

		Log.d(LOG_TAG, "requesting backup");
		
		BackupManager bm = new BackupManager(this);
		bm.dataChanged();
	}
	
	public void requestRestore()
	{
		Log.d(LOG_TAG, "request restore");
		
		BackupManager bm = new BackupManager(this);
		bm.requestRestore(
				new RestoreObserver() {
					@Override
					public void restoreStarting(int numPackages) {
						Log.d(LOG_TAG, "Restore from cloud starting.");
						Log.d(LOG_TAG, ""+gameStats.totalMoves);
						
						super.restoreStarting(numPackages);
					}
					
					@Override
					public void onUpdate(int nowBeingRestored, String currentPackage) {
						Log.d(LOG_TAG, "Restoring "+currentPackage);
						super.onUpdate(nowBeingRestored, currentPackage);
					}
					
					@Override
					public void restoreFinished(int error) {
						Log.d(LOG_TAG, "Restore from cloud finished.");
						
						super.restoreFinished(error);
						Log.d(LOG_TAG, ""+gameStats.totalMoves);
						
						Log.d(LOG_TAG, "calling load");
						load();
						
					}
				});
	}

	/**
	 * Load the game from a file and update the game
	 */
	private void load() {
		
		File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
		File gameStatsFile = new File(getFilesDir(), getString(R.string.file_game_stats));

		try {
			game = (Game) Save.load(currentGameFile);
			
			gameStats = (Statistics) Save.load(gameStatsFile);
		} catch (ClassNotFoundException e) {
			Log.w(LOG_TAG, "Class not found exception in load");
			game = new Game();
			gameStats = new Statistics();
		} catch (IOException e) {
			Log.w(LOG_TAG, "In load: No save file found, using default game");
			game = new Game();
			gameStats = new Statistics();
		}
		
		Log.d(LOG_TAG, "total moves " + gameStats.totalMoves);
		updateGame();
	}
	
	/**
	 * Unlock an achievement when a new highest tile is reached 
	 * @param tile The new highest tile
	 */
	private void unlockAchievementNewHighestTile(int tile) {
		
		Log.d(LOG_TAG, "unlocking achievement " + tile + " tile");
		
		if(getApiClient().isConnected()) {
			switch(tile) {
			case 128:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_128_achievement));
				break;
			case 256:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_256_achievement));
				break;
			case 512:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_512_achievement));
				break;
			case 1024:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_1024_achievement));
				break;
			case 2048:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_2048_achievement));
				break;
			}
		}
	}
	
	private List<ImageView> getListOfAllTiles() {
		List<ImageView> tiles = new ArrayList<ImageView>();
		List<Location> locs = game.getGrid().toList();
		
		for(Location loc : locs)
			tiles.add(findTileByLocation(loc));
		
		return tiles;
	}
	
	/**
	 * @param loc The location of the tile to find
	 * @return The ImageView of the tile
	 */
	private ImageView findTileByLocation(Location loc) {
		// It is more efficient to find a view using its parent than the entire activity
		if(gridLayout != null)
			return (ImageView) gridLayout.findViewById(getTileIdByLocation(loc));
		return (ImageView) findViewById(getTileIdByLocation(loc));
	}
	
	/**
	 * This method supports grids up to a size of 100x100 before collisions occur
	 * @param loc The location of the tile to find the id for
	 * @return The id of the tile
	 */
	private int getTileIdByLocation(Location loc) {
		return loc.getRow() * 100 + loc.getCol();
	}

	/**
	 * Shows the active quests
	 */
	/*
	public void showQuests()
	{
		// EventCallback is a subclass of ResultCallback; use this to handle the
		// query results
		EventCallback ec = new EventCallback();

		// Load all events tracked for your game
		com.google.android.gms.common.api.PendingResult<Events.LoadEventsResult>
		        pr = Games.Events.load(this.getApiClient(), true);
		pr.setResultCallback(ec);
		
		Intent questsIntent = Games.Quests.getQuestsIntent(this.getApiClient(),
	            Quests.SELECT_ALL_QUESTS);
	    startActivityForResult(questsIntent, 0);
	}
	*/

	/**
	 * The only fragment in the activity. Has the game board and the
	 * game info such as score or turn number
	 */
	public static class GameFragment extends Fragment {

		public GameFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			View rootView = inflater.inflate(R.layout.fragment_game, container,
					false);
			
			final Button undoButton = (Button) rootView.findViewById(R.id.undo_button);  
	        undoButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	((GameActivity)getActivity()).undo();
	            }
	        });
			
	        final Button restartButton = (Button) rootView.findViewById(R.id.restart_button);  
	        restartButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	((GameActivity)getActivity()).restartGame();
	            }
	        });
	        
	        /*
	        // Get tracker.
	        Tracker t = ((MainApplication) getActivity().getApplication()).getTracker();

	        // Set screen name.
	        // Where path is a String representing the screen name.
	        t.setScreenName("GameActivity");

	        // Send a screen view.
	        t.send(new HitBuilders.AppViewBuilder().build());

	        t.send(new HitBuilders.ScreenViewBuilder().build());
	        */
	        
	        return rootView;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_DPAD_UP:
			act(Location.UP);
			return true; 
		case KeyEvent.KEYCODE_DPAD_LEFT:
			act(Location.LEFT);
			return true; 
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			act(Location.RIGHT);
			return true; 
		case KeyEvent.KEYCODE_DPAD_DOWN:
			act(Location.DOWN);
			return true; 
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean onDown(MotionEvent event) {
    	listenForSwipe = true;
    	return true;
    }
    
    /**
     * When the screen is swiped, move the board
     */
    @Override
    public boolean onScroll(MotionEvent initialEvent, MotionEvent currentEvent,
    		float distanceX, float distanceY) { 

    	if(listenForSwipe && !animationInProgress) {
    		if(Math.abs(initialEvent.getX() - currentEvent.getX()) > 100) {
    			if(initialEvent.getX() > currentEvent.getX())
    				act(Location.LEFT);
    			else
    				act(Location.RIGHT);

    			listenForSwipe = false;
    		}
    		else if(Math.abs(initialEvent.getY() - currentEvent.getY()) > 100) {
    			if(initialEvent.getY() > currentEvent.getY())
    				act(Location.UP);
    			else
    				act(Location.DOWN);
    			listenForSwipe = false;
    		}
    	}
    	return true;
    }
    
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
    		float velocityX, float velocityY) {
    	return true;
    }
    @Override
    public void onShowPress(MotionEvent event) {}
    @Override
    public void onLongPress(MotionEvent event) {}
    @Override
    public boolean onSingleTapUp(MotionEvent event) { return true; }

    // Google+ sign in
	@Override
	public void onSignInFailed() {}
	@Override
	public void onSignInSucceeded() {}
}