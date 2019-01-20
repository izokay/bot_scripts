package core;

import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import java.awt.*;

@ScriptManifest(author = "LS", info = "Fill water jugs", name = "Water Filler", version = 0, logo = "")
public class main extends Script {

    private Font titleFont = new Font("Sans-Serif", Font.BOLD, 10);
    private long startTime;
    private int trips = 0;

    @Override
    public void onPaint(Graphics2D g) {
        g.setFont(titleFont);
        g.setColor(Color.WHITE);
        g.drawRect(mouse.getPosition().x - 3, mouse.getPosition().y - 3, 6, 6);
        
        g.drawString("Water Filler v1.0 - Written by LS", 10, 290);
        g.drawString("Trips: " + trips, 10, 305);
        g.drawString("Jugs / hr: " + (int)((trips * 28) / ((System.currentTimeMillis() - startTime) / 3600000.0)), 10, 320);
        g.drawString("Runtime: " + formatTime(System.currentTimeMillis() - startTime), 10, 335);
    }

    @Override
    public void onStart() {
    	startTime = System.currentTimeMillis();
    }

    @Override
    public int onLoop() throws InterruptedException {
    	
    	RS2Object bankChest = objects.closest("Bank chest");
    	do {
			bankChest.interact("Use");
			sleep(1200);
			bank.depositAll();
		} while (getInventory().contains("Jug of water"));
    	
    	bank.withdraw("Jug", 28);
    	bank.close();
    	
    	sleep(300);
    	
    	Item jug = inventory.getItem("Jug");
    	jug.interact("Use");
    	
    	RS2Object fountain = objects.closest("Fountain");
    	fountain.interact();
    	
    	while (inventory.contains("Jug")) {
    		int oldJugs = (int) inventory.getAmount("Jug");
    		if (oldJugs == 0) {
    			break;
    		}
    		sleep(3000);
    		if (inventory.getAmount("Jug") >= oldJugs) {
    			jug.interact("Use");
    			fountain.interact();
    		}
    	}
    	
    	trips += 1;    	
    	return 500;
    }
    
    private String formatTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        s %= 60; m %= 60; h %= 24;

        return d > 0 ? String.format("%02d:%02d:%02d:%02d", d, h, m, s) :
                h > 0 ? String.format("%02d:%02d:%02d", h, m, s) :
                        String.format("%02d:%02d", m, s);
    }

}