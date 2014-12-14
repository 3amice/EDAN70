package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import jules.Categorizer;
import jules.QueryPassager;
import jules.RankNouns;
import jules.Reranker;
import jules.ScoreWord;
import tagging.PosTagger;
import tagging.Word;
import util.Constants;
import util.Pair;

public class TestRerank {
	
	static Map<String, String> questions;
	static PrintWriter writer;
	static int queries = 100;
	
	public static void setUp() throws Exception {
		questions = new HashMap<String, String>();
		File dir = new File(Constants.qDir);
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String outFile = queries + "P:" + sdf.format(date) + ".txt";
		System.out.println("OutFile: " + outFile);
		writer = new PrintWriter(outFile, "UTF-8");
		for (File f : dir.listFiles()) {
			if(f.getName().startsWith(".")) continue;
			System.out.println("Reading file: " + f.getName());
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\t");
				if (!cols[6].trim().contains(" "))
					questions.put(cols[5].trim().toLowerCase(), cols[6].trim()
							.toLowerCase());
			}
			br.close();
		}
	}
	
	public static void testReranker() throws Exception {
		setUp();
		//writer.println(Integer.toString(questions.entrySet().size()));
		for (Entry<String, String> question : questions.entrySet()) {
			List<Map<String, String>> list = QueryPassager.query(question.getKey(), queries);
			List<ScoreWord> topN = RankNouns.findTopNouns(list);
			List<Pair<String, Double>> cat;
			try{
				cat = Categorizer.getCategories(question.getKey());
			}catch(Exception e){
				continue;
			}
			if(cat==null)
				continue;
			Reranker ins = Reranker.getInstance();
			PosTagger tagger = PosTagger.getInstance();
			List<Word[]> words = tagger.tagString(question.getKey());
			List<String> qLemmas = new ArrayList<String>();
			for(Word w : words.get(0)){
				qLemmas.add(w.lemma);
			}
			List<ScoreWord> results = ins.rerank(topN, qLemmas, cat);
			int i = 0;
			for (ScoreWord sw : results) {
				String s = sw.lemma;
				i++;
				if (s.equalsIgnoreCase(question.getValue())){
					//			  index found 		rank				total number of nouns
					writer.println(i + "\t" + sw.getTotalRank() + "\t" + results.size() + "\t" + question.getKey());
					writer.flush();
					break;
				}
			}
		}
		writer.close();
	}


}
