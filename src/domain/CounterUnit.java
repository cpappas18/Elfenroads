package domain;

import javax.swing.*;

import commands.DrawCounterCommand;
import enums.*;
import gamemanager.GameManager;
import windows.MP3Player;
import windows.MainFrame;
import gamemanager.ActionManager;
import networking.GameState;
import gamescreen.GameScreen;
import utils.GameRuleUtils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public abstract class CounterUnit extends Drawable{

    private Road placedOn;
    boolean owned;
    boolean isSecret;
    boolean aSelected;
    JLabel miniDisplay; // for when the counter is on the map
    JLabel superMiniDisplay;// for obstacle on the map
    private CounterUnitType aType;
    protected MP3Player track1 = new MP3Player("./assets/Music/0000171.mp3");
    
    
    CounterUnit(CounterUnitType pType, int resizeWidth, int resizeHeight, String imageNumber) {
        // find the picture of the card based on what type it is
        // since the images are named similarly and ordered the same way as they are in the enum declaration,
        // we can get the filepath just by using the type
    	super("./assets/sprites/M0" + imageNumber + ".png");
        aType = pType;
        owned = false; // default value
        isSecret = false;
        // String filepath = ("./assets/sprites/M0" + imageNumber + ".png");
        Image toResize = icon.getImage();
        Image resized = toResize.getScaledInstance(resizeWidth, resizeHeight,  java.awt.Image.SCALE_SMOOTH);
        Image resized_mini = toResize.getScaledInstance(resizeWidth/2, resizeHeight/2,  java.awt.Image.SCALE_SMOOTH);
        Image resized_supermini = toResize.getScaledInstance(resizeWidth/3, resizeHeight/3,  java.awt.Image.SCALE_SMOOTH);
        display = new JLabel(new ImageIcon(resized));
        miniDisplay = new JLabel(new ImageIcon(resized_mini));
        superMiniDisplay = new JLabel (new ImageIcon(resized_supermini));
    }


    public Road getPlacedOn() {
        return placedOn;
    }

    public void setPlacedOn(Road placedOn) {
        this.placedOn = placedOn;
    }

    public boolean isOwned() {
        return this.owned;
    }

    public void setOwned(boolean b) {
        this.owned = b;
    }
    
    public boolean isSecret() {
    	return this.isSecret;
    }
    
    public void setSecret(boolean b) {
    	this.isSecret = b;
    }

    public JLabel getMiniDisplay() {
        return this.miniDisplay;
    }
    
    public JLabel getSuperMiniDisplay() {
    	return this.superMiniDisplay;
    }
    
    public boolean isSelected() {
    	return this.aSelected;
    }
    
    public void setSelected(boolean pSelected) {
        if (pSelected && !this.aSelected) {
            // this counter is selected
            display.setBorder(BorderFactory.createLineBorder(Color.yellow));
        } else if (!pSelected && this.aSelected) {
            // this counter is deselected
            display.setBorder(BorderFactory.createEmptyBorder());
        }
    	aSelected = pSelected;
    }
    
    protected void initializeMouseListener() {
        this.getDisplay().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!GameManager.getInstance().isLocalPlayerTurn()) {
                    return;
                }

                // DRAW COUNTERS PHASE, counter is face-up and available to be chosen
                if (!isOwned() && GameRuleUtils.isDrawCountersPhase()) {
                    // adding the counter to my hand
                    track1.play();
                    GameState.instance().getFaceUpCounters().remove((TransportationCounter) CounterUnit.this); // remove the counter from the face-up pile
                    GameManager.getInstance().getThisPlayer().getHand().addUnit(CounterUnit.this);
                    GameState.instance().addFaceUpCounterFromPile(); // replenish the face-up counters with one from the pile
                    GameScreen.getInstance().updateAll(); // update GUI
                    CounterUnit.this.owned = true;
                    CounterUnit.this.setSecret(false);
                    Logger.getGlobal().info("Just added " + CounterUnit.this.getType() +
                            ", current counters in hand: " +
                            GameManager.getInstance().getThisPlayer().getHand().getCounters().toString());

                    // tell the other peers to remove the counter
                    try {
                        GameManager.getInstance().getComs().sendGameCommandToAllPlayers(
                                new DrawCounterCommand(CounterUnit.this, true));
                    } catch (IOException err) {
                        System.out.println("Error: there was a problem sending the DrawCounterCommand to the other peers.");
                    }

                    GameManager.getInstance().endTurn();

                }
                // RETURN COUNTERS PHASE - Elfengold
                else if (GameState.instance().getCurrentPhase() == EGRoundPhaseType.RETURN_COUNTERS ||
                        GameState.instance().getCurrentPhase() == ELRoundPhaseType.RETURN_COUNTERS) {
                    GameManager.getInstance().returnCounter(CounterUnit.this);
                    track1.play();
                }

                // PLAN TRAVEL ROUTES PHASE - both
                else if (GameState.instance().getCurrentPhase() == EGRoundPhaseType.PLAN_ROUTES ||
                        GameState.instance().getCurrentPhase() == ELRoundPhaseType.PLAN_ROUTES) {
                    if (getPlacedOn() == null) {
                        ActionManager.getInstance().setSelectedCounter(CounterUnit.this);
                    } else {
                        // If the counter is placed on a road, then the user's intention is to click on the road
                        ActionManager.getInstance().setSelectedRoad(getPlacedOn());
                    }
                    track1.play();
                }
            }
        });
    }
    public CounterUnitType getType() {
    	return aType;
    }
    
    protected void setType(CounterUnitType pType) {
    	aType = pType;
    }

    //getNew should be hidden by all subclasses.
    public static CounterUnit getNew(CounterUnitType pType) {
        if (Arrays.asList(enums.CounterType.values()).contains(pType)) {
            return new TransportationCounter((CounterType) pType, MainFrame.instance.getWidth() * 67 / 1440, MainFrame.instance.getHeight() * 60 / 900);
        } else if (Arrays.asList(enums.GoldPieceType.values()).contains(pType)) {
            return new GoldPiece((GoldPieceType) pType, MainFrame.instance.getWidth() * 67 / 1440, MainFrame.instance.getHeight() * 60 / 900);
        } else if (Arrays.asList(enums.MagicSpellType.values()).contains(pType)) {
            return new MagicSpell((MagicSpellType) pType, MainFrame.instance.getWidth() * 67 / 1440, MainFrame.instance.getHeight() * 60 / 900);
        } else {
            return new Obstacle((ObstacleType) pType, MainFrame.instance.getWidth() * 67 / 1440, MainFrame.instance.getHeight() * 60 / 900);
        }
    }
}
