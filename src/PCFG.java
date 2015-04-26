import org.json.*;

import java.io.*;
import java.util.*;

public class PCFG
{
	static HashSet<String> commonSet = new HashSet<String>();

	static HashMap<String, Double> UnaryProbability = new HashMap<String, Double>();
	static HashMap<String, Double> BinaryProbability = new HashMap<String, Double>();

	static HashSet<String> NonterminalSet = new HashSet<String>();
	static HashMap<String, HashSet<String>> X_YZ_Map = new HashMap<String, HashSet<String>>();

	public static void removeStopword(JSONArray ja) throws Exception
	{
		if (ja.length() == 2)
		{
			String word = ja.getString(1);
			if (commonSet.contains(word) == false)
			{
				ja.put(1, "_RARE_");
			}
		}
		else
		{
			removeStopword(ja.getJSONArray(1));
			removeStopword(ja.getJSONArray(2));
		}
	}

	// 过滤小于5的词
	public static void part1(String cntPath, String trainPath, String dstPath)
			throws Exception
	{
		HashMap<String, Integer> wordCntMap = new HashMap<String, Integer>();
		HashSet<String> rareSet = new HashSet<String>();

		BufferedReader br = new BufferedReader(new FileReader(cntPath));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			if (strs[1].equals("UNARYRULE"))
			{
				String word = strs[3];
				int cnt = Integer.parseInt(strs[0]);

				if (wordCntMap.containsKey(word) == false)
				{
					wordCntMap.put(word, cnt);
				}
				else
				{
					wordCntMap.put(word, wordCntMap.get(word) + cnt);
				}
			}
		}
		br.close();

		for (Map.Entry<String, Integer> entry : wordCntMap.entrySet())
		{
			String word = entry.getKey();
			int cnt = entry.getValue();
			if (cnt >= 5)
			{
				commonSet.add(word);
			}
			else
			{
				rareSet.add(word);
			}
		}

		FileWriter fw = new FileWriter(dstPath);
		br = new BufferedReader(new FileReader(trainPath));
		while ((line = br.readLine()) != null)
		{
			JSONArray ja = new JSONArray(line);
			removeStopword(ja);
			fw.write(ja + "\n");
		}
		br.close();
		fw.close();
	}

	static double q(String x, String y) throws Exception
	{
		String key = x + " " + y;
		if (UnaryProbability.containsKey(key))
		{
			return UnaryProbability.get(key);
		}
		else
		{
			return 0.0;
		}
	}

	static double q(String x, String y, String z) throws Exception
	{
		String key = x + " " + y + " " + z;
		if (BinaryProbability.containsKey(key))
		{
			return BinaryProbability.get(key);
		}
		else
		{
			return 0.0;
		}
	}

	static JSONArray backtrace(String[] raw_x, HashMap<String, String> bp,
			int start, int end, String XX) throws Exception
	{
		if (start == end)
		{
			JSONArray array = new JSONArray();
			array.put(XX);
			array.put(raw_x[start]);
			return array;
		}

		JSONArray array = new JSONArray();
		String tmp = bp.get(start + " " + end + " " + XX);
		String[] tmps = tmp.split(" ");
		String X = tmps[0];
		String Y = tmps[1];
		String Z = tmps[2];
		int s = Integer.parseInt(tmps[4]);

		JSONArray left = backtrace(raw_x, bp, start, s, Y);
		JSONArray right = backtrace(raw_x, bp, s + 1, end, Z);
		array.put(X);
		array.put(left);
		array.put(right);
		return array;
	}

	public static JSONArray Cky(String[] strs) throws Exception
	{
		int n = strs.length;
		String[] x = new String[n + 1];
		String[] raw_x = new String[n + 1];
		x[0] = "";
		raw_x[0] = "";
		for (int i = 0; i < strs.length; ++i)
		{
			if (commonSet.contains(strs[i]))
			{
				x[i + 1] = strs[i];
			}
			else
			{
				x[i + 1] = "_RARE_";
			}
			raw_x[i + 1] = strs[i];
		}

		HashMap<String, Double> pi = new HashMap<String, Double>();
		HashMap<String, String> bp = new HashMap<String, String>();

		// 初始化pi和bp
		for (int i = 1; i <= n; ++i)
		{

			for (String X : NonterminalSet)
			{
				String key = X + " " + x[i];
				if (UnaryProbability.containsKey(key))
				{
					pi.put(i + " " + i + " " + X, q(X, x[i]));
				}
				else
				{
					pi.put(i + " " + i + " " + X, 0d);
				}
			}
		}

		// 动态规划
		for (int l = 1; l <= n - 1; ++l) // 长度
		{
			for (int i = 1; i <= n - l; ++i)
			{
				int j = i + l;
				for (String X : NonterminalSet)
				{
					String max_Y = null;
					String max_Z = null;
					int max_s = 1;
					double max_pro = 0.0;

					// 我这一步错了，不应该先弄状态，而应该先弄切割符号

					for (int s = i; s <= j - 1; ++s)
					{
						HashSet<String> YZ_Set = X_YZ_Map.get(X);
						if (YZ_Set == null)
						{
							continue;
						}
						for (String YZ : YZ_Set)
						{
							String[] tmps = YZ.split(" ");
							String Y = tmps[0];
							String Z = tmps[1];
							double pro = q(X, Y, Z)
									* pi.get(i + " " + s + " " + Y)
									* pi.get((s + 1) + " " + j + " " + Z);
							if (pro > max_pro)
							{
								max_Y = Y;
								max_Z = Z;
								max_pro = pro;
								max_s = s;
							}
						}
					}

					pi.put(i + " " + j + " " + X, max_pro);
					bp.put(i + " " + j + " " + X, X + " " + max_Y + " " + max_Z
							+ " " + i + " " + max_s + " " + j);
				}
			}
		}

		//解析成json
		return backtrace(raw_x, bp, 1, n, "SBARQ");
	}

	// cky
	public static void part2(String cntPath, String testPath, String resultPath)
			throws Exception
	{
		HashMap<String, Integer> NonterminalCnt = new HashMap<String, Integer>();
		HashMap<String, Integer> UnaryCnt = new HashMap<String, Integer>();
		HashMap<String, Integer> BinaryCnt = new HashMap<String, Integer>();

		BufferedReader br = new BufferedReader(new FileReader(cntPath));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			int cnt = Integer.parseInt(strs[0]);
			if (strs[1].equals("NONTERMINAL"))
			{
				NonterminalCnt.put(strs[2], cnt);
				NonterminalSet.add(strs[2]);
			}
			else if (strs[1].equals("UNARYRULE"))
			{
				UnaryCnt.put(strs[2] + " " + strs[3], cnt);
			}
			else if (strs[1].equals("BINARYRULE"))
			{
				BinaryCnt.put(strs[2] + " " + strs[3] + " " + strs[4], cnt);
			}
		}
		br.close();

		for (Map.Entry<String, Integer> entry : UnaryCnt.entrySet())
		{
			String key = entry.getKey();
			String[] tmps = key.split(" ");
			String nonterminal = tmps[0];

			int unaryCnt = entry.getValue();
			int nonterminalCnt = NonterminalCnt.get(nonterminal);
			double pro = (double) unaryCnt / nonterminalCnt;
			UnaryProbability.put(key, pro);
		}

		for (Map.Entry<String, Integer> entry : BinaryCnt.entrySet())
		{
			String key = entry.getKey();
			String[] tmps = key.split(" ");
			String nonterminal = tmps[0];
			if (X_YZ_Map.containsKey(nonterminal) == false)
			{
				HashSet<String> set = new HashSet<String>();
				set.add(tmps[1] + " " + tmps[2]);
				X_YZ_Map.put(nonterminal, set);
			}
			else
			{
				HashSet<String> set = X_YZ_Map.get(nonterminal);
				set.add(tmps[1] + " " + tmps[2]);
			}
			int binaryCnt = entry.getValue();
			int nonterminalCnt = NonterminalCnt.get(nonterminal);
			double pro = (double) binaryCnt / nonterminalCnt;
			BinaryProbability.put(key, pro);
		}

		FileWriter fw = new FileWriter(resultPath);
		br = new BufferedReader(new FileReader(testPath));
		int index = 0;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			JSONArray result = Cky(strs);
			fw.write(result + "\n");
			System.out.println(++index);
		}
		br.close();
		fw.close();
	}

	public static void initCommonSet(String cntPath) throws Exception
	{
		HashMap<String, Integer> wordCntMap = new HashMap<String, Integer>();
		HashSet<String> rareSet = new HashSet<String>();

		BufferedReader br = new BufferedReader(new FileReader(cntPath));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] strs = line.split(" ");
			if (strs[1].equals("UNARYRULE"))
			{
				String word = strs[3];
				int cnt = Integer.parseInt(strs[0]);

				if (wordCntMap.containsKey(word) == false)
				{
					wordCntMap.put(word, cnt);
				}
				else
				{
					wordCntMap.put(word, wordCntMap.get(word) + cnt);
				}
			}
		}
		br.close();

		for (Map.Entry<String, Integer> entry : wordCntMap.entrySet())
		{
			String word = entry.getKey();
			int cnt = entry.getValue();
			if (cnt >= 5)
			{
				commonSet.add(word);
			}
			else
			{
				rareSet.add(word);
			}
		}
	}

	public static void part3() throws Exception
	{
//		part1("./tmp/cfg.counts.vert", "./data/parse_train_vert.dat",
//						"./tmp/parse_train.dat.rare.vert");
		initCommonSet("./tmp/cfg.counts.vert");
		part2("./tmp/cfg.counts.rare.vert", "./data/parse_dev.dat",
		"./data/parse_dev.out");
	}
	
	public static void main(String[] args) throws Exception
	{
		//				part1("./tmp/cfg.counts", "./data/parse_train.dat",
		//						"./tmp/parse_train.dat.rare");

//		initCommonSet("./tmp/cfg.counts");
//		part2("./tmp/cfg.counts.rare", "./data/parse_dev.dat",
//				"./data/parse_dev.out");
		
		part3();
		
		System.exit(0);
	}

}
