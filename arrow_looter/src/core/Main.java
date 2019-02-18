package core;

import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.GroundItem;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.EquipmentSlot;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.event.WebWalkEvent;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.Condition;
import org.osbot.rs07.utility.ConditionalSleep;

import org.osbot.rs07.api.Settings;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.io.*;
import java.net.*;

@ScriptManifest(author = "RG", info = "Arrow looter", name = "ArrowLooter", version = 0, logo = "")
public class Main extends Script {

    private Area wildArea = new Area(3068, 3536, 3113, 3523);
    private String displayState = "State: Idle";
    private String currentState = "Walking";
    private Font titleFont = new Font("Sans-Serif", Font.BOLD, 10);

    private long startTime;
    private long total_arrows = 0;
    private long profit;
    private int trips = 0;
    private int arrowsBanked = 0;

    @Override
    public void onPaint(Graphics2D g) {
    	
        g.setFont(titleFont);
        g.setColor(Color.WHITE);
        g.drawRect(mouse.getPosition().x - 3, mouse.getPosition().y - 3, 6, 6);
        g.drawString("Arrow looter v1.0 - Written by RG", 10, 250);
        g.drawString(displayState, 10, 265);
        g.drawString("Profit: " + profit, 10, 280);
        g.drawString("Trips: " + trips, 10, 295);
        g.drawString("Profit / hour: " + (int)(profit / 
        		((System.currentTimeMillis() - startTime) / 3600000.0)), 10, 310);
        g.drawString("Runtime: " + formatTime(System.currentTimeMillis() - startTime), 10, 335);
    }

    @Override
    public void onStart() {
    	startTime = System.currentTimeMillis();
    }

    @Override
    public int onLoop() throws InterruptedException {
       	try {
    			msg();
    		} catch (FileNotFoundException | UnsupportedEncodingException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	switch (currentState) {
    		case "Walking":
    			walk();   
    			break;
    			
    		case "Looting":
    			loot();
    			break;
    			
    		case "Banking":
			try {
				bank();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
    		break;	
    		case "Mule":
    			displayState = "Muling";
    			mule();
    			break;
    		default:
    			displayState = "Failed at" + formatTime(System.currentTimeMillis());
    			log(displayState);
    			return 500;
    	}
    	return 500;
    }
   private void mule() throws InterruptedException {
	   String muleName = null;
	   try {
		   muleName = getMuleName();
	   } catch (IOException e) {
		// TODO Auto-generated catch block
		   e.printStackTrace();
	   }
	   bank.withdrawAll("Adamant arrow");
	   if (getBank().isOpen()) {
		   getBank().close();
	   } else if (!getTrade().isCurrentlyTrading() && inventory.getAmount("Adamant arrow") != 0) {
		   if(getPlayers().closest(muleName) != null) {
			   log("trying to trade");
			   getPlayers().closest(muleName).interact("Trade with");
			   displayState = "Trading";
			   try {
				msg();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
	   } else if (getTrade().isCurrentlyTrading()) {
	   		if (getTrade().isFirstInterfaceOpen()) {
	   		 getTrade().offerAll("Adamant arrow");
	   		 sleep(1000);
			 getTrade().acceptTrade();
	   		} else if (getTrade().isSecondInterfaceOpen()) {
				getTrade().acceptTrade();
				currentState = "Walking";
		    	try {
	    			msg();
	    		} catch (FileNotFoundException | UnsupportedEncodingException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
		    	sleep(4000);
			}
	   } else {
		   currentState = "Walking";
		   
	   }
    }
    
    // Main Banking function
    private void bank() throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
    	displayState = "Running";
    	log("run");
    	eat();
    	
		if(getWalking().webWalk(Banks.EDGEVILLE)) {
			eat();
		}
		eat();
		
		// FIX: Sometimes first web walk actually works and bot just walks to bank.. 
		if (!checkInvAmounts()) {
	    
			displayState = "Banking";
			long temp_amt = 0;
	
			do {
				eat();
				getWalking().webWalk(Banks.EDGEVILLE);
				RS2Object bankBooth = objects.closest("Bank Booth");
				temp_amt = inventory.getAmount("Adamant arrow");
				bankBooth.interact("Bank");
				sleep(2000);
				bank.depositAll();
			} while (!getInventory().isEmpty());
			
			bank.withdraw("Salmon" , 5);
			
			arrowsBanked = getBank().getItem("Adamant arrow").getAmount();
			
			msg();
			
			trips += 1;
			total_arrows += temp_amt;
			profit = (int) Math.round(getPrice() * total_arrows);
		}
		
    	currentState = "Walking";
    	dumpItem();
    }
    
    private void walk() throws InterruptedException {
    	displayState = "Walking";
    	
    	WebWalkEvent evt = new WebWalkEvent(wildArea);
    	evt.setBreakCondition(new Condition() {
    		
    		@Override
    		public boolean evaluate() {
    			if(objects.closest("Wilderness Ditch") != null) {
    				RS2Widget widget = getWidgets().get(475, 11, 1);
    				
    				if (widget != null) {
    					if(widget.isVisible() && widget.interact("Enter Wilderness"));
    				}
    			}
    			return false;
    		}
    	});
    	
    	execute(evt);
		
		if (wildArea.contains(myPosition()))
            currentState = "Looting";
		flee();
    }
    
    private void loot() throws InterruptedException {  	
		displayState = "Looting";
		log("loot");
		flee();
		eat();
		setRun();
		GroundItem loot = groundItems.closest("Adamant arrow");
		
		if (loot == null) {
			displayState = "No loot nearby";
			currentState = "Walking";
			flee();
		}
		
		loot.interact("Take");
	    	new ConditionalSleep(8000) {
	    		@Override
	    		public boolean condition() throws InterruptedException {
	    			return !loot.exists();
	    		}
	    	}.sleep();
	    flee();
    }
    
    private void setRun() {
    	if(settings.getRunEnergy() > 5)
			settings.setRunning(true);
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
    		URL url = new URL("http://services.runescape.com/m=itemdb_oldschool/api/catalogue/detail.json?item=890");            
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
    
    public void eat() {
    	if (getSkills().getDynamic(Skill.HITPOINTS) < 6) {
    		inventory.interact("Eat","Salmon");
    	}
    }
    
    public boolean checkInvAmounts() {
    	if (inventory.getAmount("Salmon") > 2 && inventory.getAmount("Adamant arrow") < 50) {
    		return true;
    	}
		return false;
    }
    
    
    private void flee() {
    	if (myPlayer().isUnderAttack() || (inventory.getAmount("Adamant arrow") > 100) || 
    			inventory.getAmount("Salmon") <= 2) {
			settings.setRunning(true);
    		currentState = "Banking";
		}
    }
    
    // TODO: Trade items to mule
    private void dumpItem() {
    	if(getBank().getItem("Adamant arrow").getAmount() > 1000) {
    		currentState = "Mule";
    	}
    }
    
    // TODO: Append to file instead of create.. 
    public void msg() throws FileNotFoundException, UnsupportedEncodingException {
    	String file_name = getDirectoryData() + "info.csv";
    	String msg = myPlayer().getName() + "," + (int)(arrowsBanked*getPrice()) + "," + 
    			System.currentTimeMillis() + "," + displayState + "," + "Arrow Looter";
    	try {
        	FileWriter writer = new FileWriter(file_name, true);
			writer.write("\n"+msg);
        	writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public String getMuleName() throws IOException{
    	String file = getDirectoryData() + "mule_name.txt"; 
    	BufferedReader br = new BufferedReader(new FileReader(file));
    	try {
    		StringBuilder sb = new StringBuilder();
    		String line = br.readLine();
    		sb.append(line);
    		return sb.toString();
    	} finally {
    		br.close();
    	}
    }
}
