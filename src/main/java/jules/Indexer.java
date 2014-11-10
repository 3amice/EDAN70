package jules;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

public class Indexer {
    public static void Main(String[] args){
        Indexer indexer = new Indexer();
        indexer.index();
        //indexer.query("kung");
    }
    private String wikiFile;
    private String indexDir;


    public Indexer(){

        //wikiFile = "./sewiki.xml";
        //indexDir = "./indexDir_test/";
        wikiFile = "./sewiki-20141104-pages-meta-current.xml";
        indexDir = "./merisnuc/";
    }


    public String query(String querystr) {
        System.out.println("Querying: " + querystr);
        Analyzer analyzer = new StandardAnalyzer();

        QueryParser qp = new QueryParser("title", analyzer);
        Query query = null;
        try {
            query = qp.parse(querystr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // 3. search
        int hitsPerPage = 10;
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
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = null;
            try {
                d = searcher.doc(docId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("=========================================================================================");
            System.out.println("Score: " + hits[i].score);
            for (IndexableField field : d.getFields()) {
                System.out.println(field.name() + ": " + d.getField(field.name()).stringValue());
            }
            System.out.println("=========================================================================================");
        }

        return "hej";
    }

    public void index(){
        //WikiXMLParser wxp = WikiXMLParserFactory.getDOMParser(wikiFile);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try{
            final IndexWriter writer = new IndexWriter(FSDirectory.open(new File(indexDir)), iwc);
            WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(wikiFile);
            wxsp.setPageCallback(new PageCallbackHandler() {
                @Override
                public void process(WikiPage page) {
                    // We don't want to store unnecessary pages.
                    // text starts with #OMDIRIGERING
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
