package core;

import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.EquipmentSlot;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;
import org.osbot.rs07.api.Settings;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.io.*;
import java.net.*;

@ScriptManifest(author = "RG", info = "Cow hide collector", name = "Cow Hider", version = 0, logo = "")
public class Main extends Script {

    private Area cowArea = new Area(3193,3284,3209,3301);
    private String displayState = "State: Idle";
    private String currentState = "Walking";
    private Font titleFont = new Font("Sans-Serif", Font.BOLD, 10);

    private long startTime;
    
    private long prevHideCount;
    private long hideCount;
    private long hidePrice;
    
    private int trips = 0;

    @Override
    public void onPaint(Graphics2D g) {
        g.setFont(titleFont);
        g.setColor(Color.WHITE);
        g.drawRect(mouse.getPosition().x - 3, mouse.getPosition().y - 3, 6, 6);
        g.drawString("Cow Hider v1.0 - Written by RG", 10, 250);
        g.drawString(displayState, 10, 265);
        g.drawString("Profit: " + (int) Math.round(hidePrice * (hideCount + (trips * 28))), 10, 280);
        g.drawString("Trips: " + trips, 10, 295);
        g.drawString("Profit / hour: " + (int) Math.round(hidePrice * (hideCount + (trips * 28)) / 
        		((System.currentTimeMillis() - startTime) / 3600000.0)), 10, 310);
        g.drawString("Runtime: " + formatTime(System.currentTimeMillis() - startTime), 10, 335);
    }

    @Override
    public void onStart() {
    	startTime = System.currentTimeMillis();
    	
    	prevHideCount = getInventory().getAmount("Cowhide");
    	try {
			hidePrice = getPrice();
    	} catch (Exception e) {
			hidePrice = 105;
			log(e);
		}
    }

    @Override
    public int onLoop() throws InterruptedException {
    	switch (currentState) {
    		case "Walking":
    			walk();   
    			break;
    			
    		case "Looting":
    			loot();
    			break;
    			
    		case "Banking":
    			bank();
    			break;
    			
    		default:
    			displayState = "Failed at" + formatTime(System.currentTimeMillis());
    			log(displayState);
    			return 500;
    	}
    	return 500;
    }
    private void bank() throws InterruptedException {
    	displayState = "State: Banking";
		getWalking().webWalk(Banks.LUMBRIDGE_UPPER);
		
		RS2Object bankBooth = objects.closest("Bank Booth");
		do {
			bankBooth.interact("Bank");
			sleep(2000);
			bank.depositAll();
		} while (!getInventory().isEmpty());
		
		trips += 1;
    	updateHideCount();
    		
    	currentState = "Walking";
    }
    
    private void walk() {
    	displayState = "State: Walking";
		getWalking().webWalk(cowArea);
		if (cowArea.contains(myPosition()))
            currentState = "Looting";
    }
    
    private void loot() throws InterruptedException {
    	if (getInventory().isFull()) {
			currentState = "Banking";
		} else {
			displayState = "State: Looting hide";
			if(settings.getRunEnergy() > 10)
				settings.setRunning(true);
			GroundItem loot = groundItems.closest("Cowhide");
	    	if (loot != null && loot.interact("Take")) {
	    		new ConditionalSleep(800) {
	    			@Override
	    			public boolean condition() throws InterruptedException {
	    				return !loot.exists();
	    			}
	    		}.sleep();
	    		checkBones();
	    		updateHideCount();
	    	}
		}
    }
    
    
    private void checkBones() throws InterruptedException {
    	while (inventory.getAmount("Bones") != 0) {
			Item bones = inventory.getItem("Bones");
			if (bones != null) {
				bones.interact("Bury");
				if (myPlayer().isAnimating()) {
					bones.interact("Bury");
					sleep(random(200));
				}
			}
		}
    }
    private String formatTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        s %= 60; m %= 60; h %= 24;

        return d > 0 ? String.format("%02d:%02d:%02d:%02d", d, h, m, s) :
                h > 0 ? String.format("%02d:%02d:%02d", h, m, s) :
                        String.format("%02d:%02d", m, s);
    }
    
    private int getPrice() {
    	try {
    		URL url = new URL("http://services.runescape.com/m=itemdb_oldschool/api/catalogue/detail.json?item=1739");            
    		URLConnection con = url.openConnection();
            con.setUseCaches(true);
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String[] data = br.readLine().replace("{", "").replace("}", "").split(",");
            int i = Integer.parseInt(data[8].split(":")[1]);
            return i;
        } catch(Exception e) {
            log(e);
        }
        return -1;
    }
    
    private void updateHideCount() {
        long hideInvcount = getInventory().getAmount("Cowhide");
        hideCount += (hideInvcount - prevHideCount);
        prevHideCount = hideInvcount;
    }
}