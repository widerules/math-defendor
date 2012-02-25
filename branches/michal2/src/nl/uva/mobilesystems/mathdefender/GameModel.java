package nl.uva.mobilesystems.mathdefender;

import java.util.LinkedList;

import nl.uva.mobilesystems.mathdefender.andengine.events.EventsConstants;
import nl.uva.mobilesystems.mathdefender.andengine.events.ObjectPositionEvent;
import nl.uva.mobilesystems.mathdefender.andengine.events.ObjectPositionEventListener;
import nl.uva.mobilesystems.mathdefender.game.Level;
import nl.uva.mobilesystems.mathdefender.game.Wave;
import nl.uva.mobilesystems.mathdefender.objects.Enemy;
import nl.uva.mobilesystems.mathdefender.objects.Tower;
import nl.uva.mobilesystems.mathdefender.physics.PhConstants;

import org.andengine.engine.Engine;
import org.andengine.entity.IEntity;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import android.graphics.Point;
import android.util.Log;

/** 
 *
 * @author siemionides
 *
 */
public class GameModel implements ObjectPositionEventListener {
	
	// ----------------------- VARIABLES --------------------------------
	/** Game Variables */
	Engine engine;
	
	Scene scene; //it's little bit akward, it must be here because current implementation of Model starts drawing before InitialActivity.onCreateScene() method is finished, so engine variable (field in GameModel class) doesnt know about this scene yet
	
	/** Variable represeting current level that is maninated by GameModel */
	private Level currentLevel;
	
	/** Debug things */
	
	private Text wavesLeftText; 
	
	
	// ----------------------- CONSTRUCTORS --------------------------------
	
	public GameModel(InitialActivity activity, Scene scene){
		this.engine = activity.getEngine();			//Laurens: We should prob switch this to an object reference to the engine itself in case an activity can have several engines?
		this.wavesLeftText = activity.text;
		this.scene = scene;
	}
	


	// ----------------------- SETTERS & GETTERS  --------------------------------
	
	
	public LinkedList<AnimatedSprite> getCurrentWaveObjects(){
		return this.currentLevel.getCurrentWave().getObjects();
	}
	
	public LinkedList<Wave> getWavesLeft(){
		return this.currentLevel.getWaves();
	}
	
	
	// --------------------- OVERRIDDEN METHODS ------------------------------
	
	@Override
	public void handleObjectPositionEvent(ObjectPositionEvent e) {
		
		switch(e.getEventCode()){
		
		case EventsConstants.EVENT_OBJECT_OUT_OF_SCENE:
			AnimatedSprite object = (AnimatedSprite) e.getSource(); 
			removeObjectFromScene(object);
			currentLevel.getCurrentWave().removeObject(object);	//it's ugly, maybe it's better to create some method in Level that could be called instead of walking down trough this hierarchy?
			object = null;
			Log.v("eventMine", "RemovesObject");
			if(currentLevel.getCurrentWave().getObjects().size() == 0){ //check whether something is still in current Wave
				if( currentLevel.getWaves().size() > 0) //if there are still waves to be shown
					startNewWave();
				
			}
			break;
		}
		
	}
	
	//-------------------- PUBLIC METHODS ----------------------------
	
	/** 
	 * This method is here more for debug/development purposes. It sets:
	 * - 1 New Level
	 * - nrWaves Waves in it
	 * - nrTowers Towers in it (will be places in the center of screen
	 * 
	 * Ultra important and bad-coding style method; Sets waves, enemies in there */
	public void setUpSimpleGame(int nrWaves, int nrTowers, Point screenDimenstions, TiledTextureRegion textureEnemy, TiledTextureRegion textureTower, VertexBufferObjectManager objectManager){
		this.currentLevel = new Level(Level.DIFF_TUTORIAL);
		this.currentLevel.setWaves(new LinkedList<Wave>());
		
		for(int i=0; i<nrWaves; i++){
			LinkedList<AnimatedSprite>  tempEnemies = new LinkedList<AnimatedSprite>();
			for(int j=0; j< PhConstants.NR_ENEMIES_IN_WAVE; j++){ //generating enemies
				int random = (int)(Math.random() * 1000);	//should be an integer number from 0 - 1000 
				int x = screenDimenstions.x; //the edge of a screen
				int y = screenDimenstions.y / (PhConstants.NR_ENEMIES_IN_WAVE+1) * (j+1);	//so equal distribution on screen Width
				
				Enemy tempEnemy = new Enemy(x,y, textureEnemy, objectManager);
				tempEnemy.addObjectPositionEventListener(this);
				tempEnemies.add(tempEnemy);
			}
			Wave tempWave = new Wave(tempEnemies);
			this.currentLevel.getWaves().offer(tempWave);
		}
		this.currentLevel.setCurrentWave(this.currentLevel.getWaves().poll());
		
		//Add additional TOWER to the game
		for(int j=0; j<nrTowers; j++){
			setNewTowerAt(screenDimenstions.x/2, screenDimenstions.y/2, textureTower, objectManager);
		}
		
		
		
	}
	
	
	/**
	 * Sets new Tower to current Level and adds it to the scene.
	 * @param X
	 * @param Y
	 * @param pTiledTextureRegion
	 * @param pVertexBufferObjectManager
	 */
	public void setNewTowerAt(final float X, final float Y, ITiledTextureRegion pTiledTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager){
		Tower newTower = new Tower(X,Y, pTiledTextureRegion, pVertexBufferObjectManager);
		this.currentLevel.getTowers().add(newTower);
		addObjectToScene(newTower);
		
	}
	
	/**
	 * This method is for code clarity.
	 * @param entity
	 */
	public void addObjectToScene(IEntity entity){
		this.scene.attachChild(entity);
	}
	
	
	/**
	 * This method should be used to safely remove objects from scene care need be taken for concurrency issues.
	 * It is possible to do it manually, (within engine.runOnUpdateThread) but it's here for code clarity. 
	 * @param entity
	 */
	public void removeObjectFromScene(final IEntity entity){
		engine.runOnUpdateThread(new Runnable() {
			
			@Override
			public void run() {
				entity.detachSelf();
				entity.dispose();
			}
		});
	}
	
	
	//-------------------- PRIVATE METHODS ----------------------------
	
	private void startNewWave(){
		currentLevel.setCurrentWave(currentLevel.getWaves().poll());
		for(IEntity object : currentLevel.getCurrentWave().getObjects()){
			addObjectToScene(object);
		}
	}
	
}
