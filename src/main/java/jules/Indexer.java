package jules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.jhu.nlp.wikipedia.*;

//import org.sweble.*;
//import org.sweble.wom3.*;
//import org.sweble.
public class Indexer {
	public static void main (String[] args){
		while(true){
			System.out.println("Enter query:");
			System.out.print("> ");
			query(System.console().readLine(), false);
		}
	}
    private static String wikiFile = "./sewiki-20141104-pages-meta-current.xml";
    private static String indexDir = "./indexDir/";

    public static List<Map<String, String>> query(String querystr, boolean silent) {
        print("Querying: " + querystr, silent);
        Analyzer analyzer = new SwedishAnalyzer();

        QueryParser qp = new QueryParser("title", analyzer);
        Query query = null;
        try {
            query = qp.parse(querystr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // 3. search
        int hitsPerPage = 3;
        IndexReader reader = null;

        try {
            reader = DirectoryReader.open((FSDirectory.open(new File(indexDir))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        try {
            searcher.search(query, collector);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        // 4. display results
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        print("Found " + hits.length + " hits.", silent);
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = null;
            try {
                d = searcher.doc(docId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Map<String, String> tmpResult = new HashMap<String, String>();
            print("=========================================================================================", silent);
            print("Score: " + hits[i].score, silent);
            tmpResult.put("Score", Float.toString(hits[i].score));
            for (IndexableField field : d.getFields()) {
                print(field.name() + ": " + field.stringValue(), silent);
                tmpResult.put(field.name(), field.stringValue());
            }
            print("=========================================================================================", silent);
            results.add(tmpResult);
        }

        return results;
    }
    
    private static void print(String s, boolean silent){
    	if(!silent)
    		System.out.println(s);
    }

    public static void index(){
        Analyzer analyzer = new SwedishAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try{
            final IndexWriter writer = new IndexWriter(FSDirectory.open(new File(indexDir)), iwc);
            WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(wikiFile);
            wxsp.setPageCallback(new PageCallbackHandler() {
                @Override
                public void process(WikiPage page) {
                	
                	page.getText().split("=+ [A-ZÅÄÖa-zåäö]+ =+");
                	/*
                	 * We don't want to store unnecessary pages.
                	 * Text starts with #OMDIRIGERING
                	 */
                    if (page.isDisambiguationPage() || page.isRedirect() || page.isSpecialPage()
                        || page.isStub() || page.getTitle().startsWith("Användare:") || page.getTitle().startsWith("Användardiskussion:")
                        || page.getText().trim().toLowerCase().startsWith("#redirect") || page.getText().trim().startsWith("#omdirig")
                        || page.getID() == null || page.getID().length() == 0
                    ) {
                    	System.out.println(page.getTitle());
                        return;
                    }
                    Document doc = new Document();
                    doc.add(new IntField("id", Integer.parseInt(page.getID()), Field.Store.YES));
                    doc.add(new TextField("title", page.getTitle(), Field.Store.YES));
                    doc.add(new TextField("text", page.getText(), Field.Store.YES));
                    
                    /**
                     * Should we add:
                     * Infobox, 
                     * Links, 
                     * Categories?
                     */

                    if (doc.getField("text").stringValue().toLowerCase().startsWith("#omdirigering"))
                        return;

                    try {
                        writer.addDocument(doc);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            wxsp.parse();
            writer.commit();
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
