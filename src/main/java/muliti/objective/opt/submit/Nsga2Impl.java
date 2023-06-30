package muliti.objective.opt.submit;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Nsga2Impl
{
	HashMap<Integer, VarInfo> varInfoMap        = new HashMap<Integer, VarInfo>();
	int[]                     OBJ_FN_INDEX_LIST =
	{
			1, 2
	};
	//	List<Integer>               paretoFront       = new ArrayList<Integer>();
	Map<Integer, List<Integer>> dominatedMap            = new HashMap<Integer, List<Integer>>();
	int                         sampleSize              = 20;
	int                         programRunCountMax      = 2;
	double                      crossingProbability     = 0.5;
	Map<Integer, List<Integer>> rankedParetoMap         = new HashMap<Integer, List<Integer>>();
	List<BitString>             newGeneration;
	List<Integer>               previousParetoFront;
	List<Integer>               currentParetoFront;
	int                         programRunCount         = 0;
	int                         paretoFrontSameCount    = 0;
	int                         paretoFrontSameCountMax = 10;
	GraphData                   gd;
	static int                  gdRunCount              = 0;
	boolean                     isInitialRun            = true;
	int                         rankCount               = 0;
	private boolean             isDebug                 = false;

	

	public static void main(String[] args)
	{
		Nsga2Impl nsga2 = new Nsga2Impl();
		//		/**** test samplePopulation ****/

		nsga2.iterateProgram(nsga2.isDebug,nsga2.programRunCountMax,nsga2.sampleSize);
		// Test binary 
		//		nsga2.testBinaryEncoding();
	}


	public GraphData iterateProgram(boolean isDebug, int programRunCountMax, int sampleSize)
	{
		
		this.isDebug = isDebug;
		this.programRunCountMax=programRunCountMax<0?this.programRunCountMax:programRunCountMax;
		this.sampleSize=sampleSize<0?this.sampleSize:sampleSize;
		for (int i = 0; i < programRunCountMax; i++)
		{
			testRandomGenerationOfPopulation();
			
			programRunCount++;
			if (currentParetoFront != null && previousParetoFront != null)
			{
				if (isListSame(currentParetoFront, previousParetoFront))
					paretoFrontSameCount++;
			}

			if (paretoFrontSameCount >= paretoFrontSameCountMax)
			{
				break;
			}
		}

		String s = " Program run count=" + programRunCountMax + "\n" + " TotalParetoRankCount =" + rankedParetoMap.keySet().size()
				+ "\n" + " ProgramRunCountMax=" + programRunCountMax+" "+		printAllRankedPareto();


		;

		//		String s = " Program run count=" + programRunCount + "\n" + " paretoFrontSameCount=" + paretoFrontSameCount
//				+ "\n" + " ProgramRunCountMax=" + programRunCountMax
//
//		;
		System.out.println(s);
		return gd;
	}

	public void testRandomGenerationOfPopulation()
	{
		rankedParetoMap = new HashMap<Integer, List<Integer>>();
		rankCount       = 0;
		dominatedMap    = new HashMap<Integer, List<Integer>>();
		if (isInitialRun)
		{
			this.newGeneration = getRandomGeneration();
			isInitialRun       = false;
		}
		Collections.sort(newGeneration, getSortComparator(SORT_ORDER.ASC));

		//		printBitStringList(sampleRh, OBJ_FN_INDEX_LIST);
		List<Integer>   indexList   = getShuffledIndexList(sampleSize);
		List<BitString> crossedList = new ArrayList<BitString>();
		for (int i = 0; i < sampleSize; i = i + 2)
		{
			int                  i1   = indexList.get(i);
			int                  i2   = indexList.get(i + 1);
			Map<Integer, String> mm1  = splitStringBits(newGeneration.get(i1).getBitString(), OBJ_FN_INDEX_LIST);
			Map<Integer, String> mm2  = splitStringBits(newGeneration.get(i2).getBitString(), OBJ_FN_INDEX_LIST);
			Map<Integer, String> mmc1 = getCrossed(mm1.get(1), mm2.get(1), crossingProbability);                 // cross var 1 
			Map<Integer, String> mmc2 = getCrossed(mm1.get(2), mm2.get(2), crossingProbability);                 // cross var 2
			crossedList.add(new BitString(i1, mmc1.get(1) + mmc2.get(1)));
			crossedList.add(new BitString(i2, mmc1.get(2) + mmc2.get(2)));
		}
				print("====================================CROSSOVER with "+crossingProbability+" crossing probability ====================================");
				printBitStringList(crossedList, OBJ_FN_INDEX_LIST);
				print("====================================MUTATION : with "+0.125+" mutation probability====================================");

		ArrayList<BitString> mutatedList  = new ArrayList<BitString>();
		for (int i = 0; i < sampleSize; i++)
		{
			String               s        = crossedList.get(i).getBitString();
			Map<Integer, String> mm1      = splitStringBits(s, OBJ_FN_INDEX_LIST);
			String               mutationMask = getMutationMask(mm1.get(1),varInfoMap.get(1).getBitLength());

			String               mutated1 = mutate(mm1.get(1), mutationMask);
            mutationMask = getMutationMask(mm1.get(2),varInfoMap.get(1).getBitLength());

			String               mutated2 = mutate(mm1.get(2), mutationMask);
			mutatedList.add(new BitString(i, mutated1 + mutated2));
						showMutationDetails(s,mutationMask, mm1, mutated1, mutated2);

		}
		print("====================================MUTATED @:0.125  Val   Accurate  mutated       VALUE   Accurate ====================================");

				printMutatedList(crossedList, mutatedList, OBJ_FN_INDEX_LIST);
		/********COMBINE PARENT & CHILD GENERATION**********/
		ArrayList<BitString> parentChildList = new ArrayList<BitString>();
		parentChildList.addAll(mutatedList);
		parentChildList.addAll(newGeneration);
		while (getConsolidatedParitoList(-1).size() < this.sampleSize)
		{
			computeParetoRank(parentChildList);
		}
		List<Integer> consolidatedParitoList = getConsolidatedParitoList(-1);
		print("ConsolidatedParito count=" + consolidatedParitoList.size());

		/***********Display Data *********/
		this.gd            = getGraphData(parentChildList);
		this.newGeneration = getNextGeneration(parentChildList);

	}
	
	
	public GraphData fetchDataForJavaFx()
	{
		testRandomGenerationOfPopulation();
		gdRunCount++;
		this.gd.setChangeCount(gdRunCount);
		String s = " Program run count=" + programRunCountMax + "\n" + " TotalParetoRankCount =" + rankedParetoMap.keySet().size()
				+ "\n" + " ProgramRunCountMax=" + programRunCountMax

		;
		System.out.println(s);
		return this.gd;

	}
public String printAllRankedPareto()
{
	StringBuilder sb =new StringBuilder();
	for(int i : rankedParetoMap.keySet())
	{
		sb.append("rank="+i+ " "+ getStringOfList(rankedParetoMap.get(i), "Pareto Front")+"\n" );
		
	}
	return sb.toString();
}
	int                      rankShowLimit = 6;

	public GraphData getGraphData(ArrayList<BitString> parentChildList)
	{

		/***********Display Data *********/
		Map<String, List<Point>> pointMap      = new HashMap<String, List<Point>>();
		int                      rankShowCount = 0;
		for (Integer rnk : rankedParetoMap.keySet())
		{
			if (rankShowCount == rankShowLimit)
				break;
			List<Integer> paretoFront = rankedParetoMap.get(rnk);
			pointMap.put("Parito_List" + rnk, getPointList(parentChildList, paretoFront, true));
			rankShowCount++;

		}

		pointMap.put("Population", getPointList(parentChildList, getConsolidatedParitoList(rankShowLimit), false));

		GraphData gd = null;
//		gd = new GraphData("Parito Front", "Time", "Cost", "Time Vs Cost Optimization", pointMap);
		gd = new GraphData("Parito Front", "Latency", "ClockSync", "Latency Vs Clock Sync Optimization PopSize="+sampleSize+" iteration="+programRunCountMax, pointMap);
		//gd.setMinX(minX);
		return gd;

	}
	public boolean isListSame(List<Integer> fL, List<Integer> sL)
	{
		if (fL != null && sL != null && fL.size() == sL.size())
		{
			for (int i = 0; i < fL.size(); i++)
			{
				if (fL.get(i) != sL.get(i))
					return false;
			}
			return true;

		}
		return false;
	}
	public List<BitString> getRandomGeneration()
	{
//		Clock min 38,04672088	max : 39,03974233
		varInfoMap.put(1, new VarInfo(1, false, 2, "r", 1.0, 500.0));
		varInfoMap.put(2, new VarInfo(2, false, 2, "h", 380.46, 390.39));

		ArrayList<BitString> sampleR = getRandomSample(sampleSize, varInfoMap.get(1));

		ArrayList<BitString> sampleRh = getRandomSample(sampleSize, varInfoMap.get(2), sampleR);
		return sampleRh;
	}

	public List<BitString> getBitStringListByIndex(List<BitString> parentChildList, List<Integer> indexList)
	{

		List<BitString> bitList = new ArrayList<BitString>();
		for (int i : indexList)
		{
			bitList.add(parentChildList.get(i));
		}
		return bitList;
	}

	public void computeParetoRank(ArrayList<BitString> mutatedList)
	{
		List<Integer> paretoFront = getParetoFront(mutatedList);

		this.rankedParetoMap.put(rankCount, paretoFront);
		rankCount++;
		print(getStringOfList(paretoFront, "ParetoFront"));

	}

	String getMutationMask(String strBit,int maxLen)
	{
		int           bitSum = 0;
		List<Integer> mask   = new ArrayList<>();
		while (bitSum < 1)
		{
			mask = new ArrayList<Integer>();

			for (int i = 0; i < maxLen - 3; i++)
			{
				if (i == 2 || i == 5)
				{
					mask.add(i, 1);
					bitSum += 1;
				}

				else
					mask.add(0);
			}
		}
		Collections.shuffle(mask);
		Collections.shuffle(mask);
		int maskSize=mask.size();
		int strBitSize=strBit.length();
		for( int i=0;i<(strBitSize-maskSize);i++)
		mask.add(0);
//		mask.add(0);
//		mask.add(0);
		StringBuilder sb = new StringBuilder();
		for (int n : mask)
			sb.append(n);

		return sb.toString();
	}

	public String mutate(String strBit, String musK)
	{
		StringBuilder sb = new StringBuilder();
	try {
		if(strBit.length()!=musK.length())
			throw new Exception("Musk and strBit lengths are not equal ! ");
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
		for (int i = 0; i < strBit.length(); i++)
		{
			char c = strBit.charAt(i);
			if (musK.charAt(i) == '1')
			{
				// toggle c
				if (c == '1')
					c = '0';
				else
					c = '1';
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public String getStringOfArray(int[] actressList, String... title)
	{
		StringBuilder sb = new StringBuilder();

		if (actressList != null)
		{
			if (title != null && title.length > 0)
				sb.append(title[0]);
			sb.append("[ ");
			int i = 0;
			for (int a : actressList)
			{
				if (i > 0)
					sb.append(" ");
				sb.append(a);
				i++;
			}
			sb.append(" ]");
		}
		return sb.toString();
	}

	public String getStringOfList(List<Integer> list, String... title)
	{
		StringBuilder sb = new StringBuilder();

		if (list != null)
		{
			if (title != null && title.length > 0)
				sb.append(title[0]);
			sb.append("[ ");
			int i = 0;
			for (int a : list)
			{
				if (i > 0)
					sb.append(" ");
				sb.append(a);
				i++;
			}
			sb.append(" ]");
		}
		return sb.toString();
	}

	public Map<Integer, Double> getNumericValues(String a)
	{

		Map<Integer, String> mA = splitStringBits(a, OBJ_FN_INDEX_LIST);
		// get BitValues in double
		Map<Integer, Double> aD = getBitValues(mA);
		return aD;
	}

	// when isInclusion=True , it returns only the points in paritoList , otherwise it exluded the points in paritoList
	public List<Point> getPointList(List<BitString> mutatedList, List<Integer> paritoList, boolean onlyParetoPoints)
	{

		List<Point> pointList = new ArrayList<Point>();

		if (onlyParetoPoints)
		{
			for (int i : paritoList)
			{
				String a = mutatedList.get(i).getBitString();

				Map<Integer, Double> aD = getNumericValues(a);
				Point                p  = new Point(aD.get(1), aD.get(2));
				pointList.add(p);

			}
		} else
		{
			for (int i = 0; i < mutatedList.size(); i++)
			{
				if (!paritoList.contains(i))
				{
					String a = mutatedList.get(i).getBitString();

					Map<Integer, Double> aD = getNumericValues(a);
					Point                p  = new Point(aD.get(1), aD.get(2));
					pointList.add(p);

				}
			}
		}
		return pointList;
	}

	public List<Integer> getConsolidatedParitoList(int rankCount)
	{
		int           countConsolidated = 0;
		List<Integer> pList             = new ArrayList<Integer>();

		for (Integer i : rankedParetoMap.keySet())
		{
			if (rankCount > 0 && countConsolidated == rankCount)
				break;
			List<Integer> r1ParetoList = rankedParetoMap.get(i);
			for (int j = 0; j < r1ParetoList.size(); j++)
			{
				try
				{
					if (pList.contains(Integer.valueOf(r1ParetoList.get(j))))
						throw new Exception("DUPLICATE entry in paretorRank " + i);
					else
						pList.add(r1ParetoList.get(j));
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			countConsolidated++;

		}
		return pList;
	}

	public List<BitString> getNextGeneration(List<BitString> combinedList)
	{

		List<Integer>   pList         = new ArrayList<Integer>();
		List<BitString> newGenPopList = new ArrayList<BitString>();
		int             totalCount    = 0;
		for (Integer i : rankedParetoMap.keySet())
		{
			List<Integer> rList = rankedParetoMap.get(i);
			totalCount = pList.size() + rList.size();
			if (totalCount <= sampleSize)
				pList.addAll(rList);
			else
			{

				// how many more to add to make the equal to sampleSize ? 

				int moreCount = this.sampleSize - pList.size();
				// do crowding distance

				List<CrowdingData> crowdingList = new ArrayList<CrowdingData>();
				for (int u : rList)
				{
					CrowdingData cd = new CrowdingData();
					cd.setId(u);
					cd.setBitString(combinedList.get(u).getBitString());
					crowdingList.add(cd);

				}
				crowdingList.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
				crowdingList.get(crowdingList.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);
				if (crowdingList.size() > 2)

				{
					for (int fnIndex : OBJ_FN_INDEX_LIST)
					{
						Collections.sort(crowdingList, getComparator(fnIndex));

						for (int ci = 1; ci < crowdingList.size() - 1; ci++)
						{
							double d = getCrowdingDist(crowdingList.get(ci - 1), crowdingList.get(ci + 1), fnIndex,
									getValueByStrBit(crowdingList.get(0).bitString, fnIndex),
									getValueByStrBit(crowdingList.get(crowdingList.size() - 1).bitString, fnIndex));
							crowdingList.get(ci).setCrowdingDistance(crowdingList.get(ci).getCrowdingDistance() + d);

						}
					}
				}
				Collections.sort(crowdingList, comparatorByCorwdingDistance);

				print("MoreCount=" + moreCount + " pList.size=" + pList.size() + " lastParetoSize=" + rList.size());

				for (int mc = 0; mc < moreCount; mc++)
				{

					pList.add(crowdingList.get(mc).getId());
				}
				print("Last pareto="+getStringOfList(rList, "Last ParetoFront"));
				print("Last pareto sorted by Crowding distance ");
				for(CrowdingData cd:crowdingList)
				{
					System.out.println(cd);
					
				}
				print("MoreCount=" + moreCount + " pList.size=" + pList.size() + " lastParetoSize=" + rList.size());
				print("Crowding Distance applied to choose=" + moreCount + " elements from "+i+"th  pareto front which has size of =    "+ rList.size()+" elements .");

			}

		}

		totalCount = pList.size();
		if (totalCount == sampleSize)
		{
			for (int f : pList)
			{
				newGenPopList.add(combinedList.get(f));

			}

			List<Integer> firstParetoFront = this.rankedParetoMap.get(0);
			print("firstParetoFront.size=" + firstParetoFront.size());
			print("programRunCount=" + programRunCount);
			printBitStringList(this.getBitStringListByIndex(combinedList, firstParetoFront), OBJ_FN_INDEX_LIST);

			return newGenPopList;

		}
		return newGenPopList;
	}

	Comparator<CrowdingData> comparatorByCorwdingDistance = new Comparator<CrowdingData>()
	{

		@Override
		public int compare(CrowdingData cd1, CrowdingData cd2)
		{
			int v = ((Double) (cd2.getCrowdingDistance() - cd1.getCrowdingDistance())).intValue();
			return v;
		}
	};

	int[] addToCrowdingList(int[] cdList, int value)
	{
		if (!arrayContains(cdList, value))
		{
			cdList[0] = value;
		}

		return cdList;
	}

	Comparator<CrowdingData> getComparator(int functionIndex)
	{
		Comparator<CrowdingData> comp = new Comparator<CrowdingData>()
		{

			@Override
			public int compare(CrowdingData cd1, CrowdingData cd2)
			{
				double d1;
				double d2;

				if (arrayContains(OBJ_FN_INDEX_LIST, functionIndex))
				{
					VarInfo              vf      = varInfoMap.get(functionIndex);
					Map<Integer, String> bitMap1 = splitStringBits(cd1.bitString, OBJ_FN_INDEX_LIST);
					d1 = getDecimalValByLimit(bitMap1.get(functionIndex), vf.getValMax(), vf.getPrecision());
					Map<Integer, String> bitMap2 = splitStringBits(cd2.bitString, OBJ_FN_INDEX_LIST);
					d2 = getDecimalValByLimit(bitMap2.get(functionIndex), vf.getValMax(), vf.getPrecision());
					int v = ((Double) d2).intValue() - ((Double) d1).intValue();

					return v;
				}
				return -1;
			}
		};
		return comp;
	}

	boolean arrayContains(int[] array, int value)
	{
		for (int i : array)
			if (i == value)
				return true;
		return false;
	}

	public List<Integer> getParetoFront(List<BitString> mutatedList)
	{
		List<Integer> paretoFront            = new ArrayList<Integer>();
		List<Integer> consolidatedParitoList = getConsolidatedParitoList(-1);
		/// add all indices of mutatedList 
		for (int i = 0; i < mutatedList.size(); i++)
		{
			if (!consolidatedParitoList.contains(Integer.valueOf(i)))
				paretoFront.add(i);
		}
		for (int i = 0; i < mutatedList.size(); i++)
		{
			// get  bitString a
			if (!consolidatedParitoList.contains(Integer.valueOf(i)))
			{
				String a = mutatedList.get(i).getBitString();

				Map<Integer, Double> aD = getNumericValues(a);
				for (int j = 0; j < mutatedList.size(); j++)
				{
					if (!consolidatedParitoList.contains(Integer.valueOf(j)))
					{

						// get  bitStrings b
						String b = mutatedList.get(j).getBitString();

						Map<Integer, Double> bD = getNumericValues(b);

						int[][] scoreBoard = getParitoScroeBoard(aD, bD);
						int     aScore     = getScore(0, scoreBoard);
						int     bScore     = getScore(1, scoreBoard);
						if (bScore < aScore)
						{
							pushToDominatedList(i, j);
							paretoFront = removeFromPareto(j, paretoFront);
						} else if (aScore < bScore)
						{
							pushToDominatedList(j, i);
							paretoFront = removeFromPareto(i, paretoFront);
						}
						// ignore if aScore=bScore . Its a duplicate entry
						StringBuilder sb = new StringBuilder();
						sb.append(i + "=i==================j=" + j + "\n");
						sb.append("f1  f2 \n");
						sb.append(aD.get(1) + " " + aD.get(2) + "\n");
						sb.append(bD.get(1) + " " + bD.get(2) + "\n");
						sb.append("=======score===========\n");
						sb.append("f1   f2 \n").append("A " + scoreBoard[0][0] + " " + scoreBoard[0][1] + "\n")
								.append("B " + scoreBoard[1][0] + " " + scoreBoard[1][1] + "\n");
								print(sb.toString());
								print(getStringOfList(paretoFront, "Pareto"));

					}
				}
			}
		}
		return paretoFront;
	}

	private List<Integer> removeFromPareto(int i, List<Integer> paretoFront)
	{
				print("REMOVE FROM PARETO=" + i);
		if (paretoFront.contains(i))
		{

			paretoFront.remove(Integer.valueOf(i));
		}
		return paretoFront;
	}

	private void pushToDominatedList(int i, int j)
	{

		if (dominatedMap.containsKey(i))
		{
			dominatedMap.get(i).add(j);
		} else
		{
			List<Integer> l = new ArrayList<Integer>();
			l.add(j);
			dominatedMap.put(i, l);
		}

	}

	private int getScore(int i, int[][] scoreBoard)
	{
		int score = 0;
		for (int s : OBJ_FN_INDEX_LIST)
			score += scoreBoard[i][s - 1];

		return score;
	}

	private int[][] getParitoScroeBoard(Map<Integer, Double> aD, Map<Integer, Double> bD)
	{
		int[][] scoreBoard = new int[2][OBJ_FN_INDEX_LIST.length];
		for (int f : OBJ_FN_INDEX_LIST)
		{
			if (varInfoMap.get(f).isMaximize)
			{
				if (aD.get(f) == bD.get(f))
				{
					scoreBoard[0][f - 1] = 1;
					scoreBoard[1][f - 1] = 1;
				} else if (aD.get(f) > bD.get(f))
				{
					scoreBoard[0][f - 1] = 1;
					scoreBoard[1][f - 1] = 0;

				} else if (bD.get(f) > aD.get(f))
				{
					scoreBoard[0][f - 1] = 0;
					scoreBoard[1][f - 1] = 1;
				}
			} else
			{
				if (aD.get(f) == bD.get(f))
				{
					scoreBoard[0][f - 1] = 1;
					scoreBoard[1][f - 1] = 1;
				} else if (aD.get(f) < bD.get(f))
				{
					scoreBoard[0][f - 1] = 1;
					scoreBoard[1][f - 1] = 0;

				} else if (bD.get(f) < aD.get(f))
				{
					scoreBoard[0][f - 1] = 0;
					scoreBoard[1][f - 1] = 1;
				}
			}
		}
		return scoreBoard;
	}

	public Map<Integer, Double> getBitValues(Map<Integer, String> bitsMap)
	{
		Map<Integer, Double> resultMap = new HashMap<Integer, Double>();
		for (int k : bitsMap.keySet())
		{
			VarInfo vf = varInfoMap.get(k);
			double  d  = getDecimalValByLimit(bitsMap.get(k), vf.getValMax(), vf.getPrecision());
			resultMap.put(k, d);
		}

		return resultMap;
	}

	public void showMutationDetails(String combinedBits,String mutationMask, Map<Integer, String> mm1, String mutated1, String mutated2)
	{
		print("CombinedBits                      MutaionMask        MutationMask ");
		print(combinedBits+" "+mutationMask + " " + mutationMask);
		print("Original Bits                    "+mm1.get(1) +       " " + mm1.get(2));
		print("Mutated Bits                     "+mutated1 + " " + mutated2);
		print("====================================chunks====================================");

	}

	public void testRandomGenerationOfPopulationOld()
	{
		varInfoMap.put(1, new VarInfo(1, false, 2, "r", 1.0, 500.0));
		varInfoMap.put(2, new VarInfo(2, false, 2, "h", 1.0, 500.0));

		int                  sampleSize = 8;
		ArrayList<BitString> sampleR    = getRandomSample(sampleSize, varInfoMap.get(1));

		ArrayList<BitString> sampleRh = getRandomSample(sampleSize, varInfoMap.get(2), sampleR);

		Collections.sort(sampleR, getSortComparator(SORT_ORDER.ASC));
		Collections.sort(sampleRh, getSortComparator(SORT_ORDER.ASC));
		//				printBitStringList(sampleRh, OBJ_FN_INDEX_LIST);
		List<Integer>   indexList           = getShuffledIndexList(sampleSize);
		List<BitString> crossedList         = new ArrayList<BitString>();
		double          crossingProbability = 0.5;
		for (int i = 0; i < sampleSize; i = i + 2)
		{
			int                  i1 = indexList.get(i);
			int                  i2 = indexList.get(i + 1);
			Map<Integer, String> m  = getCrossed(sampleRh.get(i1).getBitString(), sampleRh.get(i2).getBitString(),
					crossingProbability);
			crossedList.add(new BitString(i1, m.get(1)));
			crossedList.add(new BitString(i2, m.get(2)));
		}
				print("========================================================================");
				printBitStringList(crossedList, OBJ_FN_INDEX_LIST);
	}

	public double getCrowdingDist(CrowdingData cd1, CrowdingData cd2, int fnIndex, double fnMax, double fnMin)
	{

		Map<Integer, String> bm1 = splitStringBits(cd1.bitString, OBJ_FN_INDEX_LIST);
		Map<Integer, String> bm2 = splitStringBits(cd2.bitString, OBJ_FN_INDEX_LIST);
		if (arrayContains(OBJ_FN_INDEX_LIST, fnIndex))
		{
			VarInfo vf = varInfoMap.get(fnIndex);

			double d1 = getDecimalValByLimit(bm1.get(fnIndex), vf.getValMax(), vf.getPrecision());
			double d2 = getDecimalValByLimit(bm2.get(fnIndex), vf.getValMax(), vf.getPrecision());
			// (fnX+1-fnX-1)/(fnMax-fnMin)
			double        cdV           = (d2 - d1) / (fnMax - fnMin);
			DecimalFormat decimalFormat = new DecimalFormat("0.00");
			cdV = Double.valueOf(decimalFormat.format(cdV));
			return cdV;
		}

		return -1;
	}

	public double getValueByStrBit(String strBit, int fnIndex)
	{
		Map<Integer, String> bitMap        = splitStringBits(strBit, OBJ_FN_INDEX_LIST);
		VarInfo              vf            = varInfoMap.get(fnIndex);
		DecimalFormat        decimalFormat = new DecimalFormat("0.00");

		double val = getDecimalValByLimit(bitMap.get(fnIndex), vf.getValMax(), vf.getPrecision());
		val = Double.valueOf(decimalFormat.format(val));
		return val;
	}

	public void printStringBits(CrowdingData cd, int[] ind)
	{
		Map<Integer, String> bitMap = splitStringBits(cd.bitString, ind);
		String               s      = cd.getId() + " " + cd.getBitString();
		for (int i : ind)
		{
			VarInfo vf = varInfoMap.get(i);
			s = s + bitMap.get(i) + " " + decodeBinary(bitMap.get(i), vf.getPrecision()) + " "
					+ getDecimalValByLimit(bitMap.get(i), varInfoMap.get(i).getValMax(), vf.getPrecision()) + " "
					+ cd.getCrowdingDistance();
			//
			//					bitMap.get(2) + " " + decodeBinary(bitMap.get(2), 2) + " "
			//					+ getDecimalValByLimit(bitMap.get(2), varInfoMap.get(2).getValMax(), 2);
		}
		print(s);

	}

	public void printBitStringList(List<BitString> sampleRh, int[] ind)
	{

		for (BitString bs : sampleRh)
		{

			//			String               bs     = sampleRh.get(0).getBitString();
			Map<Integer, String> bitMap = splitStringBits(bs.getBitString(), ind);
			String               s      = bs.getBitString() + " ";
			for (int i : ind)
			{
				VarInfo vf = varInfoMap.get(i);
				s = s + bitMap.get(i) + " " + decodeBinary(bitMap.get(i), vf.getPrecision()) + " "
						+ getDecimalValByLimit(bitMap.get(i), vf.getValMax(), vf.getPrecision()) + " ";
				//
				//					bitMap.get(2) + " " + decodeBinary(bitMap.get(2), 2) + " "
				//					+ getDecimalValByLimit(bitMap.get(2), varInfoMap.get(2).getValMax(), 2);
			}
			print(s);

		}

	}

	public void printMutatedList(List<BitString> crossedList, List<BitString> sampleRh, int[] ind)
	{
		int k = 0;
		for (BitString bs : sampleRh)
		{
			  
			BitString crs = crossedList.get(k);
			k++;
			//			String               bs     = sampleRh.get(0).getBitString();
			Map<Integer, String> bitMap = splitStringBits(bs.getBitString(), ind);
			String               s      = crs.getBitString() + " ";
			for (int i : ind)
			{
				VarInfo vf = varInfoMap.get(i);
				s = s + bitMap.get(i) + " " + decodeBinary(bitMap.get(i), vf.getPrecision()) + " "
						+ getDecimalValByLimit(bitMap.get(i), vf.getValMax(), vf.getPrecision()) + " ";
				//
				//					bitMap.get(2) + " " + decodeBinary(bitMap.get(2), 2) + " "
				//					+ getDecimalValByLimit(bitMap.get(2), varInfoMap.get(2).getValMax(), 2);
			}
			print(s);

		}

	}

	public List<Integer> getShuffledIndexList(int maxIndex)
	{
		List<Integer> indexList = new ArrayList<Integer>();
		for (int i = 0; i < maxIndex; i++)
		{
			indexList.add(i);
		}
		Collections.shuffle(indexList);

		// if the number of bits is odd , apped the first bit at the last , so that las and first bitStrings can be crossed
		if (maxIndex % 2 == 1)
		{
			int i = indexList.get(0);
			indexList.add(i);
		}

		return indexList;
	}

	public Map<Integer, String> getCrossed(String strBit1, String strBit2, double probability)
	{
		int len  = strBit1.length();
		int pos  = ((Double) (len * probability)).intValue();
		int init = 0;

		String               s1      = strBit1.substring(init, pos);
		String               s2      = strBit1.substring(pos);
		String               t1      = strBit2.substring(init, pos);
		String               t2      = strBit2.substring(pos);
		String               strBitA = s1 + t2;
		String               strBitB = t1 + s2;
		Map<Integer, String> m       = new HashMap<Integer, String>();
		m.put(1, strBitA);
		m.put(2, strBitB);
				print("==============Crossover Input ==========================");
				print(strBit1 + " " + s1 + " " + s2);
				print(strBit2 + " " + t1 + " " + t2);
				print("===================Crossover Output=====================");
				print(strBitA);
				print(strBitB);

		return m;
	}

	public String getBitsAndInt(String bitStr, int precision)
	{
		return bitStr + " " + getDecimalValByLimit(bitStr, 500, precision);
	}

	// Input= a string of combined bits of 2 or more variables , Output=  a map of string bits 
	public Map<Integer, String> splitStringBits(String strBit, int[] indexList)
	{

		// TO avoid index out of bounds exception , test if summation of all bitlengths by indexList are equal to strBit.length()
		int len = 0;
		for (int i : indexList)
			len += varInfoMap.get(i).getBitLength();
		if (len == strBit.length())
		{
			Map<Integer, String> sp     = new HashMap<Integer, String>();
			int                  init   = 0;
			int                  bitLen = 0;
			for (int i : indexList)
			{
				bitLen += varInfoMap.get(i).getBitLength();
				// get the first bitLen bits
				String s = strBit.substring(init, bitLen);
				sp.put(i, s);
				init += bitLen;
			}
			return sp;
		}
		return null;
	}

	public void testBinaryEncoding()
	{
		int    precision = 2;
		String bin       = getBinary(500.00, precision);
		print("len=" + bin.length());
		print(bin);
		print(decodeBinary(bin, precision) + "");
		print(getDecimalValByLimit(bin, 500, precision) + "");

		print("full value");
		String binaryVal = "1100001101010000";
		binaryVal = "1111111111111111";
		print("len=" + binaryVal.length());

		double dVal = decodeBinary(binaryVal, precision);
		print(dVal + "");

		print(getDecimalValByLimit(binaryVal, 500, precision) + "");

	}

	public double getMaxDaubleByBitLen(int len)
	{
		double m = 0.0;
		// get decimal value of a binary string with  all 1's ( 111111) 
		for (int i = 0; i < len; i++)
		{
			m += Math.pow(2, i);
		}
		return m;
	}

	public double getDecimalValByLimit(String binaryStr, double maxVal, int precision)
	{

		int    len = binaryStr.length();
		double m   = 0.0;
		m = getMaxDaubleByBitLen(len);
		double p = 1;
		if (precision > 0)
			p = Math.pow(10, precision);
		m = m / p;
		double deciVal = decodeBinary(binaryStr, precision);
		double dVal    = deciVal * maxVal / m;
		;
		String formatStr="0.";
		for(int i =0;i<precision;i++)
			formatStr+="0";
		return Double.valueOf(new DecimalFormat(formatStr).format(deciVal * maxVal / m));
		//	return deciVal*maxVal/m;

	}

	//	create a method to convert a double value ot binary 
	public String getBinary(double inputDouble, int precision)
	{
		//		print(" input =" + inputDouble);

		int p = 1;

		if (precision > 0)
		{
			p = ((Double) Math.pow(10, precision)).intValue();
		}
		Double d     = inputDouble * p;
		int    input = d.intValue();

		StringBuilder sb = new StringBuilder();

		while (input > 0)
		{
			int v = input % 2;
			sb.append(v);
			input = input / 2;
		}

		return reverse(sb.toString());
	}

	public ArrayList<BitString> getRandomSample(int sampleSize, VarInfo varInfo)
	{

		ArrayList<BitString> bitStrList = new ArrayList<>();
		for (int i = 0; i < sampleSize; i++)
		{
			double inputDouble = Math.random() * (varInfo.getValMax() - varInfo.getValMin()) + varInfo.getValMin();
			inputDouble = inputDouble * varInfo.getMaxDouble() / varInfo.getValMax();

			inputDouble = Double.valueOf(new DecimalFormat("0.00").format(inputDouble));
			String bitStr = getBinary(inputDouble, varInfo.getPrecision());
			// if the new bitStr is shorter than max bitlength then append 0's to make it same as maxBitlen
			int lessLen = varInfo.getBitLength() - bitStr.length();
			for (int j = 0; j < lessLen; j++)
				bitStr += '0';

			bitStrList.add(new BitString(i, bitStr));
		}
		return bitStrList;

	}

	public ArrayList<BitString> getRandomSample(int sampleSize, VarInfo varInfo, ArrayList<BitString> bitStrList)
	{
		Collections.sort(bitStrList, getSortComparator(SORT_ORDER.ASC));

		ArrayList<BitString> newBitStrList = new ArrayList<BitString>();
		for (int i = 0; i < sampleSize; i++)
		{
			double inputDouble = Math.random() * (varInfo.getValMax() - varInfo.getValMin()) + varInfo.getValMin();
			inputDouble = inputDouble * varInfo.getMaxDouble() / varInfo.getValMax();
			inputDouble = Double.valueOf(new DecimalFormat("0.00").format(inputDouble));
			//			print("max="+varInfo.getValMax()+" min="+varInfo.getValMin()+" inputDouble="+inputDouble);
			String bitStr = getBinary(inputDouble, varInfo.getPrecision());
			// if the new bitStr is shorter than max bitlength then append 0's to make it same as maxBitlen
			int lessLen = varInfo.getBitLength() - bitStr.length();
			for (int j = 0; j < lessLen; j++)
				bitStr += '0';

			String oldBitStr = bitStrList.get(i).getBitString();
			oldBitStr += bitStr;
			newBitStrList.add(i, new BitString(i, oldBitStr));
		}
		return newBitStrList;

	}

	ArrayList<BitString> getRandomSample(int sampleSize, int precison, double minVal, double maxVal)
	{
		ArrayList<BitString> bitStrList = new ArrayList<>();
		for (int i = 0; i < sampleSize; i++)
		{
			double inputDouble = Math.random() * (maxVal - minVal) + minVal;
			String bitStr      = getBinary(inputDouble, precison);
			bitStrList.add(new BitString(i, bitStr));
		}
		return bitStrList;
	}

	enum SORT_ORDER {
		ASC, DESC
	}

	public Comparator<BitString> getSortComparator(SORT_ORDER sortOrder)
	{
		Comparator<BitString> cmp = new Comparator<BitString>()
		{

			@Override
			public int compare(BitString o1, BitString o2)
			{
				if (sortOrder.equals(SORT_ORDER.ASC))
					return o1.getIndex() - o2.getIndex();
				else
					return o2.getIndex() - o1.getIndex();

			}

		};
		return cmp;
	}

	public int[][] getRandomSample(int sampleSize, double minVal, double maxVal, int precision)
	{
		String  b           = getBinary(maxVal, precision);
		int     bitLen      = b.length();
		int[][] sampleArray = new int[sampleSize][bitLen];
		for (int i = 0; i < sampleSize; i++)
		{
			double randVal  = Math.random() * (maxVal - minVal) + minVal;
			String bitStr   = getBinary(randVal, precision);
			int[]  bitArray = getBitArray(bitStr);
			sampleArray[i] = bitArray;
		}
		return sampleArray;
	}

	public int[] getBitArray(String bitStr)
	{
		int[] bitArray = new int[bitStr.length()];
		for (int i = 0; i < bitStr.length(); i++)
		{
			char c    = bitStr.charAt(i);
			int  cVal = 0;
			if (c == '1')
				cVal = 1;
			else if (c == '0')
				cVal = 0;
			bitArray[i] = cVal;

		}
		return bitArray;
	}

	public double decodeBinary(String str, int precision)
	{
		double val = 0;
		int    pos = 0;
		for (int i = str.length() - 1; i >= 0; i--)
		{
			char c = str.charAt(i);
			int  m = 0;
			if (c == '1')
				m = 1;
			else
				m = 0;
			val += m * Math.pow(2, pos);
			pos++;
		}
		int decimalPlaces = 1;
		if (decimalPlaces > 0)
			decimalPlaces = ((Double) Math.pow(10, precision)).intValue();
		val = val / decimalPlaces;
		return val;
	}

	public String reverse(String str)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = str.length() - 1; i >= 0; i--)
		{
			char c = str.charAt(i);
			sb.append(c);
		}
		return sb.toString();
	}

	public void print(String s)
	{
		if (this.isDebug)
		{
			System.out.println(s);
		}
	}

}

@Getter
@Setter
class VarInfo
{
	// Position of this variable in combined binary string with other variables ; 
	int     sequence;
	String  varName;
	boolean isMaximize;
	int     bitLength;
	double  maxDouble;
	double  valMin;
	double  valMax;
	int     precision;
	double  crossoverProbability;
	double  mutationProbability;

	public VarInfo(int sequence, boolean isMaximize, int precision, String varName, double valMin, double valMax)
	{
		this.sequence = sequence;
		this.varName  = varName;
		this.valMin   = valMin;
		this.valMax   = valMax;
		String b = new Nsga2Impl().getBinary(valMax, precision);
		this.bitLength  = b.length();
		this.maxDouble  = new Nsga2Impl().getMaxDaubleByBitLen(bitLength);
		this.isMaximize = isMaximize;
	}
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class BitString
{
	int    index;
	String bitString;

	@Override
	public String toString()
	{
		return "[" + this.index + "] " + this.bitString;
	}

	public String printVal()
	{
		return "[" + this.index + "] " + this.bitString + " " + new Nsga2Impl().decodeBinary(bitString, 2);
	}
}
