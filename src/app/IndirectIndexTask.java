package app;

import java.util.HashMap;
import java.util.List;

import org.bson.Document;

public class IndirectIndexTask implements Runnable {
    private Document mongoDoc;
    private static volatile HashMap<String, List<String>> indirectIndex;

    public IndirectIndexTask(Document document) {
        super();
        mongoDoc = document;
        indirectIndex = new HashMap<String, List<String>>();
    }

    @Override
    public void run() {
        
    }

}