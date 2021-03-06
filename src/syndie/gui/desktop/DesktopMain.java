package syndie.gui.desktop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import syndie.util.Timer;
import syndie.db.JobRunner;
import syndie.db.TextEngine;
import syndie.db.TextUI;
import syndie.db.UI;
import syndie.gui.*;

/** swt's readAndDispatch needs to be in the main thread */
public class DesktopMain {
    public static void main(final String args[]) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < args.length; i++) {
            if ("--cli".equals(args[i])) {
                TextUI.main(args);
                return;
            }
        }
        System.setProperty("jbigi.dontLog", "true");
        System.setProperty("jcpuid.dontLog", "true");
        // we don't need I2P's 4MB of strong PRNG data.  4KB will do
        System.setProperty("prng.bufsize", "1024");
        System.setProperty("prng.buffers", "4");
        
        boolean trackResources = trackResources(args);

        long beforeDisplay = System.currentTimeMillis();
        // does nothing as of Java 5 (findbugs)
        //Class cls = Display.class;
        long afterLoad = System.currentTimeMillis();
        Display d = null;
        if (trackResources) {
            DeviceData data = new DeviceData();
            data.tracking = trackResources;
            d = new Display(data);
        } else {
            d = new Display();
        }

        long afterDisplay = System.currentTimeMillis();
        
        String root = TextEngine.getRootPath();
        if (args.length > 0)
            root = args[0];
        
        File rootFile = new File(root);
        if (rootFile.exists() && !rootFile.isDirectory()) {
            System.err.println("Syndie data directory is not a directory: " + rootFile);
            System.exit(-1);
        }
        
        // this way the logs won't go to ./logs/log-#.txt (i2p's default)
        // (this has to be set before the I2PAppContext instantiates the LogManager)
        System.setProperty("loggerFilenameOverride", root + "/logs/syndie-log-#.txt");
   
        long beforeUI = System.currentTimeMillis();
        DesktopUI ui = new DesktopUI();
        Timer timer = new Timer("startup", ui);
        long now = System.currentTimeMillis();
        timer.addEvent("main to timer startup: " + (now-start) + " track time: " + (beforeDisplay-start) + " load time: " + (afterLoad-beforeDisplay) + " display time: " + (afterDisplay-afterLoad) + " dir time: " + (beforeUI - afterDisplay) + " ui time: " + (now-beforeUI));
        JobRunner.instance().setUI(ui);
        d.syncExec(new Runnable() { public void run() { ColorUtil.init(); } });
        timer.addEvent("color init");
        Desktop desktop = new Desktop(rootFile, ui, d, timer);
        
        loop(d, ui);
    }
    private static void loop(Display d, UI ui) {
        while (!d.isDisposed()) {
            try { 
                if (!d.readAndDispatch()) d.sleep(); 
            } catch (RuntimeException e) {
                System.out.println(now() + ": uncaught error");
                e.printStackTrace();
                ui.errorMessage("Internal error: " + e.getMessage(), e);
            }
        }
    }
    
    private static final SimpleDateFormat _fmt = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");
    private static final String now() {
        Date now = new Date(System.currentTimeMillis());
        synchronized (_fmt) { return _fmt.format(now); }
    }

    private static final boolean trackResources(String args[]) {
        // see browser.dumpResources
        //if (true) return true;
        if (args != null)
            for (int i = 0; i < args.length; i++)
                if ("--trackresources".equalsIgnoreCase(args[i]))
                    return true;
        return false;
    }
}
