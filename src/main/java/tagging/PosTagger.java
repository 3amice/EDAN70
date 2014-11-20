package tagging;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import se.su.ling.stagger.SwedishTokenizer;
import se.su.ling.stagger.TagNameException;
import se.su.ling.stagger.TagSet;
import se.su.ling.stagger.TaggedToken;
import se.su.ling.stagger.Tagger;
import se.su.ling.stagger.Token;
import se.su.ling.stagger.Tokenizer;

public class PosTagger {
	public static void test() throws ClassNotFoundException, IOException,
			TagNameException {
		String s = "Det var en gång en katt som hette Nils.";
		try {
			PosTagger t = new PosTagger();
			List<Word[]> res = t.tagString(s);
			for (Word[] sent : res) {
				for (Word token : sent) {
					System.out.println(token);
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}
	
	private Tagger tagger;
	private boolean silent;
	
	public PosTagger() throws IOException{
		this.silent = true;
		String modelFile = "./model/swedish.bin";

		ObjectInputStream modelReader;
		try {
			modelReader = new ObjectInputStream(new FileInputStream(modelFile));
		} catch (Exception e) {
			throw new IOException("Couldn't load modelfile");
		}
		print("Loading Stagger model ...");
		
		try {
			tagger = (Tagger) modelReader.readObject();
		} catch (Exception e) {
			modelReader.close();
			throw new IOException("Model file found but unable to be loaded.");
		}
		print("Model loaded!");
		modelReader.close();
	}

	public List<Word[]> tagString(String document) throws IOException {
		print(document);
		BufferedReader reader = new BufferedReader(new StringReader(document));

		Tokenizer tokenizer = new SwedishTokenizer(reader);
		ArrayList<Word[]> taggedSents = new ArrayList<Word[]>();
		ArrayList<Token> sentence;
		int sentIdx = 0;
		while ((sentence = tokenizer.readSentence()) != null) {
			TaggedToken[] sent = new TaggedToken[sentence.size()];
			for (int j = 0; j < sentence.size(); j++) {
				Token tok = sentence.get(j);
				String id;
				id = sentIdx + ":" + tok.offset;
				sent[j] = new TaggedToken(tok, id);
			}
			TaggedToken[] taggedSent = tagger.tagSentence(sent, true, false);
			TagSet tagset = tagger.getTaggedData().getPosTagSet();
			Word[] words = new Word[taggedSent.length];
			for (int i = 0; i < taggedSent.length; i++) {
				TaggedToken token = taggedSent[i];
				String posTag;
				try {
					posTag = tagset.getTagName(token.posTag).split("\\|")[0];
				} catch (TagNameException e) {
					posTag = null; // Todo: determine which type the
									// empty(unknown) postag should have
				}
				words[i] = new Word(token.token.value, token.lf, posTag);
			}
			taggedSents.add(words);

			sentIdx++;
		}

		tokenizer.yyclose();
		return taggedSents;
	}

	private void print(String s) {
		if (!silent)
			System.out.println(s);
	}
}
