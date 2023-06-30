package muliti.objective.opt.submit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;

public class CelebretySolution
{
	int   SELECTION_COUNT = 20;
	int   ACTRESS_COUNT   = 8;
	int NUM_ITERATION=0;
	
	int[] sortedIndex;

	enum VAR_TYPES {
		SALARY(0, false),
		PUBLICITY(1), 
//		WEIGHT(2, false, false),
		WEIGHT(2, false),
		ACTRESS_COUNT(3, true, false)
//		ACTRESS_COUNT(3, true)
		;

private		int     index      = 0;
private		boolean isPositive = true;
private		boolean isActive   = true;

		VAR_TYPES(int index)
		{
			this.index = index;
		}

		VAR_TYPES(int index, boolean isPositive)
		{
			this.index      = index;
			this.isPositive = isPositive;
		}

		VAR_TYPES(int index, boolean isPositive, boolean b)
		{
			this.index      = index;
			this.isPositive = isPositive;

			this.isActive = b;
		}

		public int getIndex()
		{
			return index;
		}

		public boolean getIspositive()
		{
			return this.isPositive;
		}

		public boolean getIsActive()
		{
			return this.isActive;
		}
	}

//	@formatter:off
	public static void main(String[] args)
	{
		int                                 i                     = 0;
		CelebretySolution                   cb                    = new CelebretySolution();
		Map<Integer, List<SelectionResult>> selectionResulListMap = new HashMap<>();
		int[][]  selectionList         = cb.getRandomSelctions(cb.SELECTION_COUNT,cb.ACTRESS_COUNT);
		selectionResulListMap = cb.calculateSolutionResult(selectionList, selectionResulListMap, 0);

		int[][] crossedList = new int[cb.SELECTION_COUNT][cb.ACTRESS_COUNT];
		do
		{
			List<Integer> shuffledIndexList = cb.groupAndShuffle(selectionResulListMap.get(i));
			
			if (i == 0)
				crossedList = cb.getCrossedList(shuffledIndexList, selectionList, selectionResulListMap.get(i));
			else 
				crossedList = cb.getCrossedList(shuffledIndexList, crossedList, selectionResulListMap.get(i));
			i++;
			selectionResulListMap = cb.calculateSolutionResult(crossedList, selectionResulListMap, i);
		    shuffledIndexList = cb.groupAndShuffle(selectionResulListMap.get(i));

			i++;

			crossedList=cb.getMutatedList2D(crossedList);
			selectionResulListMap = cb.calculateSolutionResult(crossedList, selectionResulListMap, i);
			shuffledIndexList = cb.groupAndShuffle(selectionResulListMap.get(i));

 
		}
		while (i <cb. NUM_ITERATION);
		cb.printSelectionResultListMapTabular(selectionResulListMap);
	}

public int [][] getMutatedList2D(int [][] crossedList)
{
	int [][]mutatedList=new int[SELECTION_COUNT][ACTRESS_COUNT];
	for(int i=0;i<SELECTION_COUNT;i++)
		{
			int [] cl =crossedList[i];
			int [] ml =getMutatedList(cl, 0.8);
			mutatedList[i]=ml;
			
		}
	return mutatedList;
}
//@formatter:on
	public int[] getMutatedList(int[] selection, double limit)
	{
		int[] mutationOperand = getRandomActressList(ACTRESS_COUNT, limit);
		int   sum             = getBitSum(mutationOperand);
		while (sum < 1)
		{
			mutationOperand = getRandomActressList(ACTRESS_COUNT, limit);
			sum             = getBitSum(mutationOperand);
		}
		System.out.println(getStringOfArray(selection, "In=>Selecetion"));
		System.out.println(getStringOfArray(mutationOperand, "Mask=>Operand"));

		int[] result = new int[ACTRESS_COUNT];
		int   i      = 0;
		for (int val : mutationOperand)
		{
			if (val == 1)
			{
				result[i] = selection[i] == 1 ? 0 : 1;
			} else
				result[i] = selection[i];
			i++;
		}
		int bitSum = getBitSum(result);

		while (bitSum < 1)
		{
			result = getMutatedList(selection, limit);
			bitSum = getBitSum(result);
		}
		System.out.println(getStringOfArray(result, "Out=>mutated"));

		return result;
	}

	public int getBitSum(int[] selection)
	{
		int s = 0;
		for (int i : selection)
			s += i;
		return s;
	}

	public int[][] getRandomSelctions(int index1, int index2)
	{
		int[][] selectionList = new int[index1][index2];
		for (int i = 0; i < SELECTION_COUNT; i++)
		{
			selectionList[i] = getRandomActressList(ACTRESS_COUNT);
			int bitSum=this.getBitSum(selectionList[i]);
			while(bitSum<1)
			{
				selectionList[i]=getRandomActressList(ACTRESS_COUNT);
			}
		}
		return selectionList;

	}

	public Map<Integer, List<SelectionResult>> calculateSolutionResult(int[][] selectionList,
			Map<Integer, List<SelectionResult>> selectionResulListMap, int iterationCount)
	{

		List<SelectionResult> selectionResultList = new ArrayList<>();
		for (int i = 0; i < SELECTION_COUNT; i++)
		{
			SelectionResult selectionResult = new SelectionResult(i).addSelectionRow(selectionList[i]);

			//@formatter:off

		for (VAR_TYPES objective : VAR_TYPES.values())
		{
							if(objective.isActive)
			{
				ObjectiveResult objectiveResult = new ObjectiveResult()
						.addObjectiveFunction(objective)
						.addValue(
								getValueByAcrtessList(selectionList[i], objective));
				selectionResult.addObjective(objectiveResult);
			}
		}
//		@formatter:on
			selectionResultList.add(selectionResult);
			selectionResulListMap.put(iterationCount, selectionResultList);
			System.out.println();
			System.out.println(selectionResult);

		}
		return selectionResulListMap;

	}

	public List<Integer> groupAndShuffle(List<SelectionResult> selectionResultList)
	{
		Map<String, List<Integer>> groupIndexMap = getGroupList(selectionResultList);
		this.printGroupIndexMap(groupIndexMap);

		List<Integer> shuffledIndexList = new ArrayList<Integer>(SELECTION_COUNT);
		for (int i = 0; i < SELECTION_COUNT; i++)
			shuffledIndexList.add(i);
		Collections.shuffle(shuffledIndexList);
		System.out.println(getStringOfArray(getIntArray(shuffledIndexList), "ShuffledIndex "));

		return shuffledIndexList;
	}

	//	getCrossedPopulatoin(int )

	public int[][] getCrossedList(List<Integer> shuffledIndexList, int[][] selectionList,
			List<SelectionResult> selectionResultList)
	{

		/// get evaluation

		int[][] crossedList = new int[SELECTION_COUNT][ACTRESS_COUNT];
		for (int g = 0; g < SELECTION_COUNT; g = g + 2)
		{
			crossedList = getCrossed(shuffledIndexList, g, (g + 1), selectionList, crossedList);
		}
		return crossedList;
	}

	int[] getIntArray(List<Integer> intList)
	{
		int[] intValues = new int[intList.size()];
		for (int i = 0; i < intList.size(); i++)
		{
			intValues[i] = intList.get(i);

		}
		return intValues;
	}

	public int[][] getCrossed(List<Integer> shuffledIndexList, int index1, int index2, int[][] selectionList,
			int[][] resultList)
	{
		int shuffled1 = shuffledIndexList.get(index1);
		int shuffled2 = shuffledIndexList.get(index2);
		System.out.println(this.getStringOfArray(selectionList[shuffled1], "Selectionlist" + shuffled1 + "="));
		System.out.println(this.getStringOfArray(selectionList[shuffled2], "Selectionlist" + shuffled2 + "="));
		int[] result1 = new int[ACTRESS_COUNT];
		int[] result2 = new int[ACTRESS_COUNT];
		int   i       = 0;
		for (; i < ACTRESS_COUNT; i++)
		{
			if (i < ACTRESS_COUNT / 2)
			{
				result1[i] = selectionList[shuffled1][i];
				result2[i] = selectionList[shuffled2][i];
			} else
				break;
		}

		for (; i < ACTRESS_COUNT; i++)
		{
			result1[i] = selectionList[shuffled2][i];
			result2[i] = selectionList[shuffled1][i];

		}
		resultList[index1] = result1;
		resultList[index2] = result2;
		System.out.println(this.getStringOfArray(result1, "crossed1="));
		System.out.println(this.getStringOfArray(result2, "crossed2=") + "\n");

		return resultList;
	}

	public int getRandomNumber(int low, int high)
	{
		Random r = new Random();
		int    t = r.nextInt(high - low) + low;
		return t;
	}

	public Map<String, List<Integer>> getGroupList(List<SelectionResult> selectionResultList)
	{
		Map<String, List<Integer>> groupIndexMap = new HashMap<String, List<Integer>>();

		for (VAR_TYPES v : VAR_TYPES.values())
		{
			if (v.getIsActive())
			{
				sortedIndex = getSortedByObjective(selectionResultList, v);
				System.out.println(getStringOfArray(sortedIndex, v.toString(), "sortedBy" + v.toString()));
				int objectiveFunctionCount = getObjectiveCount();
				int numberOfGroups         = objectiveFunctionCount;
				int pointer                = 0;
				for (int nb = 0; nb < numberOfGroups; nb++)
				{
					List<Integer> groupAList = new ArrayList<>();

					for (int c = 0; c < objectiveFunctionCount; c++)
					{
						groupAList.add(sortedIndex[pointer]);
						pointer++;
					}
					groupIndexMap.put(v.toString() + "-" + nb, groupAList);
				}
				if (SELECTION_COUNT % objectiveFunctionCount > 0)
				{
					// its the last group , add remaining bits to it 
					List<Integer> groupAList = new ArrayList<Integer>();
					for (int q = 0; q < SELECTION_COUNT % objectiveFunctionCount; q++)
					{
						groupAList.add(sortedIndex[pointer]);
						pointer++;
					}
					groupIndexMap.put(v.toString() + "-ext", groupAList);
				}

			}
		}
		return groupIndexMap;
	}

	public void printGroupIndexMap(Map<String, List<Integer>> groupIndexMap)
	{
		List<String> keyList = new ArrayList<>();
		keyList.addAll(groupIndexMap.keySet());

		Collections.sort(keyList);

		StringBuilder sb = new StringBuilder();
		for (String k : keyList)
		{
			sb.append("\n" + k + " [");
			for (int v : groupIndexMap.get(k))
			{
				sb.append(v).append(",");

			}
			sb.deleteCharAt(sb.lastIndexOf(","));
			sb.append(" ]\n");
		}
		System.out.println(sb.toString());
	}

	//TODO: add suffle to selectionResultMap
	public int[] getSortedByObjective(List<SelectionResult> selectionResultList, VAR_TYPES varType)
	{
		int highestSalarySelections[] = new int[selectionResultList.size()];
		highestSalarySelections = initializeArray(highestSalarySelections, -1);

		for (int ia = 0; ia < selectionResultList.size(); ia++)
		{
			highestSalarySelections[ia] = getSortedIndex(highestSalarySelections, selectionResultList, varType);
		}
		return highestSalarySelections;
	}

	int[] initializeArray(int[] array, int val)
	{
		for (int i = 0; i < array.length; i++)
			array[i] = val;
		return array;
	}

	int getSortedIndex(int[] highestSalarySelections, List<SelectionResult> selectionResultList, VAR_TYPES varType)
	{

		//	for( int i=0;i<1;i++)
		//{
		int     index        = -1;
		int     highestValue = -1;
		boolean isModified   = false;
		for (SelectionResult sr : selectionResultList)
		{

			if (arrayContains(highestSalarySelections, sr.getSelectionIndex()))
				continue;
			else
				for (ObjectiveResult or : sr.getObjectiveResultList())
				{

					if (or.getOjbectiveFuntion() == varType)
					{

						if (varType.isPositive)
						{
							if (or.getValue() >= highestValue)
							{
								highestValue = or.getValue();
								index        = sr.getSelectionIndex();
								isModified   = true;
							}
						} else if (or.getValue() <= highestValue || highestValue == -1)
						{
							highestValue = or.getValue();
							index        = sr.getSelectionIndex();
							isModified   = true;
						}

					}
				}
		}
		//System.out.println("----- ind'ex" + index + varType.toString() + "=" + highestValue);
		//		if (isModified)
		//highestSalarySelections[0]=index;
		return index;
		//		else
		//			return -1;
	}

	boolean arrayContains(int[] array, int value)
	{
		for (int i : array)
			if (i == value)
				return true;
		return false;
	}

	public Comparator<ObjectiveResult> getComparator(VAR_TYPES varType)
	{
		Comparator<ObjectiveResult> comparator = new Comparator<ObjectiveResult>()
		{

			@Override
			public int compare(ObjectiveResult ob, ObjectiveResult ob2)
			{
				if (varType == ob.getOjbectiveFuntion() && varType == ob2.getOjbectiveFuntion())
				{

					if (varType.isPositive)
						return ob.getValue() - ob2.getValue();
					else
						return ob2.getValue() - ob.getValue();
				} else
					return -1234567890;
			}
		};

		return comparator;

	}

	public int getObjectiveCount()
	{
		int n = 0;
		for (VAR_TYPES v : VAR_TYPES.values())
			if (v.isActive)
				n++;
		return n;
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

	public void printSelectionResultListMap(Map<Integer, List<SelectionResult>> selectionResulListMap)
	{
		for (int key : selectionResulListMap.keySet())
		{
			System.out.println();
			System.out.println("Itereration #" + key);
			for (SelectionResult selectionResult : selectionResulListMap.get(key))
			{
				System.out.println(selectionResult);

			}

		}
	}

	public void printSelectionResultListMapTabular(Map<Integer, List<SelectionResult>> selectionResulListMap)
	{
		String        tab = "\t";
		StringBuilder sb  = new StringBuilder();
		sb.append("\n");
		sb.append("\nRUN");
		if (selectionResulListMap != null && selectionResulListMap.containsKey(0))
			for (SelectionResult selectionResult : selectionResulListMap.get(0))
				{sb.append(tab).append("   SELECTION");
				if(isActive(VAR_TYPES.SALARY, selectionResult))
				sb.append(tab + "         ").append("SAL");
				if(isActive(VAR_TYPES.PUBLICITY, selectionResult))
				sb.append(tab).append("PUB");
				if(isActive(VAR_TYPES.WEIGHT, selectionResult))
				sb.append(tab).append("WT");
				if(isActive(VAR_TYPES.ACTRESS_COUNT, selectionResult))
				sb.append(tab).append("CNT");
				}
		sb.append("\n");

		for (int key : selectionResulListMap.keySet())
		{
			sb.append("\n#" + key + tab);
			for (SelectionResult selectionResult : selectionResulListMap.get(key))
			{
				sb.append(getStringOfArray(selectionResult.getSelectionRow()));
				sb.append(tab);
				List<ObjectiveResult> objectiveResultList = selectionResult.getObjectiveResultList();
				Collections.sort(objectiveResultList, new Comparator<ObjectiveResult>()
				{

					@Override
					public int compare(ObjectiveResult arg1, ObjectiveResult arg0)
					{
						// TODO Auto-generated method stub
						return arg1.getOjbectiveFuntion().getIndex() - arg0.getOjbectiveFuntion().getIndex();
					}
				});
				for (ObjectiveResult oj : objectiveResultList)
				{
					sb.append(oj.getValue()).append(tab);
				}
			}

		}
		System.out.println(sb.toString());
	}
public boolean isActive(VAR_TYPES varType, SelectionResult selectionResult)
{
	for(ObjectiveResult oj: selectionResult.getObjectiveResultList())
	{
		if(oj.getOjbectiveFuntion()==varType&& oj.getOjbectiveFuntion().isActive)
		{
			return true;
		}
		if(oj.getOjbectiveFuntion()==varType&& oj.getOjbectiveFuntion().isActive)
		{
			
			return true;
		}
		if(oj.getOjbectiveFuntion()==varType&& oj.getOjbectiveFuntion().isActive)
		{
			
			return true;
		}
		if(oj.getOjbectiveFuntion()==varType&& oj.getOjbectiveFuntion().isActive)
		{
			
			return true;
		}
	}
			
return false; 
}
	@Getter
	@Setter
	class SelectionResult
	{
		private List<ObjectiveResult> objectiveResultList;
		private int                   selectionIndex = -1;
		int[]                         selectionRow;
		int[]                         shuffledIndex;

		SelectionResult(int selectionIndex)
		{
			this.selectionIndex = selectionIndex;
		}

		public SelectionResult addObjective(ObjectiveResult objectiveResult)
		{
			if (objectiveResultList == null)
				objectiveResultList = new ArrayList<ObjectiveResult>();
			this.objectiveResultList.add(objectiveResult);
			return this;
		}

		public SelectionResult addSelectionIndex(int index)
		{
			this.selectionIndex = index;
			return this;
		}

		public SelectionResult addSelectionRow(int[] selectionRow)
		{
			this.selectionRow = selectionRow;
			return this;
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append(getStringOfArray(this.getSelectionRow(), "selection ")).append("\n");
			sb.append(getStringOfArray(this.getShuffledIndex(), " shuffled ")).append("\n");
			sb.append("selectionIndex : ").append(this.selectionIndex);
			sb.append(" ObjectiveResults{");
			for (ObjectiveResult or : objectiveResultList)
				sb.append(" obective function: " + or.getOjbectiveFuntion().toString())
						.append(" value : " + or.getValue());
			sb.append("}");

			return sb.toString();
		}
	}

	@Getter
	@Setter
	class ObjectiveResult
	{
		private VAR_TYPES ojbectiveFuntion;
		private int       value;

		public ObjectiveResult addObjectiveFunction(VAR_TYPES objectiveFunction)
		{
			this.ojbectiveFuntion = objectiveFunction;
			return this;
		}

		public ObjectiveResult addValue(int value)
		{
			this.value = value;
			return this;
		}

	}

	public int[] getRandomActressList(int total, double... limit)
	{
		int[] s = new int[total];
		for (int i = 0; i < total; i++)
		{
			if (limit != null && limit.length > 0)
			{
				s[i] = Math.random() > limit[0] ? 1 : 0;

			} else
				s[i] = Math.random() > 0.5 ? 1 : 0;
		}

		return s;
	}

	public int getValueByAcrtessList(int[] actressList, VAR_TYPES varType)
	{
		int var = 0;
		for (int actressIndex = 0; actressIndex < actressList.length; actressIndex++)
		{
			switch (varType)
			{
			case PUBLICITY:
				var += actressList[actressIndex] * this.inputData[actressIndex][VAR_TYPES.PUBLICITY.getIndex()];
				break;

			case SALARY:
				var += actressList[actressIndex] * this.inputData[actressIndex][VAR_TYPES.SALARY.getIndex()];
				break;

			case WEIGHT:
				var += actressList[actressIndex] * this.inputData[actressIndex][VAR_TYPES.WEIGHT.getIndex()];
				break;
			case ACTRESS_COUNT:
				var += actressList[actressIndex];
				break;
			default:
				var = -1;
				break;

			}
		}

		return var;
	}

	int inputData[][] =
	{
			//  		{Salary, Publicity , Weight}
			{ 100, 10, 50 },
			{ 200, 20, 70 },
			{ 50, 5, 80 },
			{ 600, 60, 55 },
			{ 400, 40, 67 },
			{ 300, 30, 59 },
			{ 500, 50, 74 },
			{ 700, 70, 90 }

	};

}
