package muliti.objective.opt.submit;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class GraphData
{
	String                   screenTitle;
	String                   xLabel;
	String                   yLabel;
	String                   graphTitle;
	Map<String, List<Point>> pointMap;
	double minX=0.0;
	double maxX=0.0;
	double minY=0.0;
	double maxY=0.0;
	int changeCount=0;

	public GraphData putScreenTitle(String screenTitle)
	{
		this.screenTitle = screenTitle;
		return this;
	}

	public GraphData putXLabel(String xLabel)
	{
		this.xLabel = xLabel;
		return this;
	}

	public GraphData putYLabel(String yLabel)
	{
		this.yLabel = yLabel;
		return this;
	}

	public GraphData putGraphTitle(String graphTitle)
	{
		this.graphTitle = graphTitle;
		return this;
	}

	public GraphData putPointMap(Map<String, List<Point>> pointMap)
	{
		this.pointMap = pointMap;
		return this;
	}

	public GraphData(String screenTitle, String xLabel, String yLabel, String graphTitle,
			Map<String, List<Point>> pointMap)
	{
		super();
		this.screenTitle = screenTitle;
		this.xLabel      = xLabel;
		this.yLabel      = yLabel;
		this.graphTitle  = graphTitle;
		this.pointMap    = pointMap;
	}
}
