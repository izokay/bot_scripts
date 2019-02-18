package core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

@ScriptManifest(author = "RG", info = "muler", name = "Muler", version = 0, logo = "")
public class Main extends Script {

	private boolean completedTrade = false;
	
    @Override
    public void onStart() {
    	getWalking().webWalk(Banks.EDGEVILLE);
    	try {
			msg();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public int onLoop() throws InterruptedException {
    	try {
			writeToCSV();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if(Banks.EDGEVILLE.contains(myPosition())) { 
    		if (completedTrade) {
    			getLogoutTab().logOut();
    	    	String file_name = getDirectoryData() + "mule_name.txt";
    			File file = new File(file_name);
    			file.delete();
    			System.exit(0);
    		} else if(!getTrade().isCurrentlyTrading()) {
    			if (getTrade().getLastRequestingPlayer() != null) {
    					getTrade().getLastRequestingPlayer().interact("Trade with");    	
    					sleep(500);
    			}
    		} else if (getTrade().isFirstInterfaceOpen()) {
    			if (getTrade().didOtherAcceptTrade()) {
					getTrade().acceptTrade();
				}
    		} else if (getTrade().isSecondInterfaceOpen()) {
    			if (getTrade().didOtherAcceptTrade()) {
					getTrade().acceptTrade();
					completedTrade = true;
				}
    		}
    	}
    	return 500;
    }
    
    public void writeToCSV() throws FileNotFoundException, UnsupportedEncodingException {
    	String fileName = getDirectoryData() + "info.csv";
    	String msg = myPlayer().getName() + "," + getInventory().getAmount("Adamant arrow") + "," + 
    			System.currentTimeMillis() + "," + "-" + "," + "Mule";
    	try {
        	FileWriter writer = new FileWriter(fileName, true);
			writer.write("\n"+msg);
        	writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    
    private void msg() throws IOException {
    	String file_name = getDirectoryData() + "mule_name.txt";
    	String msg = myPlayer().getName();
    	FileWriter writer = new FileWriter(file_name);
    	writer.write(msg);
    	writer.close();
    }
}
